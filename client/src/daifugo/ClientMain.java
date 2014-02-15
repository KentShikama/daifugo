package daifugo;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * This class is responsible for displaying the login window. Once the player
 * chooses a room and enters his or her username and password, this class tells
 * the DaifugoClient to create a new connection using the provided information.
 * This class is the entry point for clients.
 */
public class ClientMain {

    /**
     * Modify this IP address in order to connect to proper host. If you plan to
     * test the application from you own computer, use localhost.
     */
    private static final String HOST = "localhost"; //162.243.121.40

    private DaifugoClient client;
    private int port;
    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final int WIDTH = 500;
    private final int HEIGHT = 300;
    private JFrame frame;
    private JPanel content;
    private JPanel topPanel;
    private JLabel titleMessage;
    final String[] roomChoices = {"Test", "Blitz", "Tournament"};
    private JPanel roomsRow;
    private JComboBox rooms;
    private JLabel roomsLabel;
    private JPanel usernameRow;
    private JTextField username;
    private JLabel usernameLabel;
    private JPanel passwordRow;
    private JPasswordField password;
    private JLabel passwordLabel;
    private JPanel bottomPanel;
    private JButton loginButton;
    private JButton cancelButton;
    private JButton createNew;

    ClientMain() {
        topPanel();

        this.loginButton = new JButton("Login");
        this.cancelButton = new JButton("Cancel");
        this.createNew = new JButton("Create Account");

        this.bottomPanel = new JPanel();
        this.bottomPanel.add(this.loginButton);
        this.bottomPanel.add(this.cancelButton);
        this.bottomPanel.add(this.createNew);

        this.loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    connect();
                    frame.setVisible(false);
                } catch (Exception ex) {
                    Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        this.cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });
        this.createNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    createAccount();
                    frame.setVisible(false);
                } catch (Exception e) {
                    Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        });

        this.content = new JPanel();
        this.content.add(this.topPanel);
        this.content.add(this.bottomPanel);

        this.frame = new JFrame("Daifugo");
        this.frame.setContentPane(this.content);
        this.frame.setLocation((this.screenSize.width - 500) / 2, (this.screenSize.height - 300) / 2);

        this.frame.setDefaultCloseOperation(3);
        this.frame.setVisible(true);
        this.frame.setResizable(false);
        this.frame.setSize(500, 300);
    }

    ClientMain(String title) {
        this();
        this.titleMessage.setText(title);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        new ClientMain();
    }

    private String readUsername() {
        return this.username.getText();
    }

    private String readPassword() {
        return String.valueOf(this.password.getPassword());
    }

    private void connect() throws Exception {
        this.port = readPort();
        if (this.port == -1) {
            throw new Exception("Unsupported Room");
        }
        try {
            this.client = new DaifugoClient(HOST, this.port, readUsername(), readPassword());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Application cannot connect to the room. The room is most likely full. If you cannot connect to any room, there might be a problem with your network settings.");
            System.exit(1);
        }
    }

    private void createAccount() throws Exception {
        this.port = 23551;
        try {
            this.client = new DaifugoClient(HOST, this.port, readUsername(), readPassword());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Application failed to create new account.");
            System.exit(1);
        }
    }

    private int readPort() {
        String room = (String) this.rooms.getSelectedItem();
        if (room.equalsIgnoreCase("Test")) {
            return 23548;
        }
        if (room.equalsIgnoreCase("Blitz")) {
            return 23549;
        }
        if (room.equalsIgnoreCase("Tournament")) {
            return 23550;
        }
        return -1;
    }

    private void topPanel() {
        this.titleMessage = new JLabel("Welcome to Daifugo!!!", 0);
        this.titleMessage.setFont(new Font("Baskerville", 0, 30));
        this.titleMessage.setPreferredSize(new Dimension(300, 80));
        this.rooms = new JComboBox(this.roomChoices);
        this.roomsLabel = new JLabel("Room:");
        this.username = new JTextField();
        this.username.setColumns(30);
        this.usernameLabel = new JLabel("Username:");
        this.password = new JPasswordField();
        this.password.setColumns(30);
        this.passwordLabel = new JLabel("Password:");

        this.roomsRow = new JPanel();
        this.roomsRow.add(this.roomsLabel);
        this.roomsRow.add(this.rooms);
        this.usernameRow = new JPanel();
        this.usernameRow.add(this.usernameLabel);
        this.usernameRow.add(this.username);
        this.passwordRow = new JPanel();
        this.passwordRow.add(this.passwordLabel);
        this.passwordRow.add(this.password);

        this.topPanel = new JPanel();
        this.topPanel.setLayout(new BoxLayout(this.topPanel, 1));
        this.topPanel.add(this.titleMessage);
        this.topPanel.add(this.roomsRow);
        this.topPanel.add(this.usernameRow);
        this.topPanel.add(this.passwordRow);
    }
}
