package group.networking.raft;

import java.util.List;
import java.util.function.Consumer;

import group.networking.game.GameState;
import io.microraft.impl.log.SnapshotChunkCollector;
import io.microraft.statemachine.StateMachine;

public class StateManager implements StateMachine{
    private GameState gameState = new GameState();

    /** TODO @param operation should be switched to a specific class **/
    @Override
    public Object runOperation(long commitIndex, Object operation) {
        System.out.println("Committed at index " + commitIndex + ": " + operation);
        return "ok";

        /*
        GameAction action = (GameAction) operation;
        switch (action.getType()) {
            case DEAL_CARDS:
                gameState.dealCards();
                break;
            case PLACE_BET:
                gameState.placeBet(action.getPlayerId(), action.getAmount());
                break;
            case HIT:
                gameState.hit(action.getPlayerId());
                break;
            case STAND:
                gameState.stand(action.getPlayerId(), action.getAmount());
                break;
            case DOUBLE_DOWN:
                gameState.doubleDown(action.getPlayerId(), action.getAmount());
                break;
         } 

        return gameState.getSummary(); 

        something like this?*/ 
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
        return null;
    }
}
