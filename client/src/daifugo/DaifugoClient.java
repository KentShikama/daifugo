package daifugo;

import common.*;
import daifugo.logic.*;
import daifugo.messages.*;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.*;
import javax.swing.*;

/**
 * This class extends the Client class which sets up a basic connection with the
 * server. Messages received from the hub are mostly relayed to either the
 * TableController or the ChatController to reflect the current state. This
 * class also handles the extra handshake which is used to send username and
 * password information to the server. Additionally, the class is responsible
 * for creating the TableController and ChatController after player
 * authentication. This class will always keep track of the currently connected
 * clients with the userNameMap.
 */
public class DaifugoClient extends Client {

    private boolean connected;
    TableController tableController;
    ChatController chatController;

    DaifugoClient(String hubHostName, int hubPort, String username, String password)
            throws IOException {
        super(hubHostName, hubPort, username, password);
    }

    @Override
    protected void messageReceived(Object message) {
        if ((message instanceof ForwardedMessage)) {
            ForwardedMessage fm = (ForwardedMessage) message;
            String senderName = fm.username;
            this.chatController.addToTranscript(senderName + ":  " + fm.message);
        } else if (message instanceof ClientConnectedMessage) {
            ClientConnectedMessage cm = (ClientConnectedMessage) message;
            Toolkit.getDefaultToolkit().beep();
            this.chatController.addToTranscript('"' + cm.newClientID + "\" has logged in.");
        } else if (message instanceof ClientDisconnectedMessage) {
            ClientDisconnectedMessage dm = (ClientDisconnectedMessage) message;
            this.chatController.addToTranscript('"' + dm.departingClientID + "\" has either disconnected or logged out.");
        } else if (message instanceof DaifugoState) {
            this.tableController.newState((DaifugoState) message);
        } else if (message instanceof PlayerState) {
            this.tableController.updatePlayers((PlayerState) message);
            this.tableController.updateClients();
        } else if (message instanceof Boolean) {
            handleKakumei(message);
        } else if (message instanceof String) {
            handleServerMessage((String) message);
        }
    }

    private void handleKakumei(Object message) throws HeadlessException {
        boolean hold = ((Boolean) message).booleanValue();
        if (hold) {
            int response = JOptionPane.showConfirmDialog(this.tableController.display, "Do you wish to cause a Kakumei?", "Kakumei", 0);
            if (response == 0) {
                send(Boolean.valueOf(true));
            } else {
                send(Boolean.valueOf(false));
            }
        } else {
            int response = JOptionPane.showConfirmDialog(this.tableController.display, "Do you wish to cause a Garciano (Reverse Kakumei)?", "Kakumei Garciano", 0);
            if (response == 0) {
                send(Boolean.valueOf(false));
            } else {
                send(Boolean.valueOf(true));
            }
        }
    }

    private void handleServerMessage(String message) {
        if (message.equals("reset")) {
            this.tableController.resetPlaceandRestrictions();
            return;
        }
        this.chatController.addToTranscript("Esoteric: " + message);
    }

    @Override
    protected void extraHandshake(ObjectInputStream in, ObjectOutputStream out, String username, String password) throws IOException {
        try {
            out.writeObject(username);
            out.writeObject(password);
            String authentic = (String) in.readObject();
            showAccountCreationFeedback(authentic, username);
            showAuthenticationFeedback(authentic, username);
        } catch (IOException | ClassNotFoundException | HeadlessException e) {
            throw new IOException("Error while setting up connection: " + e);
        }
    }

    private void showAccountCreationFeedback(String authentic, String username) throws HeadlessException {
        if (authentic.equals("successAccount")) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), username + ", your account has been succesfully created.");
            new ClientMain("Welcome To Daifugo");
        } else if (authentic.equals("failAccount")) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), username + ", your account could not be created. Make sure that you are using your first name as your username with the first letter capitalized.");
            new ClientMain("Try Again");
        }
    }

    private void showAuthenticationFeedback(String authentic, String username) throws HeadlessException {
        if (authentic.equals("authenticated")) {
            System.out.println("Authenticated");
            generateDisplay();
        } else if (authentic.equals("combinationError")) {
            new ClientMain("Username/Password Error");
        } else if (authentic.equals("doubleLoginError")) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), username + " is already logged in.");
        } else if (authentic.equals("pointsError")) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), username + ", your rank is not high enough to enter this room.");
            new ClientMain("Try Another Room");
        } else if (authentic.equals("closedError")) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Room is currently closed.");
            new ClientMain("Try Another Room");
        }
    }

    private void newNameMap() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DaifugoClient.this.tableController.updateClients();
            }
        });
    }

    private void generateDisplay() {
        setAutoreset(true);
        this.tableController = new TableController(this);
        this.chatController = new ChatController(this);
    }

    @Override
    protected void connectionClosedByError(String message) {
        setConnected(false);
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

}
