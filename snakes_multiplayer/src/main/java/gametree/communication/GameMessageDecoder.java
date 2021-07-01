package gametree.communication;

@FunctionalInterface
public interface GameMessageDecoder {
    GameMessage decodeGameMessage(byte[] messageBytes) throws Exception;
}
