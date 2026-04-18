# Move-Gen is finished (mostly)

## Missing

- Castling
  - I wanna do smth else I can't do this anymore 🥲

## Working

- All move gen including En-passant and Promotions
- making a move
  - alters board state accordingly
  - returns UndoMoveInfo
- unmaking a move
  - when receiving undo info the board is reset to the earlier state.
  - The board state is the exact same as before
- More than 1 depth for move gen
  - Move gen relies on pre-allocating one giant move array at startup
  - This holds all moves till max-search depth
  - Adding more than one layer of moves into it works and doesn't break anything