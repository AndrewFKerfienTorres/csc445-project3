package group.networking.game;

import java.util.*;

public class Deck {

    public static final int SIZE = 52;
    private static Deck deck;

    private ArrayDeque<Card> cards;
    private ArrayList<Card> dealt;

    public static Deck getInstance(){
        if (deck != null){
            return deck;
        }else{
            deck = new Deck();
            return deck;
        }
    }

    /*
     * Remove all the cards from the dealt list and re-introduce them into the deck
     * thereafter shuffle the deck.
     */

    public void shuffle(){
        cards.addAll(dealt);
        ArrayList<Card> l = new ArrayList<>(cards);
        Collections.shuffle(l);
        cards = new ArrayDeque<Card>(l);
        dealt.clear();
    }

    /*
     * Remove a card from the deck, and return it to give it to the player.
     * Also places said card on the dealt list to keep track of teh card.
     */

    public Card draw(){
        if (cards.isEmpty()) return null;
        Card card = cards.pop();
        dealt.add(card);
        return card;
    }


    // =================================== DO NOT NEED TO CHECK THIS ================================================

    public ArrayDeque<Card> getCards(){
        // FOR TESTING PURPOSES;
        // SERIOUSLY, JUST USE THE SAFER CLASSES INSTEAD OF WORKING WITH CARDS DIRECTLY
        return cards;
    }

    // this just creates the deck by putting together the card of every rank for every suit.

    private Deck(){
        Card[] cards = new Card[SIZE];
        int i = 0;
        for (Card c : createForSuit(Suit.CLUBS)){
            cards[i] = c;
            i++;
        }
        for (Card c : createForSuit(Suit.HEARTS)){
            cards[i] = c;
            i++;
        }
        for (Card c : createForSuit(Suit.DIAMONDS)){
            cards[i] = c;
            i++;
        }
        for (Card c : createForSuit(Suit.SPADES)){
            cards[i] = c;
            i++;
        }
        this.cards = new ArrayDeque<>(Arrays.asList(cards));
        dealt = new ArrayList<>();
    }

    // Makes every card for the given suit.

    private Card[] createForSuit(Suit suit){
        Card[] cardsInSuit = {
                new Card(suit, "2"),
                new Card(suit, "3"),
                new Card(suit, "4"),
                new Card(suit, "5"),
                new Card(suit, "6"),
                new Card(suit, "7"),
                new Card(suit, "8"),
                new Card(suit, "9"),
                new Card(suit, "10"),
                new Card(suit, "j"),
                new Card(suit, "q"),
                new Card(suit, "k"),
                new Ace(suit)
        };
        return cardsInSuit;
    }



}
