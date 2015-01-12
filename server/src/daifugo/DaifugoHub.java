package daifugo;

import daifugo.messages.*;

import java.io.*;
import java.util.*;

import common.*;
import daifugo.logic.*;
import daifugo.parts.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaifugoHub extends Hub {

    private final Tracker tracker = Tracker.getInstance();
    private volatile String currentPlayer;
    private final int portRecord;
    private volatile Validator validator = new Validator(this);
    static final int TEST = 23548;
    static final int BLITZ = 23549;
    static final int TOURNAMENT = 23550;
    static final int ACCOUNT = 23551;
    private boolean tournament = false;

    public DaifugoHub(int port) throws IOException {
        super(port);
        portRecord = port;
        configureValidatorAndPointOptions(port);
    }

    private void configureValidatorAndPointOptions(int port) {
        if (port == DaifugoHub.TEST) {
            validator.setWinPoints(30);
            validator.setTime(40);
        } else if (port == DaifugoHub.BLITZ) {
            validator.setWinPoints(25);
            validator.setTime(21);
            tournament = true;
        } else if (port == DaifugoHub.TOURNAMENT) {
            validator.setWinPoints(30);
            validator.setTime(35);
            tournament = true;
        }
    }

    @Override
    protected String extraHandshake(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        try {
            String name = (String) in.readObject();
            String password = (String) in.readObject();
            String authentic = getTracker().authenticate(name, password, portRecord);
            switch (authentic) {
                case "accountCreated":
                    out.writeObject("successAccount");
                    return "accountCreated";
                case "failedAccountCreation":
                    out.writeObject("failAccount");
                    throw new IOException(name + " and " + password + " has failed to create an account.");
                case "authenticated":
                    out.writeObject("authenticated");
                    return name;
                case "combinationError":
                    out.writeObject("combinationError");
                    throw new IOException(name + " and " + password + " combination is generating an error.");
                case "doubleLoginError":
                    out.writeObject("doubleLoginError");
                    throw new IOException(name + " is already logged in.");
                case "pointsError":
                    out.writeObject("pointsError");
                    throw new IOException(name + " needs more points to access this room");
                default:
                    out.writeObject("closedError");
                    throw new IOException("Room is currently closed.");
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IOException("Error while setting up connection: " + e);
        }
    }

    /**
     *
     * This method is overridden to provide support for PrivateMessages. If a
     * PrivateMessage is received from some client, this method will set the
     * senderID field in the message to be the ID number of the client who sent
     * the message. It will then send the message on to the specified recipient.
     * If some other type of message is received, it is handled by the
     * messageReceived() method in the superclass (which will wrap it in a
     * ForwardedMessage and send it to all connected clients).
     */
    @Override
    protected void messageReceived(String playerID, Object message) {
        if (message instanceof PrivateMessage) {
            PrivateMessage pm = (PrivateMessage) message;
        } else if (message.equals("deal2357")) {
            setAutoreset(true);
            getValidator().deal(playerID);
        } else if (message.equals("pass2357")) {
            if (getCurrentPlayerName() == null ? playerID != null : !currentPlayer.equals(playerID)) {
                sendToOne(playerID, "It is not your turn.");
                return;
            }
            getValidator().ending();
        } else if (message instanceof Hand) {
            if (getCurrentPlayerName() == null ? playerID != null : !currentPlayer.equals(playerID)) {
                sendToOne(playerID, "It is not your turn.");
                return;
            }
            try {
                getValidator().process((Hand) message);
            } catch (Exception ex) {
                Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (message instanceof Card) {
            if (getCurrentPlayerName() == null ? playerID != null : !currentPlayer.equals(playerID)) {
                sendToOne(playerID, "It is not your turn.");
                return;
            }
            validator.jokerReceived++; // If joker recieved, notify validator here.
            Card card = (Card) message;
            sendToAll(getCurrentPlayerName() + " replaces a joker with the " + card.toString());
        } else if (message instanceof Boolean) {
            boolean hold = (Boolean) message;
            validator.setKakumei(hold);
        } else if (message.equals("reload server")) {
            this.sendToAll("Server is reloading...");
            this.tracker.readXML();
        } else {
            super.messageReceived(playerID, message);
        }
    }

    /**
     * @author This method has been borrowed from the following book:
     * Introduction to Programming Using Java, Sixth Edition.
     *
     * This method is called when a new client connects. It is called after the
     * extraHandshake() method has been called, so that the client's name has
     * already been added to userNameMap. This method creates a
     * ClientConnectedMessage and sends it to all connected clients to announce
     * the new participant in the chat room.
     * @param playerID
     */
    @Override
    protected void playerConnected(String playerID) {
        resetOutput(); // Reset the output stream before resending player points.
        this.sendPlayerPoints();
        resetOutput(); // Reset the output stream before resending userNameMap.
        sendToAll(new ClientConnectedMessage(playerID));
    }

    /**
     * @author This method has been adapted from the following book:
     * Introduction to Programming Using Java, Sixth Edition.
     *
     * This method is called when a client has been disconnected from this hub.
     * It removes the client from the userNameMap and sends a
     * ClientDisconnectMessage to all connected players to announce the fact
     * that the client has left the chat room.
     *
     * If the user quits during a game, that player, if not just viewing, will
     * lose points.
     * @param playerID
     */
    @Override
    public void playerDisconnected(String playerID) {
        if (getValidator().isGameInProgress()) {
            String[] players = getValidator().getConnectedPlayersID();
            for (String player : players) {
                if (playerID == null ? player == null : playerID.equals(player)) {
                    getValidator().restart();
                    try {
                        if (tournament) {
                            getTracker().updateTournamentPoints(playerID, -10);
                        } else {
                            getTracker().updateRegularPoints(playerID, -10);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else if (!this.validator.dealAgree.isEmpty() || this.validator.dealAgree != null) {
            for (String player : this.validator.dealAgree) {
                if (player.equals(playerID)) {
                    getValidator().restart();
                    try {
                        if (tournament) {
                            getTracker().updateTournamentPoints(playerID, -10);
                        } else {
                            getTracker().updateRegularPoints(playerID, -10);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        getTracker().logout(playerID); // Make sure the status of the player is set to logged out.
        resetOutput(); // Reset the output stream before resending player points.
        this.sendPlayerPoints();
        resetOutput(); // Reset the output stream before resending userNameMap.
        sendToAll(new ClientDisconnectedMessage(playerID));
        if (!(getValidator().isGameInProgress())) {
            getValidator().restart();
        }
    }

    public void sendState() {
        String[] players = this.getPlayerList();
        String[] connectedPlayers = getValidator().getConnectedPlayersID();
        if (players.length == connectedPlayers.length) {
            for (String player : players) {
                if (getCurrentPlayerName() == null ? player == null : getCurrentPlayerName().equals(player)) {
                    sendToOne(getCurrentPlayerName(), getValidator().getCurrentPlayerState());
                } else {
                    sendToOne(player, getValidator().getWaitingPlayerState(player));
                }
            }
        } else {
            for (String player : players) {
                boolean current = false; // If players[i] is in the current players, then this becomes true
                for (String connectedPlayer : connectedPlayers) {
                    if (connectedPlayer == null ? player == null : connectedPlayer.equals(player)) {
                        current = true;
                        /* If this block is entered, then the player is currently actually playing */
                        if (getCurrentPlayerName() == null ? player == null : getCurrentPlayerName().equals(player)) {
                            sendToOne(getCurrentPlayerName(), getValidator().getCurrentPlayerState());
                        } else {
                            sendToOne(player, getValidator().getWaitingPlayerState(player));
                        }
                        break;
                    }
                }
                if (!current) {
                    /* If player is just visiting */
                    sendToOne(player, getValidator().getVisitngPlayerState(player));
                }
            }
        }
    }

    /**
     * This method reopens the connection on the current port to allow for more
     * players to come in. The connection will be closed again once "start" is
     * pressed.
     */
    public void restart() {
        try {
            restartServer(portRecord);
        } catch (IOException ex) {
            Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Switches to the next persons turn. This method makes sure that the turn
     * only goes to currently playing players.
     */
    public void switchTurn() {
        String[] players = getValidator().getConnectedPlayersID();
        boolean matched = false; // If no matches occured then make sure it goes to existing player
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null ? getCurrentPlayerName() == null : players[i].equals(getCurrentPlayerName())) {
                if (i == (players.length - 1)) {
                    setCurrentPlayer(players[0]);
                    matched = true;
                    break;
                } else {
                    setCurrentPlayer(players[i + 1]);
                    matched = true;
                    break;
                }
            }
        }
        if (!matched) {
            setCurrentPlayer(players[0]); // If currentPlayer does not exist, make it the turn of the first player who does
        }
    }

    public String getCurrentPlayerName() {
        return currentPlayer;
    }

    public int getCurrentPlayerIndex() {
        int index = 0;
        String[] players = getValidator().getConnectedPlayersID();
        for (int i = 0; i < players.length; i++) {
            if (getCurrentPlayerName() == null ? players[i] == null : getCurrentPlayerName().equals(players[i])) {
                index = i;
            }
        }
        return index;
    }

    public String getNextPlayerName() {
        String[] players = getValidator().getConnectedPlayersID();
        String nextPlayer = players[0];
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null ? getCurrentPlayerName() == null : players[i].equals(getCurrentPlayerName())) {
                if (i == (players.length - 1)) {
                    nextPlayer = players[0];
                    break;
                } else {
                    nextPlayer = players[i + 1];
                    break;
                }
            }
        }
        return nextPlayer;
    }

    public int getNumberOfConnectedPlayers() {
        return getValidator().getConnectedPlayersID().length;
    }

    public String getPlayerName(int position) {
        String[] players = this.validator.getConnectedPlayersID();
        String id = players[position];
        return id;
    }

    public void updatePoints(String winnerID, int change) {
        try {
            if (!tournament) {
                getTracker().updateRegularPoints(winnerID, change);
            } else {
                Calendar tokyo = Calendar.getInstance();
                tokyo.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
                int hour = tokyo.get(Calendar.HOUR_OF_DAY);
                int day = tokyo.get(Calendar.DAY_OF_MONTH);
                if (hour == 21 && (day % 2 == 0)) {
                    getTracker().updateTournamentPoints(winnerID, change);
                } else if (hour == 20 && (day % 2 == 1)) {
                    getTracker().updateTournamentPoints(winnerID, change);
                } else {
                    getTracker().updateTournamentPoints(winnerID, 0);
                    sendToAll("It is currently not tournament hour. No points were added or deducted.");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendPlayerPoints();
    }

    public void sendPlayerPoints() {
        try {
            if (!tournament) {
                String[] players = this.getPlayerList();
                TreeMap<String, Integer> pointCount = new TreeMap<>();
                for (int i = 0; i < players.length; i++) {
                    pointCount.put(players[i], getTracker().getRegularPoints(players[i]));
                }
                sendToAll(new PlayerState(pointCount));
            } else {
                String[] players = this.getPlayerList();
                TreeMap<String, Integer> pointCount = new TreeMap<>();
                for (String player : players) {
                    pointCount.put(player, getTracker().getTournamentPoints(player));
                }
                sendToAll(new PlayerState(pointCount));
            }
        } catch (Exception ex) {
            Logger.getLogger(DaifugoHub.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Validator getValidator() {
        return validator;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

}
