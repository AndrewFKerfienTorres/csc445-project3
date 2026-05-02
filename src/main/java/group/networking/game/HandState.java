package group.networking.game;


/*                                          HAND STATE
 *                         BELOW LIMIT -> value is <21, therefore player is good to go.
 *                         LIMIT       -> value is 21, if turn 1 automatic win, else tell player they're at limit.
 *                         ABOVE LIMIT -> value is >21, automatic loss for the player.
 */



public enum HandState {
    BELOW_LIMIT,
    LIMIT,
    ABOVE_LIMIT
}
