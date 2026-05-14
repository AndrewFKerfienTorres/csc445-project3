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
	@Test void DealingInitialCardsToPlayers(){

		PlayerHand[] players = {new PlayerHand(0L), new PlayerHand(1L), new PlayerHand(2L)};

		dealer.dealInitialSelf();
		for (PlayerHand p: players){
			HandState state = dealer.dealInitialTo(p);
			Assertions.assertNotEquals(HandState.ABOVE_LIMIT, state);
		}

		int points = players[0].getValue();
		dealer.dealTo(players[0]);
		int points2 = players[0].getValue();
		Assertions.assertNotEquals(points, points2);
	}

	// this is to make sure that most possibilities are covered.
	@Test void DealerChoosesActionCorrectly(){ for (int i =0; i < 50; i++){
		dealer.dealInitialSelf();
		int a = dealer.getHandValue();
		if (a >= 17){
			dealer.takeTurn();
			int b = dealer.getHandValue();
			Assertions.assertEquals(a, b);
		} else if (a > 0 && a < 17){
			dealer.takeTurn();
			int b = dealer.getHandValue();
			Assertions.assertNotEquals(a, b);
		}else if (a <= 0){
			Assertions.fail();
		}
		dealer.reset();
		}}


}
