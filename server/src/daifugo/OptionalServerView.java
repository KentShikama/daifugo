package daifugo;

import daifugo.parts.Hand;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class OptionalServerView extends JFrame {

    private DaifugoHub hub;
    private JTextArea chat;
    private JScrollPane chatWrap;
    private JPanel bottomPanel;
    private JTextField input;
    private JButton send;
    private JPanel content;
    final String[] roomChoices = {"Room 1", "Room 2", "Room 3", "Room 4", "Room 5", "Room 6"};

    public OptionalServerView(DaifugoHub hub, HubMain main, int number) {
        this.hub = hub;

        this.chat = new JTextArea(20, 30);
        this.chat.setMargin(new Insets(5, 5, 5, 5));
        this.chat.setLineWrap(true);
        this.chatWrap = new JScrollPane(this.chat);
        this.chatWrap.setHorizontalScrollBarPolicy(30);

        this.input = new JTextField(20);
        this.send = new JButton("Send");
        this.bottomPanel = new JPanel();
        this.bottomPanel.add(this.input);
        this.bottomPanel.add(this.send);

        this.content = new JPanel();
        this.content.setLayout(new BorderLayout());
        this.content.add(this.chatWrap, "Center");
        this.content.add(this.bottomPanel, "South");

        setContentPane(this.content);
        setVisible(true);
        setSize(400, 400);

        if (number == 0) {
            this.setLocation(0, 0);
            this.setTitle(roomChoices[0]);
        } else if (number == 1) {
            this.setLocation(520, 0);
            this.setTitle(roomChoices[1]);
        } else if (number == 2) {
            this.setLocation(1040, 0);
            this.setTitle(roomChoices[2]);
        } else if (number == 3) {
            this.setLocation(0, 430);
            this.setTitle(roomChoices[3]);
        } else if (number == 4) {
            this.setLocation(520, 430);
            this.setTitle(roomChoices[4]);
        } else if (number == 5) {
            this.setLocation(1040, 430);
            this.setTitle(roomChoices[5]);
        } else {
            this.setLocation(0, 0);
        }

        this.input.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                send();
            }
        });
        this.send.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

    }

    /**
     * This method is responsible for appending messages to the chat box.
     *
     * @param message  The message to be added to the chat.
     * @author This class has been borrowed from the following book: Introduction to Programming Using Java, Sixth Edition.
     */
    public void addToTranscript(String message) {
        chat.append(message);
        chat.append("\n\n");
        chat.setCaretPosition(chat.getDocument().getLength());
    }

    /**
     * The message is cleared from the text area once it is sent.
     */
    void send() {
        String message = input.getText();

        if (message.trim().length() == 0) {
            return;
        }

        boolean changes = changes(message);

        if (!changes) {
            this.hub.sendToAll(message);
        }

        input.setText("");
    }

    private boolean changes(String message) {
        /**
         * The time change is safe to call anytime. It will update the time each player has to make his or her turn.
         */
        if (message.startsWith("time")) {
            String time = "";
            if (message.length() == 8) {
                time = message.substring(5, 8);
            } else if (message.length() == 7) {
                time = message.substring(5, 7);
            } else {
                this.addToTranscript("Error in reading time: " + message + "\n"
                        + "Syntax: time (number)");
                return true;
            }
            hub.getValidator().setTime(Integer.valueOf(time));
            this.addToTranscript("Time per turn has been changed to " + time + " seconds.");
            return true;
        } else if (message.startsWith("winPoints")) {
            String points = "";
            if (message.length() == 13) {
                points = message.substring(10, 13);
            } else if (message.length() == 12) {
                points = message.substring(10, 12);
            } else {
                this.addToTranscript("Error in reading points: " + message + "\n"
                        + "Syntax: winPoints (number)");
                return true;
            }
            hub.getValidator().setWinPoints(Integer.valueOf(points));
            this.addToTranscript("The number of points on win has been changed to " + points + " points.");
            return true;
        } else if (message.startsWith("current player")) {
            this.addToTranscript("Current player is " + (hub.getCurrentPlayerName()) + ".");
            return true;
        } else if (message.startsWith("connected players")) {
            this.addToTranscript("Number of connected players: " + hub.getNumberOfConnectedPlayers() + ".");
            return true;
        } else if (message.startsWith("reveal")) {
            if (hub.getValidator().getPlayerHands().isEmpty()) {
                this.addToTranscript("Game is currently not in progress.");
                return true;
            }
            ArrayList<Hand> playerHands = hub.getValidator().getPlayerHands();
            String[] connectedPlayers = hub.getValidator().getConnectedPlayersID();

            for (int i = 0; i < connectedPlayers.length; i++) {
                this.addToTranscript(connectedPlayers[i] + " has the following: " + playerHands.get(i));
            }
            return true;
        }
        return false;
    }
}
