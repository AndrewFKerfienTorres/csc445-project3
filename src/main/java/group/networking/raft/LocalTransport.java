package group.networking.raft;

import io.microraft.RaftEndpoint;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalTransport implements Transport{

    //should be shared across nodes
    private static final Map<RaftEndpoint, LocalTransport> registry = new ConcurrentHashMap<>();

    private final RaftEndpoint localEndpoint;
    private io.microraft.RaftNode localNode;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocalTransport(RaftEndpoint endpoint) {
        this.localEndpoint = endpoint;
        registry.put(endpoint, this);
    }

    public void setNode(io.microraft.RaftNode node) {
        this.localNode = node;
    }

    @Override
    public void send(RaftEndpoint target, RaftMessage message) {
        LocalTransport targetTransport = registry.get(target);
        if (targetTransport != null) {
            executor.submit(() -> targetTransport.localNode.handle(message));
        }
    }

    @Override
    public boolean isReachable(RaftEndpoint endpoint) {
        return registry.containsKey(endpoint);
    }
}
