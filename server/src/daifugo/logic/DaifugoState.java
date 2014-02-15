package daifugo.logic;

import daifugo.parts.*;
import java.io.Serializable;
import java.util.TreeMap;

/**
 * This class carries the various state information that is required by all the players
 */
public class DaifugoState implements Serializable {

    private static final long serialVersionUID = 1L;
    public final static int DEAL = 0; // When the deal starts.
    public final static int PLAY = 2; // When you are playing.
    public final static int WAIT_FOR_PLAY = 3; // When your are waiting for your turn.
    public final static int VISIT = 4; // Hwne you are just viewing the game or "just visiting"
    public Hand phand;  // Player hand
    public Hand thand;  // Table hand
    public Hand dhand;  // Discard Pile
    public int status;         // One player's game status; one of the constants defined in this class.
    public int table; // Amount of cards the table
    public int time;
    public String currentPlayerID; // The ID of the current player
    public TreeMap<String, Integer> opc;
    public String[] connectedPlayersID; // The IDs of the currently playing players


    /* Restrictions */
    public boolean shibars;
    public boolean kakumei;
    public boolean jack;
    public boolean geki;
    public boolean impossible;
    public boolean giveAble;
    public boolean throwAble;
    public boolean played;

    public DaifugoState(String currentPlayerID, String[] connectedPlayersID, Hand phand, Hand thand, int table, Hand dhand, int status, TreeMap<String, Integer> opc, int time,
            boolean shibars, boolean kakumei, boolean jack,
            boolean geki, boolean impossible, boolean giveAble, boolean throwAble,
            boolean played) {

        this.currentPlayerID = currentPlayerID;
        this.connectedPlayersID = connectedPlayersID;
        this.phand = phand;
        this.thand = thand;
        this.table = table;
        this.dhand = dhand;
        this.status = status;
        this.time = time;
        this.opc = opc;

        /* Restrictions */
        this.shibars = shibars;
        this.kakumei = kakumei;
        this.jack = jack;
        this.geki = geki;
        this.impossible = impossible;

        this.giveAble = giveAble;
        this.throwAble = throwAble;

        this.played = played;
    }

}
