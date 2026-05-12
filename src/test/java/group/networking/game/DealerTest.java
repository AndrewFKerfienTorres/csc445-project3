package group.networking.game;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DealerTest {

	public static Dealer dealer;

	@BeforeEach void setup(){
		dealer = Dealer.getInstance();
	}

	@AfterEach void cleanup(){
		dealer.reset();
	}



	@Test void SingletonPatternTest(){
		Assertions.assertNotNull(dealer);
	}

	@Test void CanDealToSelf(){
		Assertions.assertEquals(0, dealer.getHandValue());
		dealer.dealInitialSelf();
		Assertions.assertNotEquals(0, dealer.getHandValue());
	}






}
