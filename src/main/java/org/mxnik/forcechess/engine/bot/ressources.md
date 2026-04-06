- DL4J
  - https://deeplearning4j.konduit.ai/nd4j/tutorials/quickstart
- AlphaZero paper
  - https://arxiv.org/pdf/2012.11045

- batch norm.
  - https://towardsdatascience.com/batch-norm-explained-visually-how-it-works-and-why-neural-networks-need-it-b18919692739/

## Not making everything custom

- writing everything on my own (network backProp. etc.) is not possible
- To little time
- Will not be efficient
    - Skill issue
    - No Cuda Core integration (won't run on the GPU)

## Solution

- Network will be built with the DL4J library instead
- Network architecture will still be implemented by hand.