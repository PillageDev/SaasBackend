package tbd.group.utils.exceptions;

public class CollectionNotFound extends RuntimeException {
    public CollectionNotFound() {
        super("Collection not found");
    }

    public CollectionNotFound(Throwable cause) {
        super(cause);
    }

    public CollectionNotFound(String message) {
        super(message);
    }

    public CollectionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}
