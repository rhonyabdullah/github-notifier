package com.labsgn.githubnotifier.eventsource;

public interface EventSourceHandler {
    void onConnect() throws Exception;

    void onMessage(String event, MessageEvent message) throws Exception;

    void onError(Throwable t);

    void onClosed(boolean willReconnect);
}
