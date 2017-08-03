package BLL;

/**
 * Ошибки
 */
public class BLLException extends Exception{
    private String message;

    public BLLException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
