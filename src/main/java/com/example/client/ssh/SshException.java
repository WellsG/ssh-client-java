package com.example.client.ssh;

public class SshException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * do not support a no-arg constructor to make exception meaningful
     */
    @SuppressWarnings("unused")
    private SshException() {
        // do nothing
    }

    /**
     * Constructs a new exception with the specified detail message, cause, suppression enabled or disabled, and writable stack
     * trace enabled or disabled. <br />
     * BTW, the message and this exception instance will be logged using a embedded logger as a warn exception
     * 
     * @param message the detail message.
     * @param cause the cause. (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public SshException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in this exception's
     * detail message.<br />
     * BTW, the message and this exception instance will be logged using a embedded logger as a warn exception
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *        permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public SshException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.<br />
     * BTW, the message and this exception instance will be logged using a embedded logger as a warn exception
     * 
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public SshException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of <tt>cause</tt>
     * ). This constructor is useful for exceptions that are little more than wrappers for other throwables.<br />
     * BTW, the message and this exception instance will be logged using a embedded logger as a warn exception
     * 
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is
     *        permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public SshException(Throwable cause) {
        super(cause);
    }
}
