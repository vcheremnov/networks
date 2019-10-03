package client;

public class FileTransmissionException extends Exception {
    public FileTransmissionException() {
        super();
    }

    public FileTransmissionException(String message) {
        super(message);
    }

    public FileTransmissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileTransmissionException(Throwable cause) {
        super(cause);
    }
}
