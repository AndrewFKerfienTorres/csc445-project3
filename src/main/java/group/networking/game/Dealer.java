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
        if(isAce(card1)){
            card1.setHigh();
        }
        Card card2 = deck.draw();
        if (isAce(card2)){
            if(isAce(card1)){
                card2.setLow();
            }else{
                card2.setHigh();
            }
        }

        handValue += (card1.getValue() + card2.getValue());
        handState = PlayerHand.checkState(handValue);
        return handState;
    }

    public HandState dealInitialTo(PlayerHand player){
        Card c1 = deck.draw();
        if (isAce(c1)){
            c1.setHigh();
        }
        Card c2 = deck.draw();
        if (isAce(c2)){
            if(isAce(c1)){
                c2.setLow();
            }else{
                c2.setHigh();
            }
        }
        player.addDrawn(c1);
        return player.addDrawn(c2);
    }

    public HandState dealTo(PlayerHand player){
        Card c = deck.draw();
        return player.addDrawn(c);
    }

    //because it is a rule that the dealer hit if <17, and stand if >= 17

    public HandState takeTurn(){
        if (handValue >= 17){
            return handState; // STAND
        } else{
            Card c = deck.draw();
            if (isAce(c)){
                if (handValue + 11 > 21){
                    c.setLow();
                } else{
                    c.setHigh();
                }
            }

            handValue += c.getValue();
            handState = PlayerHand.checkState(handValue);
            return handState;
        }
    }

    public int getHandValue(){
        return handValue;
    }

    public void reset(){
        handState = HandState.BELOW_LIMIT;
        handValue = 0;
        deck.shuffle();
    }

    private boolean isAce(Card c){
        if (c.getRank().equalsIgnoreCase("a")) return true;
        else return false;
    }

}
