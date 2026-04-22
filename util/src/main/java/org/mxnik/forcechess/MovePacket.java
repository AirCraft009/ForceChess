package org.mxnik.forcechess;

public record MovePacket(MoveType type, int from, int to, boolean capture){
}
