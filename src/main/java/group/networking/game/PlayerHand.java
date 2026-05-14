package group.networking.game;

import static group.networking.game.HandState.*;

public class PlayerHand {

    //  ============== REPLACE THIS FOR SOME OTHER PLAYER ID BASED ON THE NETWORKING SIDE IF DIFFERENT =================
    private String id;
    private HandState status;
    private int currentValue;

    private String hand = "";

    public PlayerHand(String d){
        this.id = id;
        status = BELOW_LIMIT;
        currentValue = 0;
    }

    public String[] getHand(){
        return hand.split("\n");
    }

    public HandState getStatus(){
        return status;
    }

    public String getId() {
        return id;
    }

    public HandState addDrawn(Card c){
        currentValue += c.getValue();
        status = checkState(currentValue);
        hand +=  c.toString();
        return status;
    }

    public void reset(){
        currentValue = 0;
        hand = "";
        status = BELOW_LIMIT;
    }

    public int getValue(){
        return currentValue;
    }

    public static HandState checkState(int val){
        if (val == 21){
            return LIMIT;
        } else if (val < 21){
            return BELOW_LIMIT;
        } else if (val > 21){
            return ABOVE_LIMIT;
        }
        else throw new IllegalArgumentException("State could not be checked.");
    }




}
