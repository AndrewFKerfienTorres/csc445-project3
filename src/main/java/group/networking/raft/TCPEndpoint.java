package group.networking.raft;

import io.microraft.RaftEndpoint;
import java.io.Serializable;


public class TCPEndpoint implements RaftEndpoint, Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String host;
    private final int port;

    public TCPEndpoint(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TCPEndpoint other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Node-" + id + "(" + host + ":" + port + ")";
    }
}