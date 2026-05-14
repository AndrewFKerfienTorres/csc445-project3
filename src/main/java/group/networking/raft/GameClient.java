package group.networking.raft;

import io.microraft.RaftNode;
import io.microraft.RaftRole;
import io.microraft.exception.NotLeaderException;
import group.networking.game.GameAction;

import java.util.List;


public class GameClient {

    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY_MS = 2000;

    private final List<RaftNode> clusterNodes;

    private RaftNode cachedLeader = null;

    public GameClient(List<RaftNode> clusterNodes) {
        this.clusterNodes = clusterNodes;
    }


    // Sends a GameAction to the Raft cluster and returns the result.
    public String sendAction(GameAction action) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                RaftNode leader = getLeader();

                if (leader == null) {
                    System.out.println("[GameClient] No leader found (attempt " + attempt
                            + "/" + MAX_RETRIES + "), waiting for election...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                Object result = leader.replicate(action).join().getResult();
                System.out.println("[GameClient] Action committed: " + action
                        + " → " + result);
                return (String) result;

            } catch (NotLeaderException e) {
                System.out.println("[GameClient] Not the leader. Redirecting to: "
                        + e.getLeader());
                cachedLeader = findNodeByEndpointId(
                        e.getLeader() != null
                                ? (String) e.getLeader().getId()
                                : null
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("GameClient interrupted", e);

            } catch (Exception e) {
                // Leader likely crashed
                System.out.println("[GameClient] Request failed (attempt " + attempt
                        + "/" + MAX_RETRIES + "): " + e.getMessage()
                        + " — retrying after re-election...");
                cachedLeader = null;
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("GameClient interrupted", ie);
                }
            }
        }

        throw new RuntimeException("[GameClient] Failed to commit action after "
                + MAX_RETRIES + " attempts: " + action);
    }


    private RaftNode getLeader() {
        if (cachedLeader != null) {
            try {
                if (cachedLeader.getReport().join().getResult().getRole() == RaftRole.LEADER) {
                    return cachedLeader;
                }
            } catch (Exception e) {
                // Node is dead or unreachable
            }
            cachedLeader = null;
        }

        cachedLeader = scanForLeader();
        return cachedLeader;
    }

    private RaftNode scanForLeader() {
        for (RaftNode node : clusterNodes) {
            try {
                if (node.getReport().join().getResult().getRole() == RaftRole.LEADER) {
                    return node;
                }
            } catch (Exception e) {
                // skip
            }
        }
        return null;
    }


    private RaftNode findNodeByEndpointId(String id) {
        if (id == null) return null;
        for (RaftNode node : clusterNodes) {
            if (node.getLocalEndpoint().getId().equals(id)) {
                return node;
            }
        }
        return null;
    }
}