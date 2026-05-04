package group.networking.raft;

import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftRole;

import java.util.Collection;
import java.util.List;


public class NetworkedRaftTest {

    public static void main(String[] args) throws Exception {

        // localhost test
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

        System.out.println("started, waiting for leader election...");
        Thread.sleep(4000);

        // finds the leader
        for (RaftNode node : List.of(node1, node2, node3)) {
            if (node.getReport().join().getResult().getRole() == RaftRole.LEADER) {
                System.out.println(node.getLocalEndpoint() + " is the leader");

                try {
                var future = node.replicate("wave from networked raft");
                var result = future.join();
                System.out.println("Committed at index: " + result.getCommitIndex()+ ", result: " + result.getResult());
                } catch (Exception e) {
                    System.err.println("Replication failed: " + e.getMessage());
                    e.printStackTrace();
                }

                break;
            }
        }

        // time to apply committed entry
        Thread.sleep(2000);
        node2.terminate();
        Thread.sleep(2000);

        node1.terminate();
        node2.terminate();
        node3.terminate();

        System.out.println("All nodes terminated.");
    }

    private static RaftNode buildNode(TCPEndpoint self, Collection<RaftEndpoint> allMembers)
            throws Exception {

        TCPTransport transport = new TCPTransport(self);

        // Starting the server before building the node so it's ready to receive messages from other nodes during leader election
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