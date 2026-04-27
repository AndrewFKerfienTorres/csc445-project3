package group.networking.game;

public class Ace extends Card{

    public static final int LOW = 1;
    public static final int HIGH = 11;

    private int state;

    public Ace(Suit suit){
        super(suit, "A");
        state = 0;
    }

    @Override
    public void setLow(){
        state = LOW;
    }
    @Override
    public void setHigh(){
        state = HIGH;
    }

    @Override
    public int getValue(){
        return state;
    }

}
