package my.netty.rpc.exception;

public class RejectResponseException extends RuntimeException {

    public RejectResponseException() {
        super();
    }

    public RejectResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectResponseException(String message) {
        super(message);
    }

    public RejectResponseException(Throwable cause) {
        super(cause);
    }
}
