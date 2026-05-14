package group.networking.raft;

import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class RaftTest {

	private static List<RaftEndpoint> endpoints = null;
	private static RaftNode[] nodes;
	private static HashMap<String, RaftServer> servers;

	@BeforeEach void setup() throws Exception{
		TCPEndpoint rep1 = new TCPEndpoint("1", "localhost", 9000);
		TCPEndpoint rep2 = new TCPEndpoint("2", "localhost", 9001);
		TCPEndpoint rep3 = new TCPEndpoint("3", "localhost", 9002);
		TCPEndpoint rep4 = new TCPEndpoint("4", "localhost", 9003);
		endpoints = List.of(rep1, rep2, rep3, rep4);
		nodes = new RaftNode[4];
		servers = new HashMap<>();
		try{
			nodes[0] = buildNode(rep1, endpoints);
			nodes[1] = buildNode(rep2, endpoints);
			nodes[2] = buildNode(rep3, endpoints);
			nodes[3] = buildNode(rep4, endpoints);
		} catch (Exception e){
			e.printStackTrace();
			Assertions.fail();
		}
		electLeader();

	}

	@AfterEach void cleanup(){
		for (RaftNode node : nodes){
			node.terminate();
			servers.get(node.getLocalEndpoint().getId()).stop();
		}
	}

	@Test void canBeInstantiated(){
		Assertions.assertNotNull(endpoints);
	}

	@Test void canElectLeader() throws Exception{
		boolean hasLeader = false;
		for (RaftNode node : nodes){
			if(node.getReport().join().getResult().getRole() == RaftRole.LEADER){
				hasLeader = true;
			}
		}
		Assertions.assertTrue(hasLeader);
	}






	// from Xander's test. untouched code.
	private static RaftNode buildNode(TCPEndpoint self, Collection<RaftEndpoint> allMembers)
			throws Exception {

		TCPTransport transport = new TCPTransport(self);

		// Starting the server before building the node so it's ready to receive messages from other nodes during leader election
		servers.put(self.getId(), new RaftServer(self.getPort()));

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


		servers.get(self.getId()).start(node);
		return node;
	}

	private static void electLeader() throws Exception{
		for (RaftNode node : nodes) node.start();

		Thread.sleep(4000);

		for (RaftNode node : nodes){
			if(node.getReport().join().getResult().getRole()  == RaftRole.LEADER){
				try{
					var future = node.replicate("wave from networked raft");
					Assertions.assertNotNull(future);
					var result = future.join();
					Assertions.assertNotEquals(0L, result.getCommitIndex());
				}catch (Exception e){
					System.err.println("failed to replicate " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}
