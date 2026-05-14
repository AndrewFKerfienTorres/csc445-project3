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
    HashMap<String, Integer> betByPlayer = new HashMap<>();
    HashMap<String, Integer> funds = new HashMap<>();

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
        funds.put(playerId, 10000);
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
    public String toString() {
        return getSummary();
    }

    public boolean stand(String playerId) {


        return true; // TODO Is this for state not related to game objects?

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

        dealer.dealInitialSelf();
        for (PlayerHand p : players.values()){
            dealer.dealInitialTo(p);
        }


    }

    public boolean allBetsPlaced() {

        //TODO ==========================================================

        for (Integer i : betByPlayer.values()){
            if (i <= 0) return false;
        }

        return true;
    }

    public String placeBet(String playerId, int amount) {

        //TODO ==========================================================
        //TODO I guess we'll need an initial amount of points for every player?

        pot += amount;
        funds.put(playerId, (funds.getOrDefault(playerId, 0)-1));

        if (funds.get(playerId) <= 0) return "not enough funds.";

        betByPlayer.put(playerId, (betByPlayer.getOrDefault(playerId, 0) + amount));

        return String.format("player %s dealt %d, current pot: %d", playerId, amount, pot);
    }

    public List<String> getDealerHand() {

       return Arrays.asList(dealer.getHand());

    }

    public String calculatePayout() {

        List<PlayerHand> winners = new ArrayList<>();
        boolean dealerIsWinner = false;

        for (PlayerHand p : players.values()){
            if (p.getStatus() == HandState.ABOVE_LIMIT) continue;
            winners.add(p);
        }

        if (dealer.getHandValue() > 21) dealerIsWinner = false;
        else dealerIsWinner = true;

        for (PlayerHand p : winners){
            if (p.getValue() < dealer.getHandValue() && dealer.getHandValue() <= 21){
                winners.remove(p);
            }else if (p.getValue() > dealer.getHandValue()){
                dealerIsWinner = false;
            }
        }

        int perPlayerReward;
        int numberOfSplits = winners.size();

        if(dealerIsWinner) numberOfSplits++;

        perPlayerReward = (int) Math.floor((double) pot / numberOfSplits);

        for (PlayerHand p : winners){
            String id = p.getId();
            funds.put(id, funds.get(id) + perPlayerReward);
        }

        return String.valueOf(perPlayerReward);
    }

    public void runDealerTurn() {

        dealer.takeTurn();


    }

    public void resetForNextRound() {

        dealer.reset();
        pot = 0;
        betByPlayer.clear();

    }


    public String doubleDown(String playerId) {

        int bet = betByPlayer.get(playerId);
        if (funds.get(playerId) - bet < 0) return "not enough funds.";

        funds.put(playerId, funds.get(playerId) - bet);
        pot += bet;
        betByPlayer.put(playerId, betByPlayer.get(playerId)*2);

        return String.format("%s doubled down! current pot: %d", playerId, pot);

    }

    public static int getHandValue(int dealerHand) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHandValue'");
    }

}