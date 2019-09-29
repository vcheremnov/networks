package server;

import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private Socket connection;

    public ConnectionHandler(Socket socket) {
        connection = socket;
    }

    @Override
    public void run() {

    }
}
