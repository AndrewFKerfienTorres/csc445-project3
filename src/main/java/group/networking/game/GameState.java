package group.networking.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {

    public enum Phase {
        WAITING,        // waiting for players to join
        BETTING,       
        DEALING,        // 2 cards dealt to each player
        PLAYER_TURNS,   // hit or stand
        DEALER_TURN,    
        PAYOUT          // round over
    }

    private Phase currentPhase = Phase.WAITING;
    private final List<String> playerIds = new ArrayList<>();
    private String currentPlayerId = null;
    private int pot = 0;

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setPhase(Phase phase) { 
        this.currentPhase = phase;
    }


    public void addPlayer(String playerId) {
        playerIds.add(playerId);
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }


    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(String playerId) {
        this.currentPlayerId = playerId;
    }


    public int getPot() {
        return pot;
    }

    public void addToPot(int amount) {
        this.pot += amount;
    }

    public String getSummary() {
        return "GameState[phase=" + currentPhase
            + ", players=" + playerIds
            + ", currentPlayer=" + currentPlayerId
            + ", pot=" + pot + "]";
    }

    @Override
    public String toString() { return getSummary(); }
}