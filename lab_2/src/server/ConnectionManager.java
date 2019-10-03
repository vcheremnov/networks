package server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/*
    Message format:
    1) File name length (in bytes), integer (4 bytes)
    2) File size (in bytes), long (8 bytes)
    3) Filename encoded in UTF-8
    4) File itself

    Server response:
    * Success: send zero byte
    * Failure: send non-zero byte
 */

public class ConnectionManager implements Runnable {
    private static final int FILE_NAME_BYTES = Integer.BYTES;
    private static final int FILE_SIZE_BYTES = Long.BYTES;
    private static final int INPUT_BUFFER_SIZE = 1024;
    private static final String DOWNLOAD_DIR = "./uploads";

    private static final int SUCCESS_MESSAGE = 0;
    private static final int FAILURE_MESSAGE = 1;

    private Socket connection;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;

    private File filepath;
    private FileInfo fileInfo;
    private AtomicLong totalBytesReceived = new AtomicLong(0);
    private AtomicLong downloadStartTime = new AtomicLong(-1);

    public ConnectionManager(Socket socket) {
        connection = socket;
    }

    @Override
    public void run() {
        try {
            retrieveSocketStreams();
            receiveFileInfo();
            createFile();
            receiveFile();
            sendResponse();
        } catch (Exception e) {
            System.err.println(String.format("%s error: %s", connection.getInetAddress(), e.getMessage()));
        } finally {
            closeConnection();
        }
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (IOException e) {
            System.err.println(String.format("%s error: %s", connection.getInetAddress(), e.getMessage()));
        }
    }

    public InetAddress getRemoteAddress() {
        return connection.getInetAddress();
    }

    public int getRemotePort() {
        return connection.getPort();
    }

    public boolean isActive() {
        return !connection.isClosed();
    }

    public long getExpectedFileLength() {
        return fileInfo.getFileLength();
    }

    public long getDownloadDuration() {
        long startTime = downloadStartTime.get();
        return (startTime == -1) ? 0 : System.currentTimeMillis() - startTime;
    }

    private void retrieveSocketStreams() throws IOException {
        socketInputStream = connection.getInputStream();
        socketOutputStream = connection.getOutputStream();
    }

    private void receiveFileInfo() throws IOException {
        int bytesToRead = FILE_NAME_BYTES + FILE_SIZE_BYTES;
        byte[] sizesBytes = socketInputStream.readNBytes(bytesToRead);
        if (sizesBytes.length != bytesToRead) {
            throw new IOException("EOF was reached while reading the header from " + connection.getInetAddress());
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(sizesBytes);
        int filenameLength = byteBuffer.getInt();
        long fileLength = byteBuffer.getLong();

        byte[] encodedFilename = socketInputStream.readNBytes(filenameLength);
        if (encodedFilename.length != filenameLength) {
            throw new IOException("EOF was reached while reading the header from " + connection.getInetAddress());
        }
        String filename = new String(encodedFilename, StandardCharsets.UTF_8);

        fileInfo = new FileInfo();
        fileInfo.setFileLength(fileLength);
        fileInfo.setFilename(filename);
    }

    private void createFile() throws IOException {
        File downloadDirectory = new File(DOWNLOAD_DIR);
        downloadDirectory.createNewFile();

        Date date = new Date(System.currentTimeMillis());
        InetAddress remoteAddress = connection.getInetAddress();
        int remotePort = connection.getPort();
        String filenamePrefix = String.format(
                "[%s:%d-%s]_", remoteAddress.toString(), remotePort, date.toString()
        );

        String filename = filenamePrefix + fileInfo.getFilename();
        filepath = new File(downloadDirectory, filename);
        boolean fileWasCreated = filepath.createNewFile();
        if (!fileWasCreated) {
            filepath = File.createTempFile(filenamePrefix, "", downloadDirectory);
        }
    }

    private void receiveFile() throws IOException {
        long startTime = System.currentTimeMillis();
        downloadStartTime.set(startTime);
        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(filepath))) {
            int bytesReceived = 0;
            byte[] inputBuffer = new byte[INPUT_BUFFER_SIZE];
            while ((bytesReceived = socketInputStream.read(inputBuffer)) != -1) {
                totalBytesReceived.addAndGet(bytesReceived);
                fileOutputStream.write(inputBuffer, 0, bytesReceived);
            }
        }
    }

    private void sendResponse() throws IOException {
        if (getTotalBytesReceived() == fileInfo.getFileLength()) {
            socketOutputStream.write(SUCCESS_MESSAGE);
        } else {
            socketOutputStream.write(FAILURE_MESSAGE);
        }
    }
}
