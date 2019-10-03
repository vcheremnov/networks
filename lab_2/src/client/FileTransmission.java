package client;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FileTransmission {
    private static final int FILE_NAME_BYTES = Integer.BYTES;
    private static final int FILE_SIZE_BYTES = Long.BYTES;

    private Socket connection;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;

    private InputStream fileInputStream;
    private File file;
    private boolean isSuccessful = false;

    public void start(String serverHostname, int serverPort) throws FileTransmissionException {
        try {
            isSuccessful = false;
            openFile();
            openConnection(serverHostname, serverPort);
            sendHeader();
            sendFile();
            receiveResponse();
        } catch (IOException e) {
            throw new FileTransmissionException(e);
        } finally {
            closeConnection();
            closeFile();
        }
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setFilepath(String filepath) {
        file = new File(filepath);
    }

    private void sendHeader() throws FileTransmissionException, IOException {
        if (file == null) {
            throw new FileTransmissionException("File to send was not specified");
        }

        String filename = file.getName();
        byte[] encodedFilenameBytes = filename.getBytes(StandardCharsets.UTF_8);

        ByteBuffer numberBuffer = ByteBuffer.allocate(FILE_NAME_BYTES + FILE_SIZE_BYTES);
        numberBuffer.putInt(encodedFilenameBytes.length);
        numberBuffer.putLong(file.length());
        byte[] sizesBytes = numberBuffer.array();

        socketOutputStream.write(sizesBytes);
        socketOutputStream.write(encodedFilenameBytes);
    }

    private void openFile() throws FileTransmissionException, IOException {
        if (file == null) {
            throw new FileTransmissionException("File to send was not specified");
        }

        fileInputStream = new FileInputStream(file);
    }

    private void sendFile() throws IOException {
        fileInputStream.transferTo(socketOutputStream);
    }

    private void closeFile() {
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
                fileInputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveResponse() throws IOException {
        int response = socketInputStream.read();
        isSuccessful = (response == 0);
    }

    private void openConnection(String remoteHostname, int port) throws IOException {
        connection = new Socket(remoteHostname, port);
        socketInputStream = connection.getInputStream();
        socketOutputStream = connection.getOutputStream();
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (IOException e) {
            System.err.println(String.format("Failed to close the connection: %s", e.getMessage()));
        }
    }
}
