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
        """
        Returns True if a pawn is one move away from promotion.

        White pawn on 7th rank.
        Black pawn on 2nd rank.
        """
        board = fen.split(' ', 1)[0]
        ranks = board.split('/')

        # Rank 7 (white promotion next move)
        if 'P' in ranks[1]:
            return True

        # Rank 2 (black promotion next move)
        if 'p' in ranks[6]:
            return True

        return False
        
    

def load_positions(filepath):
    dctx = zstd.ZstdDecompressor()
    records = []
    with open(filepath, 'rb') as f:
        with dctx.stream_reader(f) as reader:
            unpacker = msgpack.Unpacker(reader, raw=False)
            for record in unpacker:
                records.append(record)
    return records

def clamp(n, min_value, max_value):
    return max(min_value, min(n, max_value))

def makeScore(score: int, mate):
    score = (score - 0.5) * 200       # center score at 0 for equal position; scale up to cp (-100 - 100)
    
    if mate == None:
        return int(score)                # return basic score
        
    
    if(mate == "#"):
        return int(score)        # score will be -1 or 1
    
    if(not type(mate) == int):
        print("ERROR OCCURRED: MATE NOT A NUMBER AFTER CHECKS")
        return int(score)
    
    mate = (mate) * 2                        # The further away from mate the worse the score is (win); The further the better the score will be (loss) 
    return  int(clamp(score - mate, -100, 100))
    

def process_files(files, filterEndgame=False, output_path="output.csv"):
    with open(output_path, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=['fen', 'best_move', 'top_moves_json', 'score_cp'])
        writer.writeheader()
        categoriser = EndgameHeuristic()
        
        for filename in files:
            if not Path.is_file(filename):
                continue
            print("starting file:")
            count = 0
            for record in load_positions(filepath=filename):
                fen = record['fen']
                if (filterEndgame and not categoriser.pawn_about_to_promote(fen)):
                    continue
                
                

                moves = record['moves']

                best_move = None
                best_score = -math.inf
                top_moves = []

                for move, eval in moves.items():
                    score = makeScore(eval['win_prob'], eval['mate'])
                    top_moves.append({"move": move, "cp": score})

                    if eval['win_prob'] > best_score:
                        best_score = eval['win_prob']
                        best_move = move
                        best_cp = score

                writer.writerow({
                    'fen': fen,
                    'best_move': best_move,
                    'top_moves_json': json.dumps(top_moves),
                    'score_cp': best_cp
                })
                count += 1
                if count % 100 == 0:
                    csvfile.flush()
                
                
            print("finished file:")

def main():
    parser = argparse.ArgumentParser(
        description="Generate chess FEN + policy/value labels using a hugging face dataset"
    )
    parser.add_argument("--download",  type=bool, default=False,
                    help="Should download the files from hugging face (mostly on first call)")
    
    parser.add_argument("--output",  type=str, default="chess_training_hf_data.csv",
                    help="File to write to (csv)")

    args = parser.parse_args()
    
    # Download If not existent
    if args.download:
        api_key = os.getenv('HF_TOKEN')
        print(api_key)
        snapshot_download(
            repo_id="prdev/chessbench-full-policy-value",
            repo_type="dataset",
            allow_patterns=["train-***0-of-01024.msgpack.zst"],  # first 5 shards
            local_dir="./data",
            token=api_key
        )

        
        
    files = list(Path("./data").iterdir())
    
    process_files(files, True, args.output)
    
if __name__ == "__main__":
    main()
        
