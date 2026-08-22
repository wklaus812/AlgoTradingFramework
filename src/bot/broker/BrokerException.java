package bot.broker;

public class BrokerException extends RuntimeException {

    public BrokerException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokerException(String message){
        super(message);
    }

}
