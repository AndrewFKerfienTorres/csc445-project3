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

    public HandState dealInitialTo(PlayerHand player){
        Card c1 = deck.draw();
        Card c2 = deck.draw();
        player.addDrawn(c1);
        return player.addDrawn(c2);
    }

    public HandState dealTo(PlayerHand player){
        Card c = deck.draw();
        return player.addDrawn(c);
    }

    //because it is a rule that the dealer hit if <17, and stand if >= 17

    public HandState takeTurn(PlayerHand[] hands){
        if (handValue >= 17){
            return handState; // STAND
        } else{
            Card c = deck.draw();
            handValue += c.getValue();
            handState = PlayerHand.checkState(handValue);
            return handState;
        }
    }

}
