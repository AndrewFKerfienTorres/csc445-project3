package group.networking.game;

public class Ace extends Card{

    public static final int LOW = 1;
    public static final int HIGH = 11;

    private int state;

    public Ace(Suit suit, boolean state){
        super(suit, "A");
    }

    public void setLow(){
        state = LOW;
    }

    public void setHigh(){
        state = HIGH;
    }
    
    @Override
    public int getValue(){
        return state;
    }

}
