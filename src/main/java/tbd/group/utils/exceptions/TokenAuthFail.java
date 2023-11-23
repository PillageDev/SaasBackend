package tbd.group.utils.exceptions;

public class TokenAuthFail extends RuntimeException {
    public TokenAuthFail(String errorMessage) {
        super(errorMessage);
    }

    public TokenAuthFail(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public TokenAuthFail(Throwable err) {
        super(err);
    }

    public TokenAuthFail() {
        super();
    }
}
