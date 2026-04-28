package group.networking.raft;

import io.microraft.RaftEndpoint;

public class LocalEndpoint implements RaftEndpoint {

    private final String id;

    public LocalEndpoint(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Node-" + id; 
    }
    
}
