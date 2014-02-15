package daifugo;

/**
 * This class updates the chat box based on the updates sent by the client. This
 * class also sends messages that the user input to the client.
 *
 */
public class ChatController {

    DaifugoClient client;
    ChatDisplay display = new ChatDisplay(this);

    ChatController(DaifugoClient client) {
        this.client = client;
    }

    /**
     * This method is responsible for appending messages to the chat box.
     *
     * @param message The message to be added to the chat.
     * @author This class has been borrowed from the following book:
     * Introduction to Programming Using Java, Sixth Edition.
     */
    void addToTranscript(String message) {
        this.display.getChat().append(message);
        this.display.getChat().append("\n\n");
        this.display.getChat().setCaretPosition(this.display.getChat().getDocument().getLength());
    }

    /**
     * This method is responsible for closing the down the connection and
     * disposing the window
     */
    void doQuit() {
        if (this.client.isConnected()) {
            this.client.disconnect();
        }
        this.display.dispose();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    /**
     * This method is responsible for sending the message input into the text
     * area on the bottom of the chat box. The message is cleared from the text
     * area once it is sent.
     */
    void send() {
        String message = this.display.getInput().getText();

        if (message.trim().length() == 0) {
            return;
        }

        /**
         * Allow user to sort based on his or her preferences. The cards update
         * when a new state is sent. This method makes sure the client has
         * enough points to have this feature activated.
         */
        if (message.equals("sort by suit")) {
            if (this.client.tableController.getClientPoints() > 300) {
                this.client.tableController.setSortBySuit(true);
                addToTranscript("Esoteric: You have set the cards to be sorted according to their suits. The cards will be updated after this turn finishes.");
            } else {
                addToTranscript("Esoteric: You must unlock this feature.");
            }
            this.display.getInput().setText("");
            return;
        } else if (message.equals("sort by value")) {
            this.client.tableController.setSortBySuit(false);
            addToTranscript("Esoteric: You have set the cards to be sorted according to their values. The cards will be updated after this turn finishes.");
            this.display.getInput().setText("");
            return;
        }

        this.client.send(message);
        this.display.getInput().setText("");
    }
}
