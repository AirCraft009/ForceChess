'''
PIPELINE:

load dataset from hugging face
Build correctly encoded tensors and write them in a format readable  by java

docs: https://huggingface.co/datasets/Lichess/chess-position-evaluations
'''
from datasets import load_dataset
import sys
import struct
    

# Source - https://stackoverflow.com/a/31852401
# Posted by Roberto, modified by community. See post 'Timeline' for change history
# Retrieved 2026-04-21, License - CC BY-SA 3.0
def load_properties(filepath : str, sep='=', comment_char='#'):
    """
    Read the file passed as parameter as a properties file.
    """
    props = {}
    with open(filepath, "rt") as f:
        for line in f:
            l = line.strip()
            if l and not l.startswith(comment_char):
                key_value = l.split(sep)
                key = key_value[0].strip()
                value = sep.join(key_value[1:]).strip().strip('"') 
                props[key] = value 
    return props

def writeFloat(f, value : float):
    # write a float (4 bytes, big-endian)
    f.write(struct.pack(">f", value))

def writeInt(f, value : int):
    # write an int (4 bytes, big-endian)
    f.write(struct.pack(">i", value))

if __name__ == "__main__":

    amount = int(sys.argv[1]) if len(sys.argv) > 1 else 10000

    dset = load_dataset("Lichess/chess-position-evaluations", split="train", streaming=True)
    needed = dset.take(amount)
    
    poperties = load_properties("boardsNBots\option.properties")

    with open("data.bin", "wb") as f:
        pass