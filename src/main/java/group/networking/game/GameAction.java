package group.networking.game;

import java.io.Serializable;


public class GameAction implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        JOIN,
        PLACE_BET,
        DEAL_CARDS,
        HIT,
        STAND,
        DOUBLE_DOWN,
        NEXT_PHASE,
        LEAVE,
        LIST
    }

    private final Type type;
    private final String playerId;  
    private final int amount;       


    public GameAction(Type type, String playerId, int amount) {
        this.type = type;
        this.playerId = playerId;
        this.amount = amount;
    }

    public GameAction(Type type, String playerId) {
        this(type, playerId, 0);
    }

    public GameAction(Type type) {
        this(type, null, 0);
    }

    public Type getType() {
        return type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "GameAction[type=" + type
                + ", player=" + playerId
                + (amount > 0 ? ", amount=" + amount : "")
                + "]";
    }
}