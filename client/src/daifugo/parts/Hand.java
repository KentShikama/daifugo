package daifugo.parts;

/**
 * An object of type Hand represents a hand of cards.  The
 * cards belong to the class Card.  A hand is empty when it
 * is created, and any number of cards can be added to it.
 */
import java.util.Vector;
import java.io.Serializable;

public class Hand implements Serializable {

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("rawtypes")
    private Vector hand;   // The cards in the hand.

    /**
     * Create a hand that is initially empty.
     */
    @SuppressWarnings("rawtypes")
    public Hand() {
        hand = new Vector();
    }

    /**
     * Remove all cards from the hand, leaving it empty.
     */
    public void clear() {
        hand.setSize(0);
    }

    /**
     * Add a card to the hand.  It is added at the end of the current hand.
     * @param c the non-null card to be added.
     * @throws NullPointerException if the parameter c is null.
     */
    @SuppressWarnings("unchecked")
    public void addCard(Card c) {
        if (c == null) {
            throw new NullPointerException("Can't add a null card to a hand.");
        }
        hand.addElement(c);
    }

    /**
     * Remove a card from the hand, if present.
     * @param c the card to be removed.  If c is null or if the card is not in
     * the hand, then nothing is done.
     */
    public void removeCard(Card c) {
        for (int i = 0; i < hand.size(); i++) {
            Card d = (Card) hand.elementAt(i);
            if (c.getValue() == d.getValue() && c.getSuit() == d.getSuit()) {
                hand.removeElementAt(i);
            }
        }
    }

    /**
     * Remove the card in a specified position from the hand.
     * @param position the position of the card that is to be removed, where
     * positions are starting from zero.
     * @throws IllegalArgumentException if the position does not exist in
     * the hand, that is if the position is less than 0 or greater than
     * or equal to the number of cards in the hand.
     */
    public void removeCard(int position) {
        if (position < 0 || position >= hand.size()) {
            throw new IllegalArgumentException("Position does not exist in hand: "
                    + position);
        }
        hand.removeElementAt(position);
    }

    /**
     * Returns the number of cards in the hand.
     */
    public int getCardCount() {
        return hand.size();
    }

    /**
     * Gets the card in a specified position in the hand.  (Note that this card
     * is not removed from the hand!)
     * @param position the position of the card that is to be returned
     * @throws IllegalArgumentException if position does not exist in the hand
     */
    public Card getCard(int position) {
        if (position < 0 || position >= hand.size()) {
            throw new IllegalArgumentException("Position does not exist in hand: "
                    + position);
        }
        return (Card) hand.elementAt(position);
    }

    /**
     * Sorts the cards in the hand so that cards of the same suit are
     * grouped together, and within a suit the cards are sorted by value.
     * Note that aces are considered to have the lowest value, 1.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void sortBySuit() {
        Vector newHand = new Vector();
        while (hand.size() > 0) {
            int pos = 0;  // Position of minimal card.
            Card c = (Card) hand.elementAt(0);  // Minumal card.
            for (int i = 1; i < hand.size(); i++) {
                Card c1 = (Card) hand.elementAt(i);
                if (c1.getSuit() < c.getSuit()
                        || (c1.getSuit() == c.getSuit() && c1.getValue() < c.getValue())) {
                    pos = i;
                    c = c1;
                }
            }
            hand.removeElementAt(pos);
            newHand.addElement(c);
        }
        hand = newHand;
    }

    /**
     * Sorts the cards in the hand so that cards are sorted into order
     * of increasing value.  Cards with the same value are sorted by suit.
     * Note that aces are considered to have the lowest value, 1.
     */
    @SuppressWarnings("unchecked")
    public void sortByValue() {
        @SuppressWarnings("rawtypes")
        Vector newHand = new Vector();
        while (hand.size() > 0) {
            int pos = 0;  // Position of minimal card.
            Card c = (Card) hand.elementAt(0);  // Minumal card.
            for (int i = 1; i < hand.size(); i++) {
                Card c1 = (Card) hand.elementAt(i);
                if (c1.getValue() < c.getValue()
                        || (c1.getValue() == c.getValue() && c1.getSuit() < c.getSuit())) {
                    pos = i;
                    c = c1;
                }
            }
            hand.removeElementAt(pos);
            newHand.addElement(c);
        }
        hand = newHand;
    }

    /**
     * Computes and returns the value of a hand.
     */
    public int getTotalValue() {

        int val;      // The value computed for the hand.
        int cards;    // Number of cards in the hand.

        val = 0;
        cards = getCardCount();

        for (int i = 0; i < cards; i++) {
            // Add the value of the i-th card in the hand.
            Card card;    // The i-th card;
            int cardVal;  // The value of the i-th card.
            card = getCard(i);
            cardVal = card.getValue();  // The normal value, 1 to 13.
            val = val + cardVal;
        }

        return val;

    }  // end getTotalValue()

    @Override
    public String toString() {
        String message = "";
        if (hand.size() == 1) {
            message += hand.get(0) + ".";
        }
        for (int i = 0; i < hand.size(); i++) {
            if (i == (hand.size() - 1)) {
                message += " " + hand.get(i) + ".";
            } else if (i == 0) {
                message += hand.get(i) + ",";
            } else {
                message += " " + hand.get(i) + ",";
            }
        }
        return message;
    }
}
