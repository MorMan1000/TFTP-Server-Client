package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionHandlers = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();

    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connectionHandlers.put(connectionId, handler);
    }

    public boolean send(int connectionId, T msg) {
        connectionHandlers.get(connectionId).send(msg);
        return true;
    }

    public void disconnect(int connectionId) {
        try {
            ConnectionHandler<T> handler = connectionHandlers.remove(connectionId);
            handler.close();
        } catch (IOException e) {
        }

    }
}
