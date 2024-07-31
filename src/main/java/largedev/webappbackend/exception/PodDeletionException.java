package largedev.webappbackend.exception;

public class PodDeletionException extends RuntimeException {
    public PodDeletionException(String message) {
        super(message);
    }
}