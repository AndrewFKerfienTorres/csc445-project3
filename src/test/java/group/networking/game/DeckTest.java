package group.networking.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DeckTest {

    private Deck deck;

    @BeforeEach void createDeck(){
        deck = Deck.getInstance();
    }

    @Test void deckCanGetInstance(){
        Assertions.assertNotNull(deck);
    }

    @Test void deckShufflesCorrectly(){

        ArrayList<Card> d1 = new ArrayList<>(deck.getCards());
        deck.shuffle(); deck.shuffle(); deck.shuffle(); // you may need to rerun this test if unlucky, or just in case
        ArrayList<Card> d2 = new ArrayList<>(deck.getCards());
        Assertions.assertFalse(d1.equals(d2));
    }

    @Test void cardReintroductionWorksAsIntended(){
        deck.draw();
        deck.draw();
        deck.draw();
        deck.draw();
        deck.shuffle();
        Assertions.assertEquals(Deck.SIZE, deck.getCards().size());
    }

    @Test void CardAmountDoesNotChangeAfterEveryShuffle(){
        int expected = 52;
        Random rng = new Random();
        Assertions.assertEquals(expected, deck.getCards().size());
        int numberOfDraws = 0;
        for (int i =0; i < 30; i++){
            numberOfDraws = rng.nextInt(1, 10);
            for (int j = 0; j < numberOfDraws; j++){
                deck.draw();
            }
            Assertions.assertNotEquals(expected, deck.getCards().size());
            deck.shuffle();
            Assertions.assertEquals(expected, deck.getCards().size());
        }
    }


}
