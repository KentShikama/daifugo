package daifugo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This class is responsible for drawing the controls panel. There are four
 * buttons on the control panel. The “start” button is used to start a new game.
 * The “quit” button is used to quit the application. The “place” button is used
 * to place cards onto the table. The “pass” button is used to pass your own
 * turn. If you play a special card such as a seven or a ten, the “place” button
 * will change to a “give” or “throw” button to allow you to do the necessary
 * follow up moves. The four labels below the buttons are there to inform the
 * player which restrictions are currently active. If the label becomes enabled
 * and darkened, this means the restriction is on. Finally on the very bottom of
 * the controls panels is some information about the account you logged in with.
 *
 */
public class ControlsDisplay extends JFrame {

    /* Controls */
    private JPanel row1;
    private JButton start;
    private JButton quit;
    private JPanel row2;
    private JButton place;
    private JButton pass;
    private JPanel row3;
    private JLabel remainingLabel;
    private JLabel remaining;
    private int remainingTime = 60;
    private Timer timer;
    private JPanel row4;
    private JLabel shibars;
    private JLabel geki;
    private JLabel jack;
    private JLabel kakumei;
    private JPanel controlsPanel;
    /* Player Info Panel */
    private JPanel row5;
    private JLabel title;
    private JPanel row6;
    private JLabel points;
    private JPanel playerPanel;
    private JPanel content;
    private final TableController controller;

    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    ControlsDisplay(TableController controller) {
        this.controller = controller;

        this.start = new JButton("Start");
        this.quit = new JButton("Quit");
        this.row1 = new JPanel();
        this.row1.add(this.start);
        this.row1.add(this.quit);

        this.place = new JButton("Place");
        this.pass = new JButton("Pass");
        this.row2 = new JPanel();
        this.row2.add(this.place);
        this.row2.add(this.pass);

        this.remainingLabel = new JLabel("Remaining Time:");
        this.remaining = new JLabel("Calculating...");
        this.remainingLabel.setFont(new Font("Times New Roman", 0, 16));
        this.remaining.setFont(new Font("Times New Roman", 0, 16));
        this.remaining.setEnabled(false);
        this.remainingLabel.setEnabled(false);
        this.row3 = new JPanel();
        this.row3.add(this.remainingLabel);
        this.row3.add(this.remaining);

        this.timer = new Timer(1000, new TimerAction());

        this.geki = new JLabel(" Geki ");
        this.shibars = new JLabel(" Shibars ");
        this.jack = new JLabel(" Jack-Back ");
        this.kakumei = new JLabel(" Kakumei ");
        this.geki.setFont(new Font("Times New Roman", 0, 16));
        this.shibars.setFont(new Font("Times New Roman", 0, 16));
        this.jack.setFont(new Font("Times New Roman", 0, 16));
        this.kakumei.setFont(new Font("Times New Roman", 0, 16));

        this.geki.setEnabled(false);
        this.shibars.setEnabled(false);
        this.jack.setEnabled(false);
        this.kakumei.setEnabled(false);
        this.row4 = new JPanel();
        this.row4.add(this.geki);
        this.row4.add(this.shibars);
        this.row4.add(this.jack);
        this.row4.add(this.kakumei);

        this.controlsPanel = new JPanel();
        this.controlsPanel.setLayout(new BoxLayout(this.controlsPanel, 1));
        this.controlsPanel.add(this.row1);
        this.controlsPanel.add(this.row2);
        this.controlsPanel.add(this.row3);
        this.controlsPanel.add(this.row4);

        this.title = new JLabel("Your Stats");
        this.title.setFont(new Font("Times New Roman", 0, 18));
        this.row5 = new JPanel();
        this.row5.add(this.title);

        this.points = new JLabel("Receiving...");
        this.points.setFont(new Font("Times New Roman", 0, 16));
        this.points.setName("points");
        this.row6 = new JPanel();
        this.row6.add(this.points);

        this.playerPanel = new JPanel();
        this.playerPanel.setLayout(new BoxLayout(this.playerPanel, 1));
        this.playerPanel.setBorder(BorderFactory.createMatteBorder(5, 0, 0, 0, Color.LIGHT_GRAY));
        this.playerPanel.add(this.row5);
        this.playerPanel.add(this.row6);

        this.content = new JPanel();
        this.content.setLayout(new BoxLayout(this.content, 1));
        this.content.add(this.controlsPanel);
        this.content.add(this.playerPanel);

        setContentPane(this.content);
        setVisible(true);
        setSize(300, 240);
        setResizable(false);
        setLocation(1090, ((screenSize.height - 700) / 2) + 450);

        this.start.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                begin();
                start.setEnabled(false);
            }
        });
        this.place.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                place();
            }
        });
        this.pass.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pass();
            }
        });
        this.quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int response
                        = JOptionPane.showConfirmDialog(ControlsDisplay.this, "Do you really wish to quit? \n"
                                + "If you quit during a game you will lose 10 points.", "Warning", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    quit();
                } else {
                    // Do nothing
                }
            }
        });

        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        quit();
                    }
                });
    }

    /**
     * This method restarts the timer.
     *
     * @param length The number of seconds left to start the timer with.
     */
    public void restartTimer(int length) {
        this.remainingTime = length;
        this.timer.start();
    }

    public void stopTimer() {
        this.timer.stop();
    }

    /* Button Handling Methods */
    private void quit() {
        this.timer.stop();
        this.controller.doQuit();
    }

    private void pass() {
        this.timer.stop();
        this.controller.pass();
    }

    private void begin() {
        this.controller.begin();
    }

    private void place() {
        this.timer.stop();
        this.controller.place();
    }

    /* Getter and Setter Methods */
    public JButton getPlace() {
        return this.place;
    }

    public JButton getStart() {
        return this.start;
    }

    public JButton getPass() {
        return this.pass;
    }

    public JLabel getRemaining() {
        return this.remaining;
    }

    public JLabel getPoints() {
        return this.points;
    }

    public JLabel getShibars() {
        return this.shibars;
    }

    public JLabel getGeki() {
        return this.geki;
    }

    public JLabel getJack() {
        return this.jack;
    }

    public JLabel getKakumei() {
        return this.kakumei;
    }

    public JLabel getRemainingLabel() {
        return this.remainingLabel;
    }

    public int getRemainingTime() {
        return this.remainingTime;
    }

    /**
     * This class handles the timer that is used to display the number of
     * seconds the current player has to finish his or her turn. If the
     * remaining time becomes 0, the players turn is automatically passed.
     */
    private class TimerAction implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            remainingTime--;
            if (getRemainingTime() <= 0) {
                remainingTime = 0;
            }
            getRemaining().setText(String.valueOf(getRemainingTime()));
        }
    }
}
