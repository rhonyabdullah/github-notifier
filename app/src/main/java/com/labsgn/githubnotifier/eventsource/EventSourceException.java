package com.labsgn.githubnotifier.eventsource;

public class EventSourceException extends RuntimeException {
    public EventSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventSourceException(String message) {
        super(message);
    }
}
