package group.networking.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    HashMap<String, PlayerHand> players = new HashMap<>();
    Dealer dealer = Dealer.getInstance();

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setPhase(Phase phase) { 
        this.currentPhase = phase;
    }


    public boolean addPlayer(String playerId) {
        if (playerIds.contains(playerId)) return false;
        playerIds.add(playerId);
        players.put(playerId, new PlayerHand(playerId));
        return true;
    }

    public boolean hasPlayer(String playerId) {
        return playerIds.contains(playerId);
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

    public boolean stand(String playerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stand'");
    }

    public boolean bust(String playerId) {
        PlayerHand p = players.get(playerId);
        return (p.getStatus() == HandState.ABOVE_LIMIT);
    }

    public List<String> getHand(String playerId) {
        PlayerHand p = players.get(playerId);
        return Arrays.asList(p.getHand());
    }

    public int hit(String playerId) {
        PlayerHand p = players.get(playerId);
        dealer.dealTo(p);
        return p.getValue();
    }

    public void dealInitialCards(long commitIndex) {

        // game part

        dealer.dealInitialSelf();
        for (PlayerHand p : players.values()){
            dealer.dealInitialTo(p);
        }



        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'dealInitialCards'");
    }

    public boolean allBetsPlaced() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'allBetsPlaced'");
    }

    public String placeBet(String playerId, int amount) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'placeBet'");
    }

    public String getDealerHand() {

        // isn't this supposed to be List<String> like player's hand?

        String dealerHand = "";
        for (String s : dealer.getHand()){
            dealerHand += s + "\n";
        }
        return dealerHand;

    }

    public String calculatePayout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'calculatePayout'");
    }

    public void runDealerTurn() {

        dealer.takeTurn();


    }

    public void resetForNextRound() {

        dealer.reset();


    }


    public String doubleDown(String playerId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doubleDown'");
    }

    public static int getHandValue(int dealerHand) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHandValue'");
    }

}