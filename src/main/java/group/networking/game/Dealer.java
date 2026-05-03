package group.networking.game;

public class Dealer {

    private static Dealer dealer;
    private static Deck deck;

    private int handValue;
    private HandState handState;

    private Dealer(){
        deck = Deck.getInstance();
        handValue = 0;
        handState = HandState.BELOW_LIMIT;
    }

    public static Dealer getInstance(){
        if (dealer == null){
            dealer = new Dealer();
        }
        return dealer;
    }

    public HandState dealInitialSelf(){
        Card card1 = deck.draw();
        Card card2 = deck.draw();
        handValue += (card1.getValue() + card2.getValue());
        handState = PlayerHand.checkState(handValue);
        return handState;
    }

    public HandState dealTo(PlayerHand player){
        Card c1 = deck.draw();
        Card c2 = deck.draw();
        player.addDrawn(c1);
        return player.addDrawn(c2);
    }




}
