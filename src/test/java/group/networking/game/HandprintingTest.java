package group.networking.game;

import java.util.Arrays;

public class HandprintingTest {

	public static void main(String[] args) {
		Dealer dealer = Dealer.getInstance();
		PlayerHand player = new PlayerHand("alavaster");
		dealer.dealInitialSelf();
		dealer.dealInitialTo(player);

		System.out.printf("%s's hand:\n%s", player.getId(), player.getHand()[0]);
		System.out.print("\n\n");
		System.out.printf("dealer's hand: %s", Arrays.asList(dealer.getHand()));


	}





}
