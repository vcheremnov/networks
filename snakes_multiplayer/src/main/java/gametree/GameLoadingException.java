package gametree;

public class GameLoadingException extends Exception {
    public GameLoadingException() {
        super();
    }

    public GameLoadingException(String message) {
        super(message);
    }

    public GameLoadingException(Throwable cause) {
        super(cause);
    }

    public GameLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
