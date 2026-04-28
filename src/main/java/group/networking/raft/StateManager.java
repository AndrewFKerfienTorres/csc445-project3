package group.networking.raft;

import io.microraft.impl.log.SnapshotChunkCollector;

public class StateManager {
    private GameState gameState = new GameState();

    @Override
    public Object runOperation(CommitRaiser commitRaiser, Object operation) {
        GameAction action = (GameAction) operation;
        
        switch (action.getType()) {
            case DEAL_CARDS:
                gameState.dealCards();
                break;
            case PLAYER_BET:
                gameState.placeBet(action.getPlayerId(), action.getAmount());
                break;
            case PLAYER_FOLD:
                gameState.fold(action.getPlayerId());
                break;
            case PLAYER_RAISE:
                gameState.raise(action.getPlayerId(), action.getAmount());
                break;
            // etc...
        }
        return gameState.getSummary(); // returned to the client
    }

    @Override
    public void takeSnapshot(SnapshotChunkCollector collector) {
        collector.collectSnapshotChunk(serialize(gameState));
    }

    @Override
    public void installSnapshot(SnapshotChunkIter iter) {
        gameState = deserialize(iter.next());
    }
}
