import msgpack
import zstandard as zstd
import io
from huggingface_hub import snapshot_download
import os
from pathlib import Path
import math 
import csv
import json
import argparse
from dataclasses import dataclass
from multiprocessing import Pool, Queue, Manager
import multiprocessing as mp

from dataclasses import dataclass

@dataclass
class EndgameHeuristic:
    max_queens: int = 0
    max_rooks: int = 2
    max_minor_pieces: int = 6
    max_total_pieces: int = 10

    def is_endgame(self, fen: str) -> bool:
        board = fen.split(' ', 1)[0]
        queens = rooks = bishops = knights = pawns = kings = 0
        total_pieces = 0

        for c in board:
            if c == '/' or c.isdigit():
                continue
            total_pieces += 1
            if c in 'qQ':
                queens += 1
            elif c in 'rR':
                rooks += 1
            elif c in 'bB':
                bishops += 1
            elif c in 'nN':
                knights += 1
            elif c in 'pP':
                pawns += 1
            elif c in 'kK':
                kings += 1

        minor_pieces = bishops + knights

        return (
            queens <= self.max_queens and
            rooks <= self.max_rooks and
            minor_pieces <= self.max_minor_pieces and
            total_pieces <= self.max_total_pieces
        )

    def pawn_about_to_promote(self, fen: str) -> bool:
        board = fen.split(' ', 1)[0]
        ranks = board.split('/')
        if 'P' in ranks[1]:
            return True
        if 'p' in ranks[6]:
            return True
        return False


def load_positions(filepath):
    dctx = zstd.ZstdDecompressor()
    with open(filepath, 'rb') as f:
        with dctx.stream_reader(f) as reader:
            unpacker = msgpack.Unpacker(reader, raw=False)
            for record in unpacker:
                yield record  # generator instead of loading all into memory


def clamp(n, min_value, max_value):
    return max(min_value, min(n, max_value))


def makeScore(score: int, mate):
    score = (score - 0.5) * 200
    if mate is None:
        return int(score)
    if mate == "#":
        return int(score)
    if not type(mate) == int:
        print("ERROR OCCURRED: MATE NOT A NUMBER AFTER CHECKS")
        return int(score)
    mate = mate * 2
    return int(clamp(score - mate, -100, 100))


def worker_fn(args):
    """Processes a single file and puts rows into the queue."""
    filename, filterEndgame, queue = args

    if not Path(filename).is_file():
        return

    categoriser = EndgameHeuristic()
    print(f"[Worker {mp.current_process().name}] starting: {filename}")
    count = 0

    for record in load_positions(filepath=filename):
        fen = record['fen']
        if filterEndgame and not categoriser.pawn_about_to_promote(fen):
            continue

        moves = record['moves']
        best_move = None
        best_score = -math.inf
        best_cp = 0
        top_moves = []

        for move, eval in moves.items():
            score = makeScore(eval['win_prob'], eval['mate'])
            top_moves.append({"move": move, "cp": score})
            if eval['win_prob'] > best_score:
                best_score = eval['win_prob']
                best_move = move
                best_cp = score

        queue.put({
            'fen': fen,
            'best_move': best_move,
            'top_moves_json': json.dumps(top_moves),
            'score_cp': best_cp
        })
        count += 1

    print(f"[Worker {mp.current_process().name}] finished: {filename} ({count} records)")


def writer_fn(queue, output_path, total_files):
    """Single writer process that drains the queue and writes to CSV."""
    with open(output_path, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=['fen', 'best_move', 'top_moves_json', 'score_cp'])
        writer.writeheader()

        total_written = 0
        while True:
            row = queue.get()
            if row is None:  # poison pill
                break
            writer.writerow(row)
            total_written += 1
            if total_written % 1000 == 0:
                csvfile.flush()
                print(f"[Writer] {total_written} rows written...")

    print(f"[Writer] Done. Total rows written: {total_written}")


def process_files(files, filterEndgame=False, output_path="output.csv", num_workers=None):
    if num_workers is None:
        num_workers = min(mp.cpu_count(), len(files))

    manager = Manager()
    queue = manager.Queue(maxsize=5000)  # backpressure so workers don't flood RAM

    # Start the writer process
    writer_proc = mp.Process(
        target=writer_fn,
        args=(queue, output_path, len(files))
    )
    writer_proc.start()

    # Worker args — queue is shared via Manager
    worker_args = [(str(f), filterEndgame, queue) for f in files]

    with Pool(processes=num_workers) as pool:
        pool.map(worker_fn, worker_args)

    # Send poison pill to shut down writer
    queue.put(None)
    writer_proc.join()


def main():
    parser = argparse.ArgumentParser(
        description="Generate chess FEN + policy/value labels using a hugging face dataset"
    )
    parser.add_argument("--download", type=bool, default=False,
                        help="Should download the files from hugging face (mostly on first call)")
    parser.add_argument("--output", type=str, default="chess_training_hf_data.csv",
                        help="File to write to (csv)")
    parser.add_argument("--workers", type=int, default=None,
                        help="Number of worker processes (default: CPU count)")

    args = parser.parse_args()

    if args.download:
        api_key = os.getenv('HF_TOKEN')
        print(api_key)
        snapshot_download(
            repo_id="prdev/chessbench-full-policy-value",
            repo_type="dataset",
            allow_patterns=["train-***0-of-01024.msgpack.zst"],
            local_dir="./data",
            token=api_key
        )

    files = list(Path("./data").iterdir())
    process_files(files, True, args.output, args.workers)


if __name__ == "__main__":
    main()