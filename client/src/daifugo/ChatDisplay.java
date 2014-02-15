package daifugo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This class is responsible for drawing the chat box. The chat interface is
 * very basic. It will be used to display messages sent from other players and
 * “esoteric,” the name of the system/AI. Messages can be sent by pressing the
 * “send” button or by hitting enter inside the text input area. It is even
 * possible to edit the chat interface in anyway you like; however, changes made
 * to your chat interface will not update on other players’ chat boxes.
 *
 */
public class ChatDisplay extends JFrame {

    private JTextArea chat;
    private JScrollPane chatWrap;
    private JPanel bottomPanel;
    private JTextField input;
    private JButton send;
    private JPanel content;
    private final ChatController controller;

    private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    ChatDisplay(ChatController controller) {
        this.controller = controller;

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
        setLocation(1030, (screenSize.height - 700) / 2);

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
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
    }

    /* Getter and Setter Methods */
    public JTextArea getChat() {
        return this.chat;
    }

    public void setChat(JTextArea chat) {
        this.chat = chat;
    }

    public JTextField getInput() {
        return this.input;
    }

    public void setInput(JTextField input) {
        this.input = input;
    }

    /* Button handling methods */
    private void send() {
        this.controller.send();
    }

    private void quit() {
        this.controller.doQuit();
    }
}
