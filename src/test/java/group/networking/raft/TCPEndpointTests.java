package group.networking.raft;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TCPEndpointTests {

	TCPEndpoint ep = new TCPEndpoint("1", "localhost", 9000);

	@Test void CanBeInstantiatedProperly(){
		Assertions.assertNotNull(ep);
		Assertions.assertEquals("1", ep.getId());
		Assertions.assertEquals("localhost", ep.getHost());
		Assertions.assertEquals(9000, ep.getPort());
	}

	@Test void TestBasicMethods(){
		TCPEndpoint ep2 = new TCPEndpoint("2", "localhost", 9001);
		Assertions.assertEquals(ep, ep);
		Assertions.assertNotEquals(ep, ep2);
		Assertions.assertNotEquals(new Object(), ep);
		String string = ep.toString();
		String testerString = "Node-%s(%s:%d)".formatted(ep.getId(), ep.getHost(), ep.getPort());
		Assertions.assertEquals(testerString, string);
	}








}
