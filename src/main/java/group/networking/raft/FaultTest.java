package group.networking.raft;

import group.networking.game.GameAction;
import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;

import java.util.Collection;
import java.util.List;

/**
 *   1. Start 3 nodes, wait for election
 *   2. Client sends PLACE_BET commits fine
 *   3. Kill the leader
 *   4. Client sends HIT GameClient detects the failure,
 *      finds the new leader, and retries automatically
 *   5. Confirm the second action committed on the surviving nodes
 */
public class FaultTest {

    public static void main(String[] args) throws Exception {

        TCPEndpoint ep1 = new TCPEndpoint("1", "localhost", 9001);
        TCPEndpoint ep2 = new TCPEndpoint("2", "localhost", 9002);
        TCPEndpoint ep3 = new TCPEndpoint("3", "localhost", 9003);

        List<RaftEndpoint> members = List.of(ep1, ep2, ep3);

        RaftNode node1 = buildNode(ep1, members);
        RaftNode node2 = buildNode(ep2, members);
        RaftNode node3 = buildNode(ep3, members);

        node1.start();
        node2.start();
        node3.start();

        System.out.println("=== Waiting for initial leader election ===");
        Thread.sleep(4000);

        List<RaftNode> allNodes = List.of(node1, node2, node3);
        GameClient client = new GameClient(allNodes);

        System.out.println("\n=== Sending PLACE_BET before crash ===");
        client.sendAction(new GameAction(GameAction.Type.PLACE_BET, "player1", 100));

        // Find and print who the leader is before we kill it
        RaftNode leaderBeforeCrash = null;
        for (RaftNode n : allNodes) {
            try {
                if (n.getReport().join().getResult().getRole()
                        == io.microraft.RaftRole.LEADER) {
                    leaderBeforeCrash = n;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (leaderBeforeCrash == null) {
            System.err.println("Could not identify leader — aborting.");
            return;
        }

        System.out.println("\nkilling leader: "
                + leaderBeforeCrash.getLocalEndpoint());
        leaderBeforeCrash.terminate();
        System.out.println("Leader down. GameClient will handle re-election automatically.");
        Thread.sleep(5000);

        System.out.println("\n=== Sending HIT after crash ===");
        try {
            client.sendAction(new GameAction(GameAction.Type.HIT, "player1"));
            System.out.println("\n[PASS] action committed after leader crash.");
        } catch (Exception e) {
            System.err.println("\n[FAIL] Could not recover after leader crash: " + e.getMessage());
        }

        System.out.println("\n=== Verifying consistency on surviving nodes ===");

        final RaftNode deadLeader = leaderBeforeCrash;
        List<RaftNode> survivors = allNodes.stream()
                .filter(n -> n != deadLeader)
                .toList();

        for (RaftNode n : survivors) {
            try {
                var report = n.getReport().join().getResult();
                System.out.println(n.getLocalEndpoint()
                        + "  role=" + report.getRole()
                        + "  commitIndex=" + report.getLog().getCommitIndex());
            } catch (Exception e) {
                System.out.println(n.getLocalEndpoint() + " unreachable: " + e.getMessage());
            }
        }

        // Clean shutdown
        System.out.println("\n=== Shutting down ===");
        for (RaftNode n : survivors) {
            try { n.terminate(); } catch (Exception ignored) {}
        }
        System.out.println("Done.");
    }

    private static RaftNode buildNode(TCPEndpoint self, Collection<RaftEndpoint> allMembers)
            throws Exception {
        TCPTransport transport = new TCPTransport(self);
        RaftServer server = new RaftServer(self.getPort());

        RaftNode node = RaftNode.newBuilder()
                .setGroupId("blackjack")
                .setLocalEndpoint(self)
                .setInitialGroupMembers(allMembers)
                .setConfig(RaftConfig.newBuilder()
                        .setLeaderHeartbeatPeriodSecs(1)
                        .setLeaderElectionTimeoutMillis(3000)
                        .build())
                .setTransport(transport)
                .setStateMachine(new StateManager())
                .build();

        server.start(node);
        return node;
    }
}