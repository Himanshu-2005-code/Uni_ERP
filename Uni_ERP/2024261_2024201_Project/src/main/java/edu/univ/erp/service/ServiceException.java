package edu.univ.erp.service;

/** A failure with a message that is safe and useful to show the user. */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
