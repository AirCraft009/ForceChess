package org.mxnik.forcechess.bot.baseStateBot;

import org.mxnik.forcechess.Util.Bitboard;

/**
 * Info for unmaking a move
 *
 * @param from origin position
 * @param to current Piece position
 * @param flags type of move (Generic, Castle, enPassent, etc.)
 *
 */
public record UndoMoveInfo(Integer from, Integer to, Integer flags) {
}
