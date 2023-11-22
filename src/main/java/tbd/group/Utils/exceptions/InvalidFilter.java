package tbd.group.utils.exceptions;

public class InvalidFilter extends RuntimeException {
    public InvalidFilter() {
        super("Invalid filter");
    }

    public InvalidFilter(Throwable cause) {
        super(cause);
    }

    public InvalidFilter(String message) {
        super(message);
    }

    public InvalidFilter(String message, Throwable cause) {
        super(message, cause);
    }
}
