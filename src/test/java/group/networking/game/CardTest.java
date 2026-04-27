package group.networking.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CardTest {

    @Test
    void canGetCards(){
        Card c1 = new Card(Suit.SPADES, "10");
        Card c2 = new Card(Suit.SPADES, "q");
        Card c3 = new Ace(Suit.HEARTS);
        Card c4 = new Card(Suit.HEARTS, "z");

        Assertions.assertNotNull(c1);
        Assertions.assertNotNull(c2);
        Assertions.assertNotNull(c3);

        //both king, queen, and also jack and 10 are 10 points
        Assertions.assertEquals(c1.getValue(), c2.getValue());
        Assertions.assertEquals(c1.getSuit(), c2.getSuit());

        c3.setLow();
        int p = c3.getValue();
        c3.setHigh();
        int p2 = c3.getValue();
        Assertions.assertEquals(11, p2);
        Assertions.assertEquals(1, p);
        Assertions.assertEquals(0, c4.getValue());
    }
}
