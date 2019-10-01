package server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/*
    Message format:
    1) File name length (in bytes), integer (4 bytes)
    2) Filename encoded in UTF-8
    3) File size (in bytes), long (8 bytes)
    4) File itself

    Server response:
    * Success: send zero byte
    * Failure: send non-zero byte
 */

public class ConnectionManager implements Runnable {
    private Socket connection;
    private AtomicLong totalBytesReceived = new AtomicLong(0);
    private long expectedFileLength = -1;

    private static final int FILENAME_LENGTH_BYTES = 4;
    private static final int FILESIZE_LENGTH_BYTES = 8;
    private static final int INPUT_BUFFER_SIZE = 1024;
    private static final String DOWNLOAD_DIR = "./uploads";

    private static final int SUCCESS_MESSAGE = 0;
    private static final int FAILURE_MESSAGE = 1;

    public ConnectionManager(Socket socket) {
        connection = socket;
    }

    @Override
    public void run() {
        try {
            InputStream socketInputStream = connection.getInputStream();
            OutputStream socketOutputStream = connection.getOutputStream();

            FileInfo fileInfo = readFileInfo(socketInputStream);
            File filepath = createFile(fileInfo);

            loadFile(socketInputStream, filepath);
            sendResponse(fileInfo);
        } catch (Exception e) {
            System.err.println(String.format("%s error: %s", connection.getInetAddress(), e.getMessage()));
        } finally {
            shutdown();
        }
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
    }

    public

    public void shutdown() {
        try {
            connection.close();
        } catch (IOException e) {
            System.err.println(String.format("%s error: %s", connection.getInetAddress(), e.getMessage()));
        }
    }

    public InetAddress getRemoteAddress() {
        return connection.getInetAddress();
    }

    public boolean isActive() {
        return !connection.isClosed();
    }

    public long getExpectedFileLength() {
        return expectedFileLength;
    }

    private FileInfo readFileInfo(InputStream socketInputStream) throws IOException {
        // TODO: think about endianness
    }

    private File createFile(FileInfo fileInfo) throws IOException {
        File downloadDirectory = new File(DOWNLOAD_DIR);
        downloadDirectory.createNewFile();

        File filepath;
        boolean fileWasCreated = false;
        do {
            Date date = new Date(System.currentTimeMillis());
            String filename = String.format("[%s] - %s", date.toString(), fileInfo.getFilename());

            filepath = new File(downloadDirectory, filename);
            fileWasCreated = filepath.createNewFile();
        } while (!fileWasCreated);

        return filepath;
    }

    private void loadFile(InputStream socketInputStream, File filepath) throws IOException {
        try (OutputStream fileOutputStream = new FileOutputStream(filepath)) {
            int bytesReceived = 0;
            byte[] inputBuffer = new byte[INPUT_BUFFER_SIZE];
            while ((bytesReceived = socketInputStream.read(inputBuffer)) != -1) {
                totalBytesReceived.addAndGet(bytesReceived);
                fileOutputStream.write(inputBuffer, 0, bytesReceived);
            }
        }
    }

    private void sendResponse(FileInfo fileInfo) throws IOException {
        OutputStream socketOutputStream = connection.getOutputStream();
        if (getTotalBytesReceived() == fileInfo.getFileLength()) {
            socketOutputStream.write(SUCCESS_MESSAGE);
        } else {
            socketOutputStream.write(FAILURE_MESSAGE);
        }
    }
}
