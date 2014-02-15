package daifugo;

import daifugo.logic.*;
import daifugo.parts.*;
import java.awt.Color;
import java.awt.Component;
import java.util.*;
import javax.swing.*;

/**
 * This class updates the table display and the controls display based on the
 * updates sent by the client. This class also sends messages to tell the server
 * what the user wants to do.
 */
public class TableController {

    DaifugoClient client;
    TableDisplay display;
    ControlsDisplay controls;
    private DaifugoState state;
    boolean[] discard;
    private Hand now = new Hand();
    private final String jokerMessage = "Type in the card you want to replace the joker with. \nIf you type in \"joker\", the card will remain a joker. \nThe format required is as follows: (value) (suit). \nFor example: 3 clubs, 10 diamonds, ace spades. \n";
    private PlayerState point;
    private boolean sortBySuit = false;
    private int clientPoints;

    TableController(DaifugoClient client) {
        this.client = client;
        this.display = new TableDisplay(this);
        this.controls = new ControlsDisplay(this);
        newState(new DaifugoState("Error from Table", null, null, null, 0, null, 0, null, 30, false, false, false, false, false, false, false, false));
    }

    /**
     * This method is responsible for updating the displays every time a new
     * state is sent from the server.
     *
     * @param state The current state of the game sent by the server
     */
    void newState(DaifugoState state) {
        this.state = state;
        this.discard = null;

        this.controls.getStart().setEnabled(state.status == DaifugoState.DEAL);

        this.controls.getPlace().setEnabled(state.status == DaifugoState.PLAY);
        this.controls.getPass().setEnabled(state.status == DaifugoState.PLAY);

        if (this.state.status == DaifugoState.PLAY) {
            if (sortBySuit) {
                this.state.phand.sortBySuit();
            } else {
                this.state.phand.sortByValue();
            }

            handlePlay();
            updateCardCount(this.state.opc);

            handleBorderColor(this.state.currentPlayerID, this.state.connectedPlayersID);

            handleRestrictions();

            this.controls.getRemaining().setEnabled(true);
            this.controls.getRemainingLabel().setEnabled(true);
            this.controls.validate();
        }

        if (this.state.status == DaifugoState.WAIT_FOR_PLAY) {
            if (sortBySuit) {
                this.state.phand.sortBySuit();
            } else {
                this.state.phand.sortByValue();
            }

            handleWait();
            handleWaitTimer();
            updateCardCount(this.state.opc);

            handleBorderColor(this.state.currentPlayerID, this.state.connectedPlayersID);

            handleRestrictions();

            this.controls.getRemaining().setEnabled(true);
            this.controls.getRemainingLabel().setEnabled(true);
            this.controls.validate();
        }

        if (this.state.status == DaifugoState.DEAL) {
            handleWait();
            this.controls.getRemaining().setEnabled(false);
            this.controls.getRemainingLabel().setEnabled(false);
        }

        if (this.state.status == DaifugoState.VISIT) {
            handleWait();
            updateCardCount(this.state.opc);

            handleBorderColor(this.state.currentPlayerID, this.state.connectedPlayersID);

            handleRestrictions();

            this.controls.getRemaining().setEnabled(false);
            this.controls.getRemainingLabel().setEnabled(false);
            this.controls.validate();
        }

        this.display.repaint();
    }

    public void resetPlaceandRestrictions() {
        this.controls.getPlace().setText("Place");
        this.controls.getGeki().setEnabled(false);
        this.controls.getShibars().setEnabled(false);
        this.controls.getJack().setEnabled(false);
        this.controls.getKakumei().setEnabled(false);
    }

    void begin() {
        this.client.send("deal2357");
    }

    void doQuit() {
        if (this.client != null) {
            this.client.disconnect();
        }
        this.display.dispose();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    void pass() {
        this.client.send("pass2357");
    }

    void place() {
        boolean valid = process(this.discard);
        if ((this.now.getCardCount() == 0) && valid) {
            this.client.send("pass2357");
            return;
        }
        if (valid) {
            this.client.send(this.now);
        } else {
            this.controls.restartTimer(this.controls.getRemainingTime());
        }
    }

    /**
     * This method is responsible for building an actual hand out of the discard
     * array. It also takes care of which card is substituted for a joker.
     *
     * @param discard The boolean array that keeps track of which cards were
     * selected by the player.
     * @return boolean This boolean represents whether the process succeeded.
     */
    public boolean process(boolean[] discard) {
        this.now.clear();

        int handLength = 0;
        for (int i = 0; i < discard.length; i++) {
            if (discard[i]) {
                handLength++;
            }
        }
        for (int i = 0; i < discard.length; i++) {
            if (discard[i]) {
                if ((this.state.phand.getCard(i).getValue() == Card.JOKER) && (!this.state.giveAble) && (!this.state.throwAble) && (handLength != 1)) {
                    Card replacement = new Card();
                    boolean valid = false;
                    while (!valid) {
                        String card = JOptionPane.showInputDialog(jokerMessage, "");
                        if (card == null) {
                            this.client.chatController.addToTranscript("Invalid card, please retype.");
                            return false;
                        }
                        replacement = processCard(card);
                        if (replacement != null) {
                            valid = true;
                        } else {
                            this.client.chatController.addToTranscript("Invalid card, please retype.");
                            return false;
                        }
                    }
                    this.now.addCard(replacement);
                    this.client.send(replacement);
                } else {
                    this.now.addCard(this.state.phand.getCard(i));
                }
            }
        }
        return true;
    }

    void updatePlayers(PlayerState message) {
        this.point = message;
    }

    /**
     * Whenever a player logins or a game ends, this method is called to update
     * the status of the players.
     */
    void updateClients() {
        for (JPanel panel : this.display.getClientPanels()) {
            panel.removeAll();
        }
        this.display.getClientPanels().clear();

        for (Map.Entry<String, Integer> entry : this.point.pointCount.entrySet()) {
            if (entry.getKey() == null ? this.client.getID() == null : entry.getKey().equals(this.client.getID())) {
                String rank = rank(entry.getValue());
                clientPoints = entry.getValue();
                this.controls.getPoints().setText("Your rank is: " + rank + " (" + String.valueOf(entry.getValue()) + ").");
            } else {
                this.display.drawClient(entry.getKey(), entry.getKey(), entry.getValue());
            }
        }

        if ((this.state.status == DaifugoState.PLAY) || (this.state.status == DaifugoState.WAIT_FOR_PLAY)) {
            updateCardCount(this.state.opc);
        }

        this.display.redrawClients();
    }

    /**
     * The naming system of the rankings is based on the complexity of the
     * kingdoms.
     */
    String rank(int points) {
        if (points < -150) {
            return "Dangerous";
        }
        if (points < 1) {
            return "Bacteria";
        }
        if (points < 31) {
            return "Protist";
        }
        if (points < 100) {
            return "Fungi";
        }
        if (points < 300) {
            return "Plant";
        }
        if (points < 1000) {
            return "Animal";
        }
        if (points < 3000) {
            return "Human";
        }
        if (points < 10000) {
            return "Himajin";
        }
        if (points < 100000) {
            return "Addicted";
        }
        return "Unknown";
    }

    public DaifugoState getState() {
        return this.state;
    }

    private void handlePlay() {
        this.discard = new boolean[this.state.phand.getCardCount()];

        this.controls.restartTimer(this.state.time);

        if (this.state.giveAble) {
            this.controls.getPlace().setText("Give");
        }

        if ((!this.state.giveAble) && (this.state.throwAble)) {
            this.controls.getPlace().setText("Throw");
        }
    }

    private void handleWait() {
        this.controls.getPlace().setText("Place");
        this.controls.getRemaining().setText("Calculating...");
        this.controls.stopTimer();
    }

    /**
     * This method tells the table display to rewrite the number of cards each
     * player has.
     *
     * @param opc The TreeMap that maps player ID's to the number of cards they
     * have.
     */
    private void updateCardCount(TreeMap<String, Integer> opc) {

        for (JPanel oneClient : this.display.getClientPanels()) {
            for (Map.Entry<String, Integer> entry : opc.entrySet()) {
                if (entry.getKey().equals(oneClient.getName())) {
                    for (Component component : oneClient.getComponents()) {
                        if ((component.getName() != null) && (component.getName().equals("remainingCards"))) {
                            JLabel count = (JLabel) component;
                            count.setText(entry.getValue() + " cards remaining");
                        }
                    }
                }
            }

        }

        List visitorID = calculateVisitingPlayers();
        for (JPanel oneClient : this.display.getClientPanels()) {
            for (int i = 0; i < visitorID.size(); i++) {
                if (((String) visitorID.get(i)).equals(oneClient.getName())) {
                    for (Component component : oneClient.getComponents()) {
                        if ((component.getName() != null) && (component.getName().equals("remainingCards"))) {
                            JLabel count = (JLabel) component;
                            count.setText("Watching...");
                        }
                    }
                }
            }

        }

        this.display.validate();
    }

    /**
     * This method tells the table display to change the border color of the
     * opponents. If the opponent is the current player, his or her border color
     * becomes blue. In the future the border color of waiting players may
     * change.
     *
     * @param currentPlayerID The ID of the current player.
     * @param connectedPlayersID The IDs of the connected players.
     */
    private void handleBorderColor(String currentPlayerID, String[] connectedPlayersID) {
        for (JPanel oneClient : this.display.getClientPanels()) {
            if (currentPlayerID.equals(oneClient.getName())) {
                oneClient.setBorder(BorderFactory.createLineBorder(Color.CYAN, 8));
            } else {
                boolean connected = false;
                for (String connectedPlayersID1 : connectedPlayersID) {
                    if (connectedPlayersID1.equals(oneClient.getName())) {
                        oneClient.setBorder(BorderFactory.createLineBorder(Color.RED, 8));
                        connected = true;
                    }
                }
                if (!connected) {
                    oneClient.setBorder(BorderFactory.createLineBorder(Color.RED, 8));
                }
            }
        }
    }

    private void handleRestrictions() {
        if (this.state.geki) {
            this.controls.getGeki().setEnabled(true);
        } else {
            this.controls.getGeki().setEnabled(false);
        }

        if (this.state.shibars) {
            this.controls.getShibars().setEnabled(true);
        } else {
            this.controls.getShibars().setEnabled(false);
        }

        if (this.state.jack) {
            this.controls.getJack().setEnabled(true);
        } else {
            this.controls.getJack().setEnabled(false);
        }

        if (this.state.kakumei) {
            this.controls.getKakumei().setEnabled(true);
        } else {
            this.controls.getKakumei().setEnabled(false);
        }
    }

    private Card processCard(String cardUpperCase) {
        if (cardUpperCase.isEmpty()) {
            return null;
        }
        Card replacement = null;
        String card = cardUpperCase.toLowerCase();
        int suit = 0;
        int value = 0;

        if (card.endsWith("spades")) {
            suit = Card.SPADES;
        } else if (card.endsWith("hearts")) {
            suit = Card.HEARTS;
        } else if (card.endsWith("clubs") || card.endsWith("clovers")) {
            suit = Card.CLUBS;
        } else if (card.endsWith("diamonds")) {
            suit = Card.DIAMONDS;
        } else if (card.endsWith("joker")) {
            suit = Card.JOKER;
        } else {
            return null;
        }

        if (card.startsWith("ace") || (card.startsWith("1") && card.substring(1, 2).equals(" "))) {
            value = 1;
        } else if (card.startsWith("2") || card.startsWith("two")) {
            value = 2;
        } else if (card.startsWith("3") || card.startsWith("three")) {
            value = 3;
        } else if (card.startsWith("4") || card.startsWith("four")) {
            value = 4;
        } else if (card.startsWith("5") || card.startsWith("five")) {
            value = 5;
        } else if (card.startsWith("6") || card.startsWith("six")) {
            value = 6;
        } else if (card.startsWith("7") || card.startsWith("seven")) {
            value = 7;
        } else if (card.startsWith("8") || card.startsWith("eight")) {
            value = 8;
        } else if (card.startsWith("9") || card.startsWith("nine")) {
            value = 9;
        } else if (card.startsWith("10") || card.startsWith("ten")) {
            value = 10;
        } else if (card.startsWith("11") || card.startsWith("jack")) {
            value = 11;
        } else if (card.startsWith("12") || card.startsWith("queen")) {
            value = 12;
        } else if (card.startsWith("13") || card.startsWith("king")) {
            value = 13;
        } else if (card.startsWith("joker")) {
            value = Card.JOKER;
        } else {
            return null;
        }

        replacement = new Card(value, suit);

        return replacement;
    }

    private List<String> calculateVisitingPlayers() {
        List visitingPlayers = new ArrayList();
        List<String> allPlayers = Arrays.asList(this.client.connectedPlayerIDs);
        String[] players = this.state.connectedPlayersID;
        String[] playersIDString = new String[this.state.connectedPlayersID.length];
        for (int i = 0; i < players.length; i++) {
            playersIDString[i] = String.valueOf(players[i]);
        }
        List connectedPlayers = Arrays.asList(playersIDString);
        if (connectedPlayers.size() == allPlayers.size()) {
            return visitingPlayers;
        }
        for (int i = 0; i < allPlayers.size(); i++) {
            boolean current = false;
            for (int j = 0; j < connectedPlayers.size(); j++) {
                if (allPlayers.get(i).equals(connectedPlayers.get(j))) {
                    current = true;
                    break;
                }
            }
            if (!current) {
                visitingPlayers.add(allPlayers.get(i));
            }
        }

        return visitingPlayers;
    }

    public void setSortBySuit(boolean sortBySuit) {
        this.sortBySuit = sortBySuit;
    }

    public int getClientPoints() {
        return clientPoints;
    }

    private void handleWaitTimer() {
        this.controls.restartTimer(this.state.time);
    }
}
