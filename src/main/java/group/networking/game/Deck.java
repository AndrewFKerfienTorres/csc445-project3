package group.networking.game;

import java.util.*;

public class Deck {

    public static final int SIZE = 52;
    private static Deck deck;

    private ArrayDeque<Card> cards;
    private ArrayList<Card> dealt;


    public void shuffle(){
        cards.addAll(dealt);
        ArrayList<Card> l = new ArrayList<>(cards);
        Collections.shuffle(l);
        cards = new ArrayDeque<Card>(l);
    }

    public Card draw(){
        if (cards.isEmpty()) return null;
        Card card = cards.pop();
        dealt.add(card);
        return card;
    }







    public ArrayDeque<Card> getCards(){
        // FOR TESTING PURPOSES;
        return cards;
    }

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

    public static Deck getInstance(){
        if (deck != null){
            return deck;
        }else{
            deck = new Deck();
            return deck;
        }
    }

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
