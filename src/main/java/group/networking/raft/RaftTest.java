package group.networking.raft;

import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class RaftTest {
    public static void main(String[] args) throws Exception {
        LocalEndpoint ep1 = new LocalEndpoint("1");
        LocalEndpoint ep2 = new LocalEndpoint("2");
        LocalEndpoint ep3 = new LocalEndpoint("3");

        ArrayList<RaftEndpoint> members = new ArrayList<>();
        members.add(ep1);
        members.add(ep2);
        members.add(ep3);

        RaftNode node1 = buildNode(ep1, members);
        RaftNode node2 = buildNode(ep2, members);
        RaftNode node3 = buildNode(ep3, members);

        node1.start();
        node2.start();
        node3.start();

        //wait for leader election
        Thread.sleep(2000);


        for (RaftNode node : List.of(node1, node2, node3)) {
            if (node.getReport().join().getResult().getRole() == RaftRole.LEADER) {
                System.out.println(node.getLocalEndpoint() + " is the leader!");
                var future = node.replicate("Hello from Raft!");
                var result = future.join();
                System.out.println("Committed! Index: " + result.getCommitIndex() 
                    + ", Result: " + result.getResult());
                break;
            }
        }


        node1.terminate();
        node2.terminate();
        node3.terminate();
    }

    private static RaftNode buildNode(LocalEndpoint self, Collection<RaftEndpoint> allMembers) {
        LocalTransport transport = new LocalTransport(self);

        RaftNode node = RaftNode.newBuilder()
            .setGroupId("poker")
            .setLocalEndpoint(self)
            .setInitialGroupMembers(allMembers)
            .setConfig(RaftConfig.newBuilder()
                .setLeaderHeartbeatPeriodSecs(1)
                .setLeaderElectionTimeoutMillis(3000)
                .build())
            .setTransport(transport)
            .setStateMachine(new StateManager())
            .build();

        // give the transport a reference back to the node
        transport.setNode(node);
        return node;
    }
}
