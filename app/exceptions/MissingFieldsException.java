package exceptions;

/**
 * Created by plessmann on 05/06/15.
 */
public class MissingFieldsException extends Exception {
    public MissingFieldsException(String msg) {
        super(msg);
    }
}