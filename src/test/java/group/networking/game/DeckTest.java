package group.networking.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;

public class DeckTest {

    private Deck deck;

    @BeforeEach
    void createDeck(){
        deck = Deck.getInstance();
    }

    @Test
    void deckCanGetInstance(){
        Assertions.assertNotNull(deck);
    }

    @Test
    void deckShufflesCorrectly(){
        ArrayDeque<Card> d1, d2;
        d1 = deck.getCards();
        deck.shuffle();
        d2 = deck.getCards();
        int i = 0;
        for  (i = 0; i < Deck.SIZE; i++){
            if (d1.pop() == d2.pop()) break;
        }
        Assertions.assertNotEquals(Deck.SIZE -1, i);
    }

    @Test
    void cardReintroductionWorksAsIntended(){
        deck.draw();
        deck.draw();
        deck.draw();
        deck.draw();
        deck.shuffle();
        Assertions.assertEquals(Deck.SIZE, deck.getCards().size());
    }




}
