package group.networking.game;

import static group.networking.game.HandState.*;

public class PlayerHand {

    //  ============== REPLACE THIS FOR SOME OTHER PLAYER ID BASED ON THE NETWORKING SIDE IF DIFFERENT =================
    private long id;
    private HandState status;
    private int currentValue;

    public PlayerHand(long id){
        this.id = id;
        status = BELOW_LIMIT;
        currentValue = 0;
    }
    /*
     * Storing the actual card is not needed, we only need to keep track of the value.
     * then it returns the value after checking the current value.
     *
     * This also means that this class is not responsible for keeping track the value of a given ace. remember to make
     * such a decision and ask for input before calling addDrawn().
     *
     * always check the returned value to check if the player has gone over the limit.
     * black jack at turn one would mean LIMIT is set at turn one.
     *
     * that being said if turn ==0 && status == LIMIT, then said player should win the round automatically.
     *
     */

    public HandState addDrawn(Card c){
        currentValue += c.getValue();
        if (currentValue == 21){
            status = LIMIT;
        }else if (currentValue >21){
            status = ABOVE_LIMIT;
        }else{
            if (status != BELOW_LIMIT) status = BELOW_LIMIT;
        }
        return status;
    }

    public void reset(){
        currentValue = 0;
        status = BELOW_LIMIT;
    }

    public int getValue(){
        return currentValue;
    }




}
