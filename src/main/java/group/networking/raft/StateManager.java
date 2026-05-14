package group.networking.raft;

import group.networking.game.GameAction;
import group.networking.game.GameState;
import io.microraft.statemachine.StateMachine;

import java.util.List;
import java.util.function.Consumer;


public class StateManager implements StateMachine {

    private GameState gameState = new GameState();

    @Override
    public Object runOperation(long commitIndex, Object operation) {
        if (!(operation instanceof GameAction action)) {
            // MicroRaft internal marker (e.g. getNewTermOperation result) — ignore
            return "ok";
        }

        String playerId = action.getPlayerId();
        GameState.Phase phase = gameState.getCurrentPhase();

        switch (action.getType()) {
            case JOIN:
                if (phase != GameState.Phase.WAITING) {
                    return "Game already started. No new players can join.";
                }
                boolean added = gameState.addPlayer(playerId);
                if (!added) return playerId + " is already in the lobby.";
                return playerId + " joined the lobby. Players: " + gameState.getPlayerIds();

            case PLACE_BET:
                if (phase != GameState.Phase.BETTING) {
                    return "Not in betting phase (current: " + phase + ")";
                }
                String betError = gameState.placeBet(playerId, action.getAmount());
                if (betError != null) return betError;

                String msg = playerId + " bet $" + action.getAmount();
                if (gameState.allBetsPlaced()) {
                    gameState.setPhase(GameState.Phase.DEALING);
                    return msg + ". All bets placed — dealing cards now!";
                }
                return msg + ". Waiting for others to bet.";

            case DEAL_CARDS:
                if (phase != GameState.Phase.DEALING) {
                    return "Not in dealing phase (current: " + phase + ")";
                }
                gameState.dealInitialCards(commitIndex);
                gameState.setPhase(GameState.Phase.PLAYER_TURNS);

                StringBuilder sb = new StringBuilder("Cards dealt!\n");
                sb.append(gameState.getSummary());
                String current = gameState.getCurrentPlayerId();
                if (current != null) sb.append("\n→ ").append(current).append("'s turn. Hit or stand?");
                return sb.toString();

            case HIT:
                if (phase != GameState.Phase.PLAYER_TURNS) {
                    return "Not your turn yet (phase: " + phase + ")";
                }
                if (!playerId.equals(gameState.getCurrentPlayerId())) {
                    return "It's not your turn. Waiting for: " + gameState.getCurrentPlayerId();
                }

                int value = gameState.hit(playerId);
                String handStr = gameState.getHand(playerId).toString();

                if (value > 21) {
                    boolean allDone = gameState.bust(playerId);
                    String bustMsg = playerId + " drew → " + handStr + " = " + value + " BUST!";
                    if (allDone) {
                        return bustMsg + "\n" + runDealerAndPayout();
                    }
                    return bustMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";
                }

                String hitMsg = playerId + " drew → " + handStr + " = " + value;
                if (value == 21) {
                    boolean allDone = gameState.stand(playerId);
                    hitMsg += " (21 — auto-stand)";
                    if (allDone) return hitMsg + "\n" + runDealerAndPayout();
                    return hitMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";
                }
                return hitMsg + ". Hit or stand?";

            case STAND:
                if (phase != GameState.Phase.PLAYER_TURNS) {
                    return "Not your turn yet (phase: " + phase + ")";
                }
                if (!playerId.equals(gameState.getCurrentPlayerId())) {
                    return "It's not your turn. Waiting for: " + gameState.getCurrentPlayerId();
                }

                boolean allDone = gameState.stand(playerId);
                String standMsg = playerId + " stands with " + gameState.getHand(playerId)
                        + " = " + GameState.getHandValue(gameState.getHand(playerId));

                if (allDone) {
                    return standMsg + "\n" + runDealerAndPayout();
                }
                return standMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";

            case DOUBLE_DOWN:
                if (phase != GameState.Phase.PLAYER_TURNS) {
                    return "Not your turn yet (phase: " + phase + ")";
                }
                if (!playerId.equals(gameState.getCurrentPlayerId())) {
                    return "It's not your turn. Waiting for: " + gameState.getCurrentPlayerId();
                }

                String doubleError = gameState.doubleDown(playerId);
                if (doubleError != null) return doubleError;

                List<String> hand = gameState.getHand(playerId);
                int handValue = GameState.getHandValue(hand);
                String doubleMsg = playerId + " doubles down → " + hand + " = " + handValue
                        + (handValue > 21 ? " BUST!" : "");

                boolean allDoneDouble = gameState.getCurrentPlayerId() == null;
                if (allDoneDouble) {
                    return doubleMsg + "\n" + runDealerAndPayout();
                }
                return doubleMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";

            case NEXT_PHASE:
                switch (phase) {
                    case WAITING:
                        if (gameState.getPlayerIds().isEmpty()) {
                            return "No players have joined yet.";
                        }
                        gameState.setPhase(GameState.Phase.BETTING);
                        return "Game started! " + gameState.getPlayerIds().size()
                                + " players. Place your bets!";
                    case PAYOUT:
                        gameState.resetForNextRound();
                        gameState.setPhase(GameState.Phase.BETTING);
                        return "New round! Place your bets.";
                    default:
                        return "NEXT_PHASE not valid in phase: " + phase;
                }

        default:
            return "Unknown action type: " + action.getType();
        }
    }


    private String runDealerAndPayout() {
        gameState.setPhase(GameState.Phase.DEALER_TURN);
        gameState.runDealerTurn();

        String result = gameState.calculatePayout();
        gameState.setPhase(GameState.Phase.PAYOUT);

        return "Dealer plays: " + gameState.getDealerHand()
                + " = " + GameState.getHandValue(gameState.getDealerHand())
                + "\n" + result
                + "\nType 'next' to start a new round.";
    }


    @Override
    public void takeSnapshot(long commitIndex, Consumer<Object> chunkConsumer) {
        chunkConsumer.accept(gameState);
    }

    @Override
    public void installSnapshot(long commitIndex, List<Object> chunks) {
        if (!chunks.isEmpty() && chunks.get(0) instanceof GameState restored) {
            gameState = restored;
        }
    }

    @Override
    public Object getNewTermOperation() {
        // MicroRaft requires a non-null marker for new term I guess
        return "NEW_TERM";
    }
}