package daifugo.logic;

import daifugo.*;
import daifugo.parts.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Validator {

    DaifugoHub hub;
    private Deck deck = new Deck(true); // Deck with 52 cards + 2 jokers
    private volatile int time = 60; // The number of seconds per turn.
    private volatile int winPoints = 30; // The number of points won per win

    /* Non-Rotation Variables */
    private String[] connectedPlayersID; // These clients are the ones who are actually playing
    private ArrayList<Hand> playerHands = new ArrayList<Hand>();
    private TreeMap<String, Integer> handCount = new TreeMap<String, Integer>();
    private Hand handd = new Hand(); // Hand for table's discard pile
    public boolean kakumei;
    private int readyCount; // Checks how many people have pressed the "deal" button
    private boolean gameInProgress;
    /* Rotation Variables */
    private Hand handt = new Hand(); // Hand for table
    private Hand previous = new Hand(); // Hand played by the opponent last turn
    private int skipped;
    private boolean impossible;
    private int shibarsprep; // To check if the next turn you can shibar it.
    private boolean shibars;
    private boolean jack;
    private boolean kaidan;
    private boolean geki;
    /* Turn variables */
    private boolean reversi;
    private int gotobashi; // To see how many people the gotobashi skip's
    private boolean give; // To see if operation is to give cards
    private boolean away; // To see if operation is to throw away
    private int giveCount;
    private int throwCount;
    private boolean chombo;
    private boolean played; // Did the current player play any cards?
    public boolean invalid;
    private int table; // The amount of cards played on the table, (aka now.getCardCount() when not give/throw)
    private Hand now = new Hand(); // Hand played by current player
    private boolean end; // To see if the turn should be ended for the CP
    public int jokerReceived; // See if joker was used
    Timer timer;
    private int illegalMoveCount;
    private TreeMap<String, Integer> timeOutCount = new TreeMap<>();
    public ArrayList<String> dealAgree = new ArrayList<>();

    boolean dealTimer = false;
    Timer dealTimerObject;

    public Validator(DaifugoHub hub) {
        this.hub = hub;
    }

    private void resumeTimer() {
        this.timer = new Timer();
        this.timer.schedule(new SwitchTask(), time * 1000);
    }

    private void pauseTimer() {
        this.timer.cancel();
    }

    class SwitchTask extends TimerTask {

        @Override
        public void run() {
            if (timeOutCount.get(hub.getCurrentPlayerName()) == null && gameInProgress) {
                timeOutCount.put(hub.getCurrentPlayerName(), 0);
                ending();
            } else if (gameInProgress) {
                hub.playerDisconnected(hub.getCurrentPlayerName());
                hub.sendToAll(hub.getCurrentPlayerName() + " disconnects during a game: -10 points!");
            } else {
                this.cancel();
            }
        }
    }

    private void resumeDealTimer() {
        this.dealTimerObject = new Timer();
        this.dealTimerObject.schedule(new DealTask(), 10000);
    }

    private void pauseDealTimer() {
        if (dealTimerObject != null) {
            this.dealTimerObject.cancel();
        }
    }

    class DealTask extends TimerTask {

        @Override
        public void run() {
            if (!gameInProgress) {
                dealTimer = true;
                deal("null");
            } else {
                System.out.println("Deal timer has been activated during game.");
            }
        }

    }

    public void process(Hand message) throws Exception {

        pauseTimer();

        now = message;
        /* First sort by suit and then by value to ensure no mistake during shibars */
        now.sortBySuit();
        now.sortByValue();

        if (give) {
            boolean random = give(now);
            /* If the give input is invalid */
            if (!random) {
                hub.sendToOne(hub.getCurrentPlayerName(), "Invalid card(s) for giving.");
                hub.sendState();
                resumeTimer();
                return;
            }
            give = false;
            end = true;

            if (away) {
                end = false;
                /* Updates the state, indirectly the graphics. */
                hub.sendState();
                resumeTimer();
                return;
            }

            /* Updates the state, indirectly the graphics. */
            hub.sendState();

        } else if (away) {
            boolean random = away(now);
            /* If the give input is invalid */
            if (!random) {
                hub.sendToOne(hub.getCurrentPlayerName(), "Invalid card(s) for throwing.");
                hub.sendState();
                resumeTimer();
                return;
            }
            away = false;
            end = true;

//            hub.getDisplay().addToTranscript(hub.getCurrentPlayerName() + " throws away the following: " + now.toString());
            /* Updates the state, indirectly the graphics. */
            hub.sendState();
        } else {

//            hub.getDisplay().addToTranscript(hub.getCurrentPlayerName() + " plays the following: " + now.toString());

            /* Situation if you skipped turn (must press end button)*/
            if (now.getCardCount() == 0) {
                played = false;
                skipped++;
                invalid = false;
            } else {
                if (played) {
                    throw new Exception("Shouldn't happen");
                }
                check();
                if (invalid) {
                    /* Make sure reversi is reset to "original" as it could have been changed at method start */
                    if (jack) {
                        reversi = !reversi;
                    }
                    if (kakumei) {
                        reversi = !reversi;
                    }
                    hub.sendState();
                    resumeTimer();
                    if (illegalMoveCount == 0) {
                        // Do nothing
                    } else if (illegalMoveCount > 0) {
                        hub.updatePoints(hub.getCurrentPlayerName(), -2);
                        hub.sendToAll(hub.getCurrentPlayerName() + " attempts more than one illegal move in a single turn: -2 points.");
                    }
                    illegalMoveCount++;
                    return;
                }
                played = true;
            }

            for (int i = 0; i < now.getCardCount(); i++) {
                handt.addCard(now.getCard(i));
                getPlayerHands().get(hub.getCurrentPlayerIndex()).removeCard(now.getCard(i));
                /* If joker has been received then remove it here */
                if (jokerReceived > 0) {
                    getPlayerHands().get(hub.getCurrentPlayerIndex()).removeCard(new Card());
                    /* It should loop more than twice if two cards are played */
                    jokerReceived--;
                }
            }

            table = now.getCardCount(); // Sets amount of cards played to the table count

            if (!kakumei && table >= 4) {
                hub.sendToOne(hub.getCurrentPlayerName(), true);
            } else if (kakumei && table >= 4) {
                hub.sendToOne(hub.getCurrentPlayerName(), false);
            }

            if (!(give || away)) {
                end = true;
            } else {
                now.clear();
                hub.sendState();
                resumeTimer();
            }
        }

        if (end) {
            ending();
        }
    }


    /* Methods to get states things */
    public DaifugoState getCurrentPlayerState() {
        int index = 0;
        String[] players = this.getConnectedPlayersID();
        for (int i = 0; i < players.length; i++) {
            if (hub.getCurrentPlayerName() == null ? players[i] == null : hub.getCurrentPlayerName().equals(players[i])) {
                index = i;
                handCount.put(players[i], getPlayerHands().get(i).getCardCount());
            } else {
                handCount.put(players[i], getPlayerHands().get(i).getCardCount());
            }
        }
        getPlayerHands().get(index).sortBySuit();
        getPlayerHands().get(index).sortByValue();

        return new DaifugoState(hub.getCurrentPlayerName(), connectedPlayersID, getPlayerHands().get(index), handt, table, handd, DaifugoState.PLAY,
                handCount, time, shibars, kakumei, jack, geki, impossible,
                give, away, played);
    }

    public DaifugoState getWaitingPlayerState(String playerID) {
        int index = 0;
        String[] players = this.getConnectedPlayersID();
        for (int i = 0; i < players.length; i++) {
            if (playerID == null ? players[i] == null : playerID.equals(players[i])) {
                index = i;
            } else {
                handCount.put(players[i], getPlayerHands().get(i).getCardCount());
            }
        }
        getPlayerHands().get(index).sortBySuit();
        getPlayerHands().get(index).sortByValue();

        return new DaifugoState(hub.getCurrentPlayerName(), connectedPlayersID, getPlayerHands().get(index), handt, table, handd, DaifugoState.WAIT_FOR_PLAY,
                handCount, time, shibars, kakumei, jack, geki, impossible,
                give, away, played);
    }

    public DaifugoState getVisitngPlayerState(String playerID) {
        String[] players = this.getConnectedPlayersID();
        for (int i = 0; i < players.length; i++) {
            handCount.put(players[i], getPlayerHands().get(i).getCardCount());
        }

        return new DaifugoState(hub.getCurrentPlayerName(), connectedPlayersID, null, handt, table, handd, DaifugoState.VISIT,
                handCount, time, shibars, kakumei, jack, geki, impossible,
                give, away, played);
    }

    /* Methods that are included in "process()" */
    private void check() {

        /**
         * Note: anything that comes before the checking of "invalid" must be
         * set back to false in the block following this method call
         */

        /* If the jack is still true because its the same pile after a jack has been played then enable a reverse */
        if (jack) {
            reversi = !reversi;
        }

        /* If kakumei is true then reverse the reversei (can stack with jack)  */
        if (kakumei) {
            reversi = !reversi;
        }

        invalid = false; // Assume invalid is false at first

        /* Situation if you played a single card */
        if (now.getCardCount() == 1) {
            /* The previous must be none or ... */
            if (previous.getCardCount() == 0) {
                // any card is valid
		/* Check each card for specifics using double check */
                doubleCheck(now);
                invalid = false;
                shibarsprep = 1;
            } /* The previous must be one */ else if (previous.getCardCount() == 1) {
                /* If card is a joker it will be valid no matter what */
                if (now.getCard(0).getSuit() == Card.JOKER) {
                    doubleCheck(now);
                    invalid = false;
                    return;
                }

                if (previous.getCard(0).getValue() == Card.JOKER
                        && (now.getCard(0).getValue() == 3 && now.getCard(0).getSuit() == Card.SPADES)) {
                    // Valid + Situation if joker was put down before and
                    // now a spade of 3 is put
                    impossible = true;
                    invalid = false;
                    return;
                }

                // Card that is greater/lesser than prev or joker
                if ((!reversi && (now.getCard(0).getValue() <= previous.getCard(0).getValue()))
                        || (reversi && (now.getCard(0).getValue() >= previous.getCard(0).getValue()))) {
                    // Invalid card must be lower/higher value
                    hub.sendToOne(
                            hub.getCurrentPlayerName(),
                            "The card "
                            + now.getCard(0).toString()
                            + " is invalid because value must be higher/lower.");
                    invalid = true;
                    return;
                } else if (shibars
                        && (now.getCard(0).getSuit() != previous.getCard(0).getSuit())) {
                    // Invalid shibars is on so card must be same as
                    // previous
                    hub.sendToOne(
                            hub.getCurrentPlayerName(),
                            "The card "
                            + now.getCard(0).toString()
                            + " is invalid because it doesn't match the previouis suit.");
                    invalid = true;
                    return;
                } else if (geki && ((!reversi && now.getCard(0).getValue() != previous.getCard(0).getValue() + 1)
                        || (reversi && now.getCard(0).getValue() + 1 != previous.getCard(0).getValue()))) {
                    hub.sendToOne(
                            hub.getCurrentPlayerName(),
                            "The cards are invalid because the pile is currently in geki shiba mode.");
                    invalid = true;
                    return;
                } else {
                    // Valid
                    /* Check each card for specifics using double check */
                    doubleCheck(now);
                    invalid = false;
                }
            } else {
                // Invalid
                hub.sendToOne(
                        hub.getCurrentPlayerName(),
                        "The card "
                        + now.getCard(0).toString()
                        + " is invalid because it is currently not a single card pile.");
                invalid = true;
                return;
            }
        }

        /* Situation if you played two cards */
        if (now.getCardCount() == 2) {
            int length = now.getCardCount();
            /* The previous must be 1 or none */
            if (previous.getCardCount() == 0) {
                // If card is same value it is valid (includes double joker)
                for (int i = 0; i < length - 1; i++) {
                    if (now.getCard(i).getValue() != now.getCard(i + 1).getValue()) {
                        // Invalid
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The card "
                                + now.getCard(0).toString()
                                + " and "
                                + now.getCard(1).toString()
                                + " is invalid because their value isn't the same.");
                        invalid = true;
                        return;
                    }
                }
                if (!invalid) {
                    shibarsprep = 1;
                }
            } else if (previous.getCardCount() == 2) {
                // Card that is great/less than prev or card that is
                // great/less than prev + joker or two jokers
                for (int i = 0; i < length - 1; i++) {
                    if (now.getCard(i).getValue() != now.getCard(i + 1).getValue()) {
                        // Invalid
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The card "
                                + now.getCard(0).toString()
                                + " and "
                                + now.getCard(1).toString()
                                + " is invalid because their value isn't the same.");
                        invalid = true;
                        return;
                    } else if (((!reversi && (now.getCard(i).getValue() <= previous.getCard(i).getValue()))
                            || (reversi && (now.getCard(i).getValue() >= previous.getCard(i).getValue()))) && now.getCard(i).getValue() != Card.JOKER) {
                        // Invalid card must be lower/higher value or be two jokers
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The card "
                                + now.getCard(0).toString()
                                + " and "
                                + now.getCard(1).toString()
                                + " is invalid because value must be higher/lower.");
                        invalid = true;
                        return;
                    } else if (shibars
                            && (now.getCard(i).getSuit() != previous.getCard(i).getSuit())) {
                        // Invalid shibars is on so card must be same as
                        // previous
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The card "
                                + now.getCard(0).toString()
                                + " and "
                                + now.getCard(1).toString()
                                + " is invalid because they have the wrong suit.");
                        invalid = true;

                        return;
                    } else if (geki && ((!reversi && now.getCard(0).getValue() != previous.getCard(0).getValue() + 1)
                            || (reversi && now.getCard(0).getValue() + 1 != previous.getCard(0).getValue()))) {
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The cards are invalid because the pile is currently in geki shiba mode.");
                        invalid = true;
                        return;
                    }
                }
            } else {
                // Invalid
                hub.sendToOne(
                        hub.getCurrentPlayerName(),
                        "The card "
                        + now.getCard(0).toString()
                        + " and "
                        + now.getCard(1).toString()
                        + " is invalid because it is currently not a double card pile.");
                invalid = true;
                return;
            }

            if (!invalid) {
                /* Check each card for specifics using double check */
                doubleCheck(now);
                invalid = false;
            }

        }

        /*
         * Situation if you played 3 or more cards (possibility of a
         * reversi)
         */
        if (now.getCardCount() >= 3) {
            int length = now.getCardCount();
            /**
             * To eliminate the possibility of a (Ace, Ace, Two) or (Two, Two,
             * Two, Three), an extra check will be performed for cardCount > 3
             */
            boolean threeCardValid = false;
            int valueCheck = 0;
            for (int i = 0; i < length - 1; i++) {
                if (now.getCard(i).getValue() == now.getCard(i + 1).getValue()) {
                    valueCheck++;
                }
            }
            if (valueCheck == (length - 1)) {
                threeCardValid = true;
            }
            int suitCheck = 0;
            for (int i = 0; i < length - 1; i++) {
                if ((now.getCard(i).getSuit() == now.getCard(i + 1).getSuit())
                        && (now.getCard(i).getValue() + 1 == now.getCard(i + 1).getValue())) {
                    suitCheck++;
                }
            }
            if (suitCheck == (length - 1)) {
                kaidan = true;
                threeCardValid = true;
            }

            if (!threeCardValid) {
                // Invalid because it wasn't a same value or kaidan
                hub.sendToOne(
                        hub.getCurrentPlayerName(),
                        "The cards are invalid because they do not have the same value or they are not a kaidan.");
                invalid = true;
                return;
            }


            /* The previous must be the hand.length or none */
            if (previous.getCardCount() == 0) {
                /**
                 * All same value ^ All - 1 same value + joker All - 2 same
                 * value + 2 jokers All same suit + value offset by one All - 1
                 * same suit + value offset by one or two + joker All - 2 same
                 * suit + value offset by one or two or three + 2 jokers
                 *
                 */
                /**
                 * Check here has already been done above
                 */
                if (!invalid) { // Valid
	      /* Check each card for specifics using double check */
                    shibarsprep = 1;
                    doubleCheck(now);
                    invalid = false;
                }
            } else if (previous.getCardCount() == now.getCardCount()) {
                /**
                 * All same value + All great/lesser All - 1 same value
                 * (great/less) + joker All - 2 same value (great/less) + 2
                 * jokers All same suit + value offset by one (great/less) All -
                 * 1 same suit + value offset by one or two (great/less) + joker
                 * All - 2 same suit + value offset by one or two or three
                 * (great/less) + 2 jokers If shibars is on, then same suit.
                 */
                for (int i = 0; i < length - 1; i++) {
                    /* Is currently backwards... will bunch up later */

                    /*
                     * All not same value (with jokers) or not a legit
                     * kaidan (This check has already been done above
                     */

                    /* Removed method used to be here */
                    // 
                    if ((!reversi && (now.getCard(i).getValue() <= previous.getCard(i).getValue()))
                            || (reversi && (now.getCard(i).getValue() >= previous.getCard(i).getValue()))) {
                        // Invalid card must be lower/higher value
                        hub.sendToOne(hub.getCurrentPlayerName(),
                                "The cards are invalid because they must have a higher/lower value.");
                        invalid = true;
                        return;

                    } else if (shibars
                            && (now.getCard(i).getSuit() != previous.getCard(i).getSuit())) {
                        // Invalid shibars is on so card must be same as
                        // previous
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The cards are invalid because they must be have the same suit as the previous hand played.");
                        invalid = true;
                        return;
                    } else if (geki && ((!reversi && now.getCard(0).getValue() != previous.getCard(0).getValue() + 1)
                            || (reversi && now.getCard(0).getValue() + 1 != previous.getCard(0).getValue()))) {
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The cards are invalid because the pile is currently in geki shiba mode.");
                        invalid = true;
                        return;
                    } else if (kaidan && ((now.getCard(i).getSuit() != now.getCard(
                            i + 1).getSuit()) && (now.getCard(i).getValue() + 1 != now.getCard(i + 1).getValue()))) {
                        // Invalid as previous is kaidan but this hand isn't
                        hub.sendToOne(
                                hub.getCurrentPlayerName(),
                                "The cards are invalid because they are not a legitimate kaidan.");
                        invalid = true;
                        return;
                    }
                }

                if (!invalid) {
                    /* Check each card for specifics using double check */
                    doubleCheck(now);
                    invalid = false;
                }
            } else {
                hub.sendToOne(hub.getCurrentPlayerName(),
                        "The cards are invalid because it is not currently a "
                        + length + "-card pile.");
                invalid = true;
            }

        }

    }

    /**
     * Checks if the card activates any restrictions
     *
     * @param current
     */
    private void doubleCheck(Hand current) {
        int length = current.getCardCount();
        /**
         * Method for testings for nines
         *
         */
        boolean nineMessage = false;
        if (length >= 2) {
            int count = 0;
            for (int i = 0; i < length; i++) {
                if (current.getCard(i).getValue() == 9) {
                    count++;
                }
            }
            if (count >= 2) {
                impossible = true;
                nineMessage = true;
            }
        }

        if (nineMessage) {
            hub.sendToAll(hub.getCurrentPlayerName() + " plays two or more 9's.");
        }

        /* Method for testings the shibars */
        if (shibarsprep == 2 && previous.getCardCount() > 0) {
            int count = 0;
            for (int i = 0; i < length; i++) {
                if (current.getCard(i).getSuit() == previous.getCard(i).getSuit()) {
                    count++;
                }
            }
            if (count == current.getCardCount()) {
                shibars = true;
                /* Check for geki shiba (only first card should do as they line up in order anyways) */
                if ((!reversi && current.getCard(0).getValue() == previous.getCard(0).getValue() + 1)
                        || (reversi && current.getCard(0).getValue() + 1 == previous.getCard(0).getValue())) {
                    geki = true;
                }
            }
        }

        /* Method for testing kaidan */
        if (length >= 3) {
            for (int i = 0; i < length - 1; i++) {
                if ((now.getCard(i).getSuit() == now.getCard(i + 1).getSuit())
                        && (now.getCard(i).getValue() + 1 == now.getCard(i + 1).getValue())) {
                    kaidan = true;
                }
            }
        }
        boolean eightMessage = false;
        boolean fiveMessage = false;
        for (int i = 0; i < length; i++) {
            if (current.getCard(i).getValue() == Card.JACK) {
                jack = true;
            } else if (current.getCard(i).getValue() == 7) {
                give = true;
                giveCount++;
            } else if (current.getCard(i).getValue() == 10) {
                away = true;
                throwCount++;
            } else if (current.getCard(i).getValue() == 5) {
                gotobashi++;
                fiveMessage = true;
            }
            if (current.getCard(i).getValue() == 8) {
                if (impossible == false) {
                    impossible = true;
                    eightMessage = true;
                }
            }
            if ((!reversi && (current.getCard(i).getValue() == 15)) || (reversi && (current.getCard(i).getValue() == 3))
                    || (current.getCard(i).getValue() == 8)) {
                chombo = true;
            }
        }

        if (eightMessage) {
            hub.sendToAll(hub.getCurrentPlayerName() + " plays one or more 8's.");
        } else if (fiveMessage) {
            hub.sendToAll(hub.getCurrentPlayerName() + " plays one or more 5's; your turn might have been skipped.");
        }

    }

    private boolean give(Hand now) {

        if (now.getCardCount() > giveCount) {
            return false;
        }

        for (int i = 0; i < now.getCardCount(); i++) {
            /* If the last index points to the same object as the last player hand, wrap around */
            if (getPlayerHands().get(hub.getCurrentPlayerIndex()) == getPlayerHands().get(getPlayerHands().size() - 1)) {
                getPlayerHands().get(0).addCard(now.getCard(i));
            } else {
                getPlayerHands().get(hub.getCurrentPlayerIndex() + 1).addCard(now.getCard(i));
            }

            getPlayerHands().get(hub.getCurrentPlayerIndex()).removeCard(now.getCard(i));

            hub.sendToOne(hub.getCurrentPlayerName(), "You give " + hub.getNextPlayerName() + " the " + now.getCard(i).toString() + ".");
            hub.sendToOne(hub.getNextPlayerName(), hub.getCurrentPlayerName() + " gives you the " + now.getCard(i).toString()
                    + ".");
        }

        give = false; // Variable give set to false so you cannot give
        // twice.
        giveCount = 0; // Can only give once, set back to 0.

        return true;
    }

    private boolean away(Hand now) {

        if (now.getCardCount() > throwCount) {
            return false;
        }

        for (int i = 0; i < now.getCardCount(); i++) {
            handd.addCard(now.getCard(i));
            getPlayerHands().get(hub.getCurrentPlayerIndex()).removeCard(now.getCard(i));
        }

        away = false; // Variable give set to false so you cannot throw away
        // twice.
        throwCount = 0; // Can only throw once, set back to 0.

        hub.sendToOne(hub.getCurrentPlayerName(), "You throw away " + now.getCardCount() + " card(s).");
        for (String player : hub.getPlayerList()) {
            /* If the player is not equal to the current player */
            if (player == null ? hub.getCurrentPlayerName() != null : !player.equals(hub.getCurrentPlayerName())) {
                hub.sendToOne(player, hub.getCurrentPlayerName() + " throws away " + now.getCardCount()
                        + " card(s).");
            }
        }

        return true;
    }

    /**
     * Ending. This method will get called every turn. Implement any timers
     * here.
     */
    public void ending() {
        pauseTimer();
        /**
         * If one player finished his hand, then restart the game. (Check at
         * beginning of this method?)
         */
        for (int i = 0; i < getPlayerHands().size(); i++) {
            if (getPlayerHands().get(i).getCardCount() == 0) {
                /* First update state to show player has 0 cards left */
                hub.sendState();

                /* Code to display last cards played */
                previous.clear();
                int draw = handt.getCardCount() - table;
                for (int j = draw; j < handt.getCardCount(); j++) {
                    previous.addCard(handt.getCard(j));
                }

                hub.sendToAll(hub.getCurrentPlayerName() + " ends with the following cards:");

                for (int k = 0; k < previous.getCardCount(); k++) {
                    hub.sendToAll(previous.getCard(k).toString());
                }

                /* If chombo do not update the points */
                if (chombo) {
                    hub.updatePoints(hub.getCurrentPlayerName(), -10);
                    hub.sendToAll(hub.getCurrentPlayerName() + " ends with an invalid card (chombo): -10 points!");
                    restart();
                    return;
                }

                /**
                 * Allows for level up messages and notifications etc
                 */
                int currentPoints = 0;
                int updatedPoints = 0;

                try {
                    currentPoints = hub.getTracker().getPoints(hub.getCurrentPlayerName());
                } catch (Exception ex) {
                    Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (connectedPlayersID.length == 2) {
                    hub.sendToAll(hub.getCurrentPlayerName() + " wins a two player game: +" + (winPoints / 2) + " points!");
                    hub.updatePoints(hub.getCurrentPlayerName(), (winPoints / 2));
                } else {
                    hub.sendToAll(hub.getCurrentPlayerName() + " wins: +" + (winPoints + (((connectedPlayersID.length * 10) - 30) / 2)) + " points!");
                    hub.updatePoints(hub.getCurrentPlayerName(), winPoints + (((connectedPlayersID.length * 10) - 30) / 2));
                }

                try {
                    updatedPoints = hub.getTracker().getPoints(hub.getCurrentPlayerName());
                } catch (Exception ex) {
                    Logger.getLogger(Validator.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (currentPoints < 300 && updatedPoints > 300) {
                    hub.sendToAll(hub.getCurrentPlayerName() + " has now been granted access to the following rooms: Himajin, One-on-One, and Blitz.");
                    hub.sendToOne(hub.getPlayerName(i), "You have now unlocked a secret feature: sort by suit. "
                            + "By typing into the chat input box \"sort by suit,\" you can now sort your cards according to their suit. "
                            + "If you wish to switch back to the normal mode, just type \"sort by value\" into the chat input box.");
                } else if (currentPoints < 100 && updatedPoints > 100) {
                    hub.sendToAll(hub.getCurrentPlayerName() + " has now been granted access to the following room: Amateur.");
                }

                restart();
                return; // Make sure to return as game has ended
            }
        }

        if (!played) {
            hub.sendToOne(hub.getCurrentPlayerName(), "You skip your turn.");
            for (String player : hub.getPlayerList()) {
                /* If the player is not equal to the current player */
                if (player == null ? hub.getCurrentPlayerName() != null : !player.equals(hub.getCurrentPlayerName())) {
                    hub.sendToOne(player, hub.getCurrentPlayerName() + " skips his or her turn.");
                }
            }
            skipped++;
        } else {
            skipped = 0; // If player did play something then reset skipped back to 0.
        }

        /* If all players skip their turn */
        if (skipped == (hub.getNumberOfConnectedPlayers() - 1)) {
            /* Throw all cards on the table into discard pile */
            restartPile();
        } else {
            if (handt.getCardCount() > 0) {
                previous.clear();
                int draw = handt.getCardCount() - table;
                /*
                 * Adds cards played this turn to the "previous" hand to
                 * be compared with next play.
                 */
                for (int i = draw; i < handt.getCardCount(); i++) {
                    previous.addCard(handt.getCard(i));
                }
            }
        }

        /* Sets shibars prep to false if its true (missed opportunity) or if shibarsprep was already used turn it back to 0*/
        if (shibarsprep == 1) {
            shibarsprep++;
        } else if (shibarsprep == 2) {
            shibarsprep = 0;
        }


        /* Set turn variables to false */
        resetTurnVariables();

        /* If its possible for the next players */
        if (!impossible) {
            /* Switch turn to next person */
            /**
             * Algorithm to take care of gotobashi. SwitchTurn until gotobashi
             * goes to -1 or goes back to your turn
             */
            int players = hub.getNumberOfConnectedPlayers();
            String previousCurrentPlayerID = hub.getCurrentPlayerName();
            int counter = 0;
            skipped--;
            do {
                if (counter < players) {
                    hub.switchTurn(); // Switch only if turn does wrap around back to you.
                    skipped++;
                }
                gotobashi--;
                counter++;
            } while (gotobashi > -1);
            gotobashi = 0;
            String nowCurrentPlayerID = hub.getCurrentPlayerName();
            /* Restart Pile if you gotobashi'ed back to yourself */
            if (previousCurrentPlayerID == null ? nowCurrentPlayerID == null : previousCurrentPlayerID.equals(nowCurrentPlayerID)) {
                restartPile();
            }
        } else {
            /* Reseting the impossible variable is included */
            restartPile();
        }
        /* Send the new state */
        hub.sendState();
        resumeTimer();
    }

    /**
     * Called by ending() if a player has finished. (Add set to default
     * statements later)
     */
    public void restart() {
        /* Clear all restrictions */
        semiRestart();
        //    hub.restart();
        hub.sendToAll("reset");
        String[] players = hub.getPlayerList();
        for (String player : players) {
            hub.sendToOne(player, new DaifugoState(hub.getCurrentPlayerName(), connectedPlayersID, null, handt, table, handd, DaifugoState.DEAL,
                    handCount, 30, shibars, reversi, jack, geki, impossible,
                    give, away, played));
        }

    }

    /**
     * Called to reset all up to non-rotational variables
     */
    private void semiRestart() {
        shibars = false;
        jack = false;
        impossible = false;
        kaidan = false;
        geki = false;
        end = false;
        chombo = false;
        kakumei = false;
        reversi = false;
        table = 0;
        shibarsprep = 0;
        skipped = 0;
        previous.clear();
        now.clear();
        handt.clear();
        handd.clear();
        getPlayerHands().clear();
        handCount.clear();
        give = false;
        away = false;
        invalid = false;
        giveCount = 0;
        throwCount = 0;

        readyCount = 0;
        gameInProgress = false;

        played = false;
        gotobashi = 0;

        timeOutCount.clear();
        dealAgree.clear();
        connectedPlayersID = null;
        if (this.dealTimerObject != null) {
            this.pauseDealTimer();
        }
        if (this.timer != null) {
            this.pauseTimer();
        }
    }

    public void deal(String playerID) {

        if (isGameInProgress() == true) {
            hub.sendToAll("The game is currently in progress. If you have just logged in, please wait for the \"esoteric\" to send you the current game status.");
            return;
        }

        readyCount++;

        if (readyCount == 2 && !dealTimer) {
            hub.sendToAll("The game will start in 10 seconds... \n"
                    + "Please press the start button if you wish to join.");
            this.resumeDealTimer();
            dealAgree.add(playerID);
            return;
        } else if (dealTimer) {
            dealTimer = false;
            this.pauseDealTimer();
            gameInProgress = true;
            // Start
        } else {
            if (dealAgree.size() < 6) {
                dealAgree.add(playerID);
            } else {
                hub.sendToAll("Six people have already registered...please wait for the next game to start or switch rooms.");
            }
            return;
        }

//        if (hub.getPlayerList().length == 1) {
//            hub.sendToAll("There needs to be at least two players for a game to start.");
//            return;
//        } else if (hub.getPlayerList().length <= getReadyCount()) {
//            // Start
//        } else {
//            hub.sendToAll("The game will start once " + (hub.getPlayerList().length - getReadyCount()) + " more player(s) presses \"start\"");
//            return;
//        }
        // hub.shutdownServerSocket();

        /* Shuffles the deck */
        deck.shuffle();

        connectedPlayersID = dealAgree.toArray(new String[dealAgree.size()]);
        int number = ThreadLocalRandom.current().nextInt(0, dealAgree.size());
        hub.setCurrentPlayer(connectedPlayersID[number]);
        String order = "The turn order for this round is as follows: ";
        for (String player : connectedPlayersID) {
            order += player + " -> ";
        }
        order = order.substring(0, order.length() - 4);
        hub.sendToAll(order);

        /* Create the number of hands equal to the number of players */
        for (int i = 0; i < hub.getNumberOfConnectedPlayers(); i++) {
            getPlayerHands().add(new Hand());
        }

        /* Deals 14 cards to each player */
        int cardPerPlayer = 18;
        if (hub.getNumberOfConnectedPlayers() > 2) {

            cardPerPlayer = (54 / hub.getNumberOfConnectedPlayers());

        }

        for (int i = 0; i < cardPerPlayer; i++) {
            for (Hand hand : getPlayerHands()) {
                hand.addCard(deck.dealCard());
            }
        }

        /* Sends messages */
        hub.sendState();
        hub.sendToAll("Dealing cards...");
        resumeTimer();
    }

    /**
     * Called when the pile is reset
     */
    private void resetRotational() {
        shibars = false;
        jack = false;
        reversi = false;
        impossible = false;
        kaidan = false;
        geki = false;
        end = false;
        chombo = false;
        played = false;
        gotobashi = 0;
        shibarsprep = 0;
        skipped = 0;
        table = 0;
        handt.clear();
        previous.clear();
        now.clear();

        hub.sendToAll("reset");

        give = false;
        away = false;
        invalid = false;
        giveCount = 0;
        throwCount = 0;
        illegalMoveCount = 0;
    }

    /**
     * Called after each turn is over
     */
    private void resetTurnVariables() {
        reversi = false;
        played = false;
        give = false;
        away = false;
        invalid = false;
        chombo = false;
        end = false;
        giveCount = 0;
        throwCount = 0;
        illegalMoveCount = 0;
        now.clear();
    }

    private void restartPile() {
        for (int i = 0; i < handt.getCardCount(); i++) {
            handd.addCard(handt.getCard(i));
        }

        hub.sendToAll("A new pile has started.");

        /* Set all rotation variables to false/clear/etc */
        resetRotational();
    }

    /**
     * @return the connectedPlayersID
     */
    public String[] getConnectedPlayersID() {
        return connectedPlayersID;
    }

    /**
     * @return the gameInProgress
     */
    public boolean isGameInProgress() {
        return gameInProgress;
    }


    /* Getter and Setter Methods */
    /**
     * @param time the time to set
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     * @return the playerHands
     */
    public ArrayList<Hand> getPlayerHands() {
        return playerHands;
    }

    /**
     * @param winPoints the winPoints to set
     */
    public void setWinPoints(int winPoints) {
        this.winPoints = winPoints;
    }

    /**
     * @return the readyCount
     */
    public int getReadyCount() {
        return readyCount;
    }
}
