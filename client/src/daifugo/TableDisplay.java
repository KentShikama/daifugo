package daifugo;

import daifugo.logic.DaifugoState;
import daifugo.parts.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The table display is where all the main action will take place.
 * On the top of the display is where you can see all your opponents.
 * Around the middle of the display is where the actual "table" would be.
 * On the bottom of the display is where all the cards in your hand will be displayed.
 * If you click on a card in your hand when it is your turn, the card will flip over, 
 * signifying that it has been selected.
 * Once you have selected all the cards you want to play, simply press "place" to confirm your action.
 * The current player will be surrounded by a blue border 
 * while the other players will be surrounded by a red border.
 */
public class TableDisplay extends JFrame {

    private final TableController controller;
    private ArrayList<JPanel> clientPanels = new ArrayList();
    JPanel content;
    JPanel topPanel;
    TableContent graphicsTable;
    private Image cardImages;
    private final int CLIENT_WIDTH = 240;
    private final Color CARPET = new Color(0, 135, 0);
    private final Font CLIENT_FONT = new Font("Verdana", 0, 18);
    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    TableDisplay(TableController controller) {
        this.controller = controller;
        try {
            URL resource = TableDisplay.class.getResource("/cards.png");
            this.cardImages = ImageIO.read(resource);
        } catch (IOException e) {
            System.out.println("Error reading image: " + e.getMessage());
        }

        /* Support for smaller screen heights */
        if (screenSize.height < 800) {
            this.topPanel = new JPanel();
            this.topPanel.setBackground(this.CARPET);
            this.topPanel.setSize(new Dimension(1016, 190));
            this.topPanel.setPreferredSize(new Dimension(1016, 190));
        } else {
            this.topPanel = new JPanel();
            this.topPanel.setBackground(this.CARPET);
            this.topPanel.setSize(new Dimension(1016, 340));
            this.topPanel.setPreferredSize(new Dimension(1016, 340));
        }

        this.graphicsTable = new TableContent();
        this.graphicsTable.setLayout(null);
        this.graphicsTable.setBackground(this.CARPET);
        this.graphicsTable.setSize(new Dimension(1016, 425));
        this.graphicsTable.setPreferredSize(new Dimension(1016, 425));

        this.content = new JPanel();
        this.content.setLayout(new BoxLayout(this.content, 1));
        this.content.setBackground(this.CARPET);

        this.content.add(this.topPanel);
        this.content.add(this.graphicsTable);

        setContentPane(this.content);
        setVisible(true);
        if (screenSize.height < 800) {
            if (screenSize.height < 615) {
                setLocation(0, 0);
            } else {
                setLocation(0, (screenSize.height - 625) / 2);
            }
            setSize(new Dimension(1016, 615));
        } else {
            setSize(new Dimension(1016, 765));
            setLocation(0, (screenSize.height - 775) / 2);
        }

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                windowQuit();
            }
        });
        this.graphicsTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evt) {
                graphicsTable.doClick(evt.getX(), evt.getY());
            }
        });
    }

    /**
     * This method is responsible for drawing a client with the information given in the parameters.
     * @param playerID  The players ID.
     * @param username  The players username.
     * @param points  The number of points the player has.
     */
    void drawClient(String playerID, String username, int points) {
        JPanel client = new JPanel();

        client.setBackground(this.CARPET);
        client.setSize(CLIENT_WIDTH, 150);
        client.setPreferredSize(new Dimension(CLIENT_WIDTH, 150));
        client.setBorder(BorderFactory.createLineBorder(Color.RED, 8));
        client.setLayout(new GridLayout(3, 1));
        client.setName(playerID);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Verdana", 0, 28));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(0);

        String rank = this.controller.rank(points);
        JLabel pointLabel = new JLabel("Rank: " + rank + " (" + String.valueOf(points) + ")");
        pointLabel.setFont(this.CLIENT_FONT);
        pointLabel.setForeground(Color.WHITE);
        pointLabel.setHorizontalAlignment(0);

        JLabel remainingCards = new JLabel();
        remainingCards.setFont(this.CLIENT_FONT);
        remainingCards.setForeground(Color.WHITE);
        remainingCards.setHorizontalAlignment(0);

        remainingCards.setName("remainingCards");
        client.add(nameLabel);
        client.add(pointLabel);
        client.add(remainingCards);
        this.clientPanels.add(client);
    }

    private void windowQuit() {
        this.controller.doQuit();
    }

    public void redrawClients() {
        this.topPanel.removeAll();
        for (JPanel client : this.clientPanels) {
            this.topPanel.add(client);
        }
        this.topPanel.repaint();
        validate();
    }

    public ArrayList<JPanel> getClientPanels() {
        return this.clientPanels;
    }

    class TableContent extends JPanel {

        /**
         * Handles the player's mouse clicks. If the player clicks on a card, it will flip over.
         * @param x  The x coordinate of the user's click.
         * @param y  The y coordinate of the user's click.
         * @author This class has been adapted from the following book: Introduction to Programming Using Java, Sixth Edition.
         */
        private void doClick(int x, int y) {
            if ((controller.getState() == null) || (controller.getState().status != 2)) {
                return;
            }
            for (int i = 0; i < controller.getState().phand.getCardCount(); i++) {
                if (x > 1012) {
                    return;
                }
                if ((y > 154) && (y < 278) && (x > 4 + i * 84) && (x < 88 + i * 84)) {
                    controller.discard[i] = !controller.discard[i];
                    repaint();
                    break;
                }
                if ((y > 278) && (y < 362) && (x > 4 + i * 84) && (x < 88 + i * 84)) {
                    controller.discard[(i + 12)] = !controller.discard[(i + 12)];
                    repaint();
                    break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            draw(g);
        }

        /**
         * This method draws the entire table based on the current state.
         * @param g  The Graphics object to draw with.
         * @author This class has been adapted from the following book: Introduction to Programming Using Java, Sixth Edition.
         */
        void draw(Graphics g) {
            if (controller.getState() == null) {
                return;
            }
            if ((controller.getState().status == DaifugoState.PLAY) || (controller.getState().status == DaifugoState.WAIT_FOR_PLAY)) {
                for (int i = 0; i < controller.getState().phand.getCardCount(); i++) {
                    if (controller.discard != null) {
                        if (controller.discard[i]) {
                            if (i < 12) {
                                drawCard(g, null, 4 + i * 84, 154);
                            } else {
                                drawCard(g, null, 4 + (i - 12) * 84, 278);
                            }
                        } else if (i < 12) {
                            drawCard(g, controller.getState().phand.getCard(i), 4 + i * 84, 154);
                        } else {
                            drawCard(g, controller.getState().phand.getCard(i), 4 + (i - 12) * 84, 278);
                        }

                    } else if (i < 12) {
                        drawCard(g, controller.getState().phand.getCard(i), 4 + i * 84, 154);
                    } else {
                        drawCard(g, controller.getState().phand.getCard(i), 4 + (i - 12) * 84, 278);
                    }

                }

                drawTable(g);
            } else if (controller.getState().status == DaifugoState.VISIT) {
                g.setFont(new Font("Verdana", 0, 48));
                g.setColor(Color.WHITE);
                g.drawString("Please wait for the next round to join.", 50, 280);
                drawTable(g);
            } else if (controller.getState().status == DaifugoState.DEAL) {
            } else {
                System.out.println("Unreachable code.");
            }
        }
    }

    private void drawTable(Graphics g) {
        if (controller.getState().thand.getCardCount() > 0) {
            int draw = controller.getState().thand.getCardCount() - controller.getState().table;
            int offset = 508 - controller.getState().table * 37;
            for (int i = draw; i < controller.getState().thand.getCardCount(); i++) {
                drawCard(g, controller.getState().thand.getCard(i), offset + (i - draw) * 84, 5);
            }
        }
    }

    /**
     * This method draws a single playing card.
     * @param g  Graphics object to draw with.
     * @param card  The card to draw.
     * @param x  The x coordinate of where to draw the card.
     * @param y  The y coordinate of where to draw the card.
     * @author This class has been adapted from the following book: Introduction to Programming Using Java, Sixth Edition.
     */
    public void drawCard(Graphics g, Card card, int x, int y) {
        int cx; // x-coord of upper left corner of the card inside cardsImage
        int cy; // y-coord of upper left corner of the card inside cardsImage
        if (card == null) {
            cy = 4 * 123; // coords for a face-down card.
            cx = 2 * 79;
        } else if (card.getSuit() == Card.JOKER) {
            cy = 4 * 123;
            cx = 0;
        } else {
            if (card.getValue() == 14) {
                cx = 0;
            } else if (card.getValue() == 15) {
                cx = 79;
            } else {
                cx = (card.getValue() - 1) * 79;
            }
            switch (card.getSuit()) {
                case Card.CLUBS:
                    cy = 0;
                    break;
                case Card.DIAMONDS:
                    cy = 123;
                    break;
                case Card.HEARTS:
                    cy = 2 * 123;
                    break;
                default: // spades
                    cy = 3 * 123;
                    break;
            }
        }
        g.drawImage(cardImages, x, y, x + 75, y + 120, cx, cy, cx + 79,
                cy + 123, this);
    }
}
