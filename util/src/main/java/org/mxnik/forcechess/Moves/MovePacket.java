package org.mxnik.forcechess.Moves;

public record MovePacket(MoveType type, int from, int to, boolean capture){
}
