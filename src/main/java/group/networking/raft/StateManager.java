package group.networking.raft;

import java.util.List;
import java.util.function.Consumer;

import group.networking.game.GameAction;
import group.networking.game.GameState;
import io.microraft.statemachine.StateMachine;


public class StateManager implements StateMachine {

    private GameState gameState = new GameState();

    //                        TODO: The result currently only shows up for one
    //                        TODO  of the hosts
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
                    System.out.println("\n[Game] " + "Game already started. No new players can join.");
                    return "Game already started. No new players can join.";
                }
                boolean added = gameState.addPlayer(playerId);
                if (!added) return playerId + " is already in the lobby.";

                System.out.println("\n[Game] " + playerId + " joined the lobby. Players: " + gameState.getPlayerIds());
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

                    System.out.println("\n[Game] " + msg + ". All bets placed — dealing cards now!");
                    return msg + ". All bets placed — dealing cards now!";
                }
                System.out.println("\n[Game] " + msg + ". Waiting for others to bet.");
                return msg + ". Waiting for others to bet.";

            case DEAL_CARDS:
                if (phase != GameState.Phase.DEALING) {
                    return "Not in dealing phase (current: " + phase + ")";
                }
                String initialHands = gameState.dealInitialCards(commitIndex);
		        gameState.startPlayerTurns();
                gameState.setPhase(GameState.Phase.PLAYER_TURNS);
                StringBuilder sb = new StringBuilder("Cards dealt!\n");
                sb.append(gameState.getSummary());

                sb.append(initialHands);

                
                String current = gameState.getCurrentPlayerId();
                if (current != null) sb.append("\n→ ").append(current).append("'s turn. Hit or stand?");
                System.out.println("\n[Game] " + sb.toString());
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
                        System.out.println("\n[Game] " + bustMsg);
                        return bustMsg + "\n" + runDealerAndPayout();
                    }
                    System.out.println("\n[Game] " + bustMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.");
                    return bustMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";
                }

                String hitMsg = playerId + " drew → " + handStr + " = " + value;
                if (value == 21) {
                    boolean allDone = gameState.stand(playerId);
                    hitMsg += " (21 — auto-stand)";
                    if (allDone) return hitMsg + "\n" + runDealerAndPayout();
                    
                    System.out.println("\n[Game] " + hitMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.");
                    return hitMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";
                }
                System.out.println("\n[Game] " + hitMsg + ". Hit or stand?");
                return hitMsg + ". Hit or stand?";

            case STAND:
                if (phase != GameState.Phase.PLAYER_TURNS) {
                    return "Not your turn yet (phase: " + phase + ")";
                }
                if (!playerId.equals(gameState.getCurrentPlayerId())) {
                    return "It's not your turn. Waiting for: " + gameState.getCurrentPlayerId();
                }

                boolean allDone = gameState.stand(playerId);
                String standMsg = playerId + " stands with " + gameState.getHand(playerId) + " = " + GameState.getHandValue(gameState.getHand(playerId));

                if (allDone) {
                    System.out.println("\n[Game] " + standMsg);
                    return standMsg + "\n" + runDealerAndPayout();
                }
                System.out.println("\n[Game] " + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.");
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
                String doubleMsg = playerId + " doubles down → " + hand + " = " + handValue + (handValue > 21 ? " BUST!" : "");

                boolean allDoneDouble = gameState.getCurrentPlayerId() == null;
                if (allDoneDouble) {
                    System.out.println("\n[Game] " + doubleMsg);
                    return doubleMsg + "\n" + runDealerAndPayout();
                }
                System.out.println("\n[Game] " + doubleMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.");
                return doubleMsg + "\n→ " + gameState.getCurrentPlayerId() + "'s turn.";

            case NEXT_PHASE:
                switch (phase) {
                    case WAITING:
                        if (gameState.getPlayerIds().isEmpty()) {
                            return "No players have joined yet.";
                        }
                        gameState.setPhase(GameState.Phase.BETTING);
                        System.out.println("\n[Game] " + "Game started! " + gameState.getPlayerIds().size() + " players. Place your bets!");
                        return "Game started! " + gameState.getPlayerIds().size() + " players. Place your bets!";
                    case PAYOUT:
                        gameState.resetForNextRound();
                        gameState.setPhase(GameState.Phase.BETTING);
                        System.out.println("\n[Game] " + "New round! Place your bets.");
                        return "New round! Place your bets.";
                    default:
                        return "NEXT_PHASE not valid in phase: " + phase;
                }
            case LEAVE:
                boolean shouldAdvance = gameState.removePlayer(playerId);
                String leaveResult;

                if (gameState.getPlayerIds().isEmpty()) {
                    gameState.setPhase(GameState.Phase.WAITING);
                    leaveResult = playerId + " left. No players remaining.";
                } else if (shouldAdvance) {
                    GameState.Phase currentPhase2 = gameState.getCurrentPhase();
                    if (currentPhase2 == GameState.Phase.BETTING) {
                        gameState.setPhase(GameState.Phase.DEALING);
                        leaveResult = playerId + " left. All remaining players have bet — dealing!";
                    } else if (currentPhase2 == GameState.Phase.PLAYER_TURNS) {
                        String next = gameState.getCurrentPlayerId();
                        if (next == null) {
                            leaveResult = playerId + " left.\n" + runDealerAndPayout();
                        } else {
                            leaveResult = playerId + " left. -> " + next + "'s turn.";
                        }
                    } else {
                        leaveResult = playerId + " left. Players: " + gameState.getPlayerIds();
                    }
                } else {
                    leaveResult = playerId + " left. Players: " + gameState.getPlayerIds();
                }

                System.out.println("\n[Game] " + leaveResult);
                return leaveResult;
            case LIST:
                return gameState.getPlayerIds();

        default:
            return "Unknown action type: " + action.getType();
        }
        
    }


    private String runDealerAndPayout() {
        gameState.setPhase(GameState.Phase.DEALER_TURN);
        gameState.runDealerTurn();

        String result = gameState.calculatePayout();
        gameState.setPhase(GameState.Phase.PAYOUT);

        System.out.println("\n[Game] " +"Dealer plays: " + gameState.getDealerHand()
                + " = " + GameState.getHandValue(gameState.getDealerHand())
                + "\n" + result
                + "\nType 'next' to start a new round.");

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