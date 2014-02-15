package daifugo.logic;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * This class carries the number of points the players have
 */
public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;
    public TreeMap<Integer, Integer> pointCount;

    public PlayerState(TreeMap pointCount) {
        this.pointCount = pointCount;
    }
}
