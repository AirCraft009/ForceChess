"""
Chess Training Data Generator
==============================
Generates FEN + policy + value labels for neural network training.
Output: CSV or JSON with fields:
  - fen: position string
  - best_move: UCI move string (policy)
  - top_moves: list of (move, cp_score) tuples (policy distribution)
  - score_cp: centipawn eval from white's POV (value)
  - score_wdl: win/draw/loss probabilities (value, if supported)
  - source: where the position came from

Requirements:
  pip install chess stockfish tqdm

Usage:
  python chess_data_generator.py --target 100000 --output data.csv
  python chess_data_generator.py --target 1000000 --output data.json --format json
  python chess_data_generator.py --resume --output data.csv  # continue existing file
"""

import argparse
import csv
import json
import os
import random
import re
import sys
import time
from pathlib import Path
from typing import Optional

import chess
import chess.pgn
import chess.polyglot
from stockfish import Stockfish
from tqdm import tqdm

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

DEFAULT_STOCKFISH_PATH = "C:/Users/Mxsxll/Documents/Schule/2CHIF/Fächer/POS/Projekt/ForceChess/boardsNBots/stockfish/stockfish.exe"   # override with --engine
EVAL_DEPTH = 12          # depth per position (good quality / speed tradeoff)
MULTIPV = 5              # top N moves to store for policy
HASH_MB = 256
THREADS = max(1, os.cpu_count() - 1)

# Mix ratios (must sum to 1.0)
SOURCE_MIX = {
    "random_game": 0.40,   # random self-play to diverse positions
    "pgn":         0.40,   # real master/online games from PGN files
    "opening":     0.20,   # curated opening lines
}

# Built-in opening lines (ECO A-E sampler) — extend as needed
OPENING_LINES = [
    "e2e4 e7e5 g1f3 b8c6 f1b5",                          # Ruy Lopez
    "e2e4 e7e5 g1f3 b8c6 f1c4",                          # Italian
    "e2e4 c7c5",                                           # Sicilian
    "e2e4 e7e6",                                           # French
    "e2e4 c7c6",                                           # Caro-Kann
    "d2d4 d7d5 c2c4",                                     # Queen's Gambit
    "d2d4 g8f6 c2c4 g7g6",                                # King's Indian
    "d2d4 g8f6 c2c4 e7e6 g1f3 d7d5 g2g3",                # Catalan
    "g1f3 d7d5 g2g3",                                     # Reti
    "e2e4 g8f6",                                           # Alekhine
    "d2d4 f7f5",                                           # Dutch
    "c2c4",                                                # English
    "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6",                     # Pirc
    "e2e4 e7e5 f2f4",                                     # King's Gambit
    "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5",                # QGD Classical
]

CP_CLAMP = 1500   # clamp evals beyond ±1500cp (mating/lost positions)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def clamp_cp(cp: int) -> int:
    return max(-CP_CLAMP, min(CP_CLAMP, cp))


def normalize_cp_to_wdl_approx(cp: int):
    """Very rough WDL approximation when engine doesn't return WDL."""
    import math
    win = 1 / (1 + math.exp(-cp / 400))
    loss = 1 - win
    draw = 0.0  # placeholder
    return round(win, 4), round(draw, 4), round(loss, 4)


def get_existing_fens(path: str, fmt: str) -> set:
    """Load already-seen FENs from an existing output file (for resuming)."""
    seen = set()
    if not Path(path).exists():
        return seen
    try:
        if fmt == "csv":
            with open(path, newline="") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    seen.add(row["fen"])
        else:
            with open(path) as f:
                for line in f:
                    line = line.strip().rstrip(",")
                    if not line or line in ("[\n", "]"):
                        continue
                    try:
                        obj = json.loads(line)
                        seen.add(obj["fen"])
                    except Exception:
                        pass
    except Exception:
        pass
    print(f"[resume] Found {len(seen):,} existing positions, continuing...")
    return seen


# ---------------------------------------------------------------------------
# Stockfish wrapper
# ---------------------------------------------------------------------------

class Evaluator:
    def __init__(self, engine_path: str, depth: int, multipv: int):
        self.depth = depth
        self.multipv = multipv
        self.sf = Stockfish(
            path=engine_path,
            depth=depth,
            parameters={
                "Hash": HASH_MB,
                "Threads": THREADS,
                "MultiPV": multipv,
            },
        )

    def evaluate(self, fen: str) -> Optional[dict]:
        """Return dict with best_move, top_moves, score_cp, score_wdl or None on error."""
        try:
            self.sf.set_fen_position(fen)
            top = self.sf.get_top_moves(self.multipv)
            if not top:
                return None

            best = top[0]
            best_move = best.get("Move", "")

            # Parse centipawn score
            raw_cp = best.get("Centipawn")
            raw_mate = best.get("Mate")
            if raw_mate is not None:
                cp = CP_CLAMP if raw_mate > 0 else -CP_CLAMP
            elif raw_cp is not None:
                cp = clamp_cp(int(raw_cp))
            else:
                return None

            # WDL if available
            wdl = best.get("WDL")
            if wdl:
                try:
                    parts = [int(x) for x in str(wdl).split()]
                    total = sum(parts) or 1
                    w, d, l = [round(p / total, 4) for p in parts]
                except Exception:
                    w, d, l = normalize_cp_to_wdl_approx(cp)
            else:
                w, d, l = normalize_cp_to_wdl_approx(cp)

            # Top moves list
            top_moves = []
            for m in top:
                mv = m.get("Move", "")
                if not mv:
                    continue
                m_cp = m.get("Centipawn")
                m_mate = m.get("Mate")
                if m_mate is not None:
                    m_score = CP_CLAMP if m_mate > 0 else -CP_CLAMP
                elif m_cp is not None:
                    m_score = clamp_cp(int(m_cp))
                else:
                    m_score = 0
                top_moves.append({"move": mv, "cp": m_score})

            return {
                "best_move": best_move,
                "top_moves": top_moves,
                "score_cp": cp,
                "score_wdl": {"w": w, "d": d, "l": l},
            }
        except Exception as e:
            return None


# ---------------------------------------------------------------------------
# Position sources
# ---------------------------------------------------------------------------

def random_game_positions(n: int):
    """Play random legal games, sample positions along the way."""
    positions = []
    while len(positions) < n:
        board = chess.Board()
        game_positions = []
        # Play up to 80 half-moves randomly
        for _ in range(random.randint(10, 80)):
            moves = list(board.legal_moves)
            if not moves or board.is_game_over():
                break
            board.push(random.choice(moves))
            # Skip very early and very late positions
            if 5 <= board.fullmove_number <= 40:
                game_positions.append(board.fen())
        positions.extend(game_positions)
    return positions[:n]


def pgn_positions(pgn_files: list, n: int):
    """Sample positions from PGN files."""
    positions = []
    if not pgn_files:
        return positions
    random.shuffle(pgn_files)
    for pgn_path in pgn_files:
        if len(positions) >= n:
            break
        try:
            with open(pgn_path) as f:
                while len(positions) < n:
                    game = chess.pgn.read_game(f)
                    if game is None:
                        break
                    board = game.board()
                    game_pos = []
                    for move in game.mainline_moves():
                        board.push(move)
                        if 5 <= board.fullmove_number <= 40:
                            game_pos.append(board.fen())
                    # Sample at most 8 positions per game to keep diversity
                    sampled = random.sample(game_pos, min(8, len(game_pos)))
                    positions.extend(sampled)
        except Exception:
            continue
    return positions[:n]


def opening_positions(n: int):
    """Play out opening lines then add a few random moves for variety."""
    positions = []
    while len(positions) < n:
        line = random.choice(OPENING_LINES)
        moves = line.split()
        board = chess.Board()
        for uci in moves:
            try:
                board.push_uci(uci)
            except Exception:
                break
        # Add 0-5 random moves beyond the opening
        extra = random.randint(0, 5)
        for _ in range(extra):
            legal = list(board.legal_moves)
            if not legal or board.is_game_over():
                break
            board.push(random.choice(legal))
        if not board.is_game_over():
            positions.append(board.fen())
    return positions[:n]


# ---------------------------------------------------------------------------
# Output writers
# ---------------------------------------------------------------------------

CSV_FIELDS = ["fen", "best_move", "top_moves_json", "score_cp",
              "score_wdl_w", "score_wdl_d", "score_wdl_l", "source"]

def write_header_csv(path: str, resume: bool):
    if not resume or not Path(path).exists():
        with open(path, "w", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
            writer.writeheader()

def write_row_csv(path: str, row: dict):
    with open(path, "a", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS)
        writer.writerow(row)

def write_row_json(path: str, row: dict, first: bool):
    with open(path, "a") as f:
        if first:
            f.write("[\n")
        else:
            f.write(",\n")
        json.dump(row, f)

def finalize_json(path: str):
    with open(path, "a") as f:
        f.write("\n]\n")


# ---------------------------------------------------------------------------
# Main generator
# ---------------------------------------------------------------------------

def generate(args):
    engine_path = args.engine
    if not Path(engine_path).exists():
        # Try common locations
        for candidate in ["/usr/bin/stockfish", "/usr/games/stockfish",
                          "/opt/homebrew/bin/stockfish", "stockfish"]:
            if Path(candidate).exists() or candidate == "stockfish":
                engine_path = candidate
                break
        else:
            print(f"ERROR: Stockfish not found at '{args.engine}'.")
            print("Install it (e.g. `apt install stockfish` or `brew install stockfish`)")
            print("or pass --engine /path/to/stockfish")
            sys.exit(1)

    print(f"Engine:   {engine_path}")
    print(f"Depth:    {args.depth}  |  MultiPV: {args.multipv}")
    print(f"Target:   {args.target:,} positions")
    print(f"Output:   {args.output} ({args.format})")
    print(f"Threads:  {THREADS}  |  Hash: {HASH_MB}MB")
    print()

    resume = args.resume
    seen_fens = get_existing_fens(args.output, args.format) if resume else set()
    already_have = len(seen_fens)
    remaining = args.target - already_have
    if remaining <= 0:
        print("Already have enough positions. Done.")
        return

    evaluator = Evaluator(engine_path, args.depth, args.multipv)

    # Build position queue according to mix
    pgn_files = args.pgn if args.pgn else []
    n_random   = int(remaining * SOURCE_MIX["random_game"])
    n_pgn      = int(remaining * SOURCE_MIX["pgn"]) if pgn_files else 0
    n_opening  = remaining - n_random - n_pgn

    print(f"Building position pool: {n_random:,} random | {n_pgn:,} PGN | {n_opening:,} opening")
    pool = []
    pool.extend([(fen, "random_game") for fen in random_game_positions(n_random)])
    if pgn_files:
        pool.extend([(fen, "pgn") for fen in pgn_positions(pgn_files, n_pgn)])
    else:
        # redistribute pgn share to opening if no PGN provided
        pool.extend([(fen, "opening") for fen in opening_positions(n_opening + n_pgn)])
        n_opening += n_pgn
    pool.extend([(fen, "opening") for fen in opening_positions(n_opening)])
    random.shuffle(pool)
    print(f"Pool size: {len(pool):,} positions (before dedup)")

    # Prepare output file
    if args.format == "csv":
        write_header_csv(args.output, resume)

    written = 0
    first_json = not resume or not Path(args.output).exists()
    skipped_dup = 0
    skipped_eval = 0

    with tqdm(total=remaining, unit="pos", desc="Evaluating") as pbar:
        for fen, source in pool:
            if written >= remaining:
                break

            # Dedup
            # Use FEN without move counters for dedup (more aggressive)
            fen_key = " ".join(fen.split()[:4])
            if fen_key in seen_fens:
                skipped_dup += 1
                continue
            seen_fens.add(fen_key)

            # Evaluate
            result = evaluator.evaluate(fen)
            if result is None:
                skipped_eval += 1
                continue

            row_base = {
                "fen": fen,
                "best_move": result["best_move"],
                "top_moves": result["top_moves"],
                "score_cp": result["score_cp"],
                "score_wdl": result["score_wdl"],
                "source": source,
            }

            if args.format == "csv":
                write_row_csv(args.output, {
                    "fen": fen,
                    "best_move": result["best_move"],
                    "top_moves_json": json.dumps(result["top_moves"]),
                    "score_cp": result["score_cp"],
                    "score_wdl_w": result["score_wdl"]["w"],
                    "score_wdl_d": result["score_wdl"]["d"],
                    "score_wdl_l": result["score_wdl"]["l"],
                    "source": source,
                })
            else:
                write_row_json(args.output, row_base, first=first_json and written == 0)

            written += 1
            pbar.update(1)

    if args.format == "json":
        finalize_json(args.output)

    total = already_have + written
    print(f"\nDone! Written {written:,} new positions ({total:,} total).")
    print(f"Skipped: {skipped_dup:,} duplicates, {skipped_eval:,} eval failures.")
    print(f"Output:  {args.output}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Generate chess FEN + policy/value labels using Stockfish"
    )
    parser.add_argument("--target",  type=int, default=100_000,
                        help="Total number of positions to generate (default: 100000)")
    parser.add_argument("--output",  default="chess_training_data.csv",
                        help="Output file path (default: chess_training_data.csv)")
    parser.add_argument("--format",  choices=["csv", "json"], default="csv",
                        help="Output format (default: csv)")
    parser.add_argument("--engine",  default=DEFAULT_STOCKFISH_PATH,
                        help="Path to Stockfish binary")
    parser.add_argument("--depth",   type=int, default=EVAL_DEPTH,
                        help=f"Search depth per position (default: {EVAL_DEPTH})")
    parser.add_argument("--multipv", type=int, default=MULTIPV,
                        help=f"Number of top moves to store (default: {MULTIPV})")
    parser.add_argument("--pgn",     nargs="*", default=[],
                        help="Path(s) to PGN files for real game positions")
    parser.add_argument("--resume",  action="store_true",
                        help="Resume from existing output file (skip already-seen FENs)")

    args = parser.parse_args()
    generate(args)


if __name__ == "__main__":
    main()