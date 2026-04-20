https://www.youtube.com/watch?v=UXW2yZndl7U
https://www.chessprogramming.org/AlphaZero#Network_Architecture
https://arxiv.org/pdf/2012.11045
https://www.youtube.com/watch?v=NjeYgIbPMmg&t=87s


## Reasons for the Mcts - Layout

after some thought it's clear that saving MctsNodes as classes, \
with a reference to an array of references to other MctsNodes\
would lead to ptr chasing (hopping from reference to reference).\
This has negative effects on performance because new CPU's heavily rely on Cache\
and every single deref. could lead to another cache miss.

## Solution

instead of having a MctsNode class I use flat arrays with Java primitives\
with this only one ptr deref. is necessary and leads to real values.\
This is probs. the closest I can get to C performance, even tho I the array is heap alloced.\
Maybe I could have it live on the stack if I keep it compact enough \
(only speculation; and not poss. in Java anyw.)