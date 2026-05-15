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
        int currentIndex = playerIds.indexOf(currentPlayerId);

        int nextIndex = currentIndex + 1;

        if (nextIndex < playerIds.size()) {
            currentPlayerId = playerIds.get(nextIndex);
            return false;
        } else {
            currentPlayerId = null;
            return true;
        }
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

    public String dealInitialCards(long commitIndex) {

        dealer.dealInitialSelf();
        for (PlayerHand p : players.values()){
            dealer.dealInitialTo(p);
        }

        String initialHands = "";

        initialHands += "dealer's initial hand:\n%s\n[FACE DOWN]\n".formatted(dealer.getHand()[0]);

        for (PlayerHand p : players.values()){
            initialHands += "%s's hand:\n%s\n".formatted(p.getId(), p.getHand());
        }

        return initialHands;
    }

    public boolean allBetsPlaced() {
        if (playerIds.isEmpty()) return false;

        return betByPlayer.size() == playerIds.size();
    }

    public String placeBet(String playerId, int amount) {
        int currentFunds = funds.getOrDefault(playerId, 0);
        if (currentFunds <= 0) return "not enough funds.";

        funds.put(playerId, (currentFunds-amount));
        pot += amount;

        betByPlayer.put(playerId, amount);

        return null;
        //return String.format("player %s dealt %d, current pot: %d", playerId, amount, pot);
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

        winners.removeIf(p -> p.getValue() < dealer.getHandValue() && dealer.getHandValue() <= 21);
        for (PlayerHand p : winners) {
            if (p.getValue() > dealer.getHandValue()) {
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

    public static int getHandValue(List<String> hand) {
        int value = 0;
        int aceCount = 0;

        for (String card : hand) {
            if (card.equals("J") || card.equals("Q") || card.equals("K") || card.equals("10")) {
                value += 10;
            }

            else if (card.equals("A")) {
                aceCount++;
                value += 11;
            }
            else
                value += Integer.parseInt(card);
        }

        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }

        return value;
    }
}