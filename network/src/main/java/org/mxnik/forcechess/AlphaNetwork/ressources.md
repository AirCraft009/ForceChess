- DL4J
  - https://deeplearning4j.konduit.ai/nd4j/tutorials/quickstart
- AlphaZero paper
  - https://arxiv.org/pdf/2012.11045
- batch norm.
  - https://towardsdatascience.com/batch-norm-explained-visually-how-it-works-and-why-neural-networks-need-it-b18919692739/
- AlphaZero - resNet.
  - https://www.marktechpost.com/2021/12/16/understanding-alphazero-neural-networks-superhuman-chess-ability/
- ResNets
  - https://en.wikipedia.org/wiki/Residual_neural_network
  - https://medium.com/@siddheshb008/resnet-architecture-explained-47309ea9283d
  - www.youtube.com/watch?v=w1UsKanMatM

## Not making everything custom

- writing everything on my own (AlphaNetwork backProp. etc.) is not possible
- To little time
- Will not be efficient
    - Skill issue
    - No Cuda Core integration (won't run on the GPU)

## Solution

- Network will be built with the DL4J library instead
- Network architecture will still be implemented by hand.