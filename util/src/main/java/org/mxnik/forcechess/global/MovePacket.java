package org.mxnik.forcechess.global;

public record MovePacket(MoveType type, int from, int to, boolean capture){
}
