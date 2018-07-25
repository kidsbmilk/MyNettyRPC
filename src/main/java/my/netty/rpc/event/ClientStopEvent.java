package my.netty.rpc.event;

public class ClientStopEvent {

    private final String message;

    public ClientStopEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
