package daifugo;

import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public final class Tracker {

    private File tracker;
    private Document xmldoc;
    private ArrayList<Player> players;
    private final String userhome = System.getProperty("user.home");
    private File newAccount;

    private static final Tracker INSTANCE = new Tracker();

    private Tracker() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        readXML();
    }

    public static Tracker getInstance() {
        return INSTANCE;
    }

    public int getTournament(String username) throws Exception {
        Element root = xmldoc.getRootElement();
        int tournament = -1;
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                if (username.equalsIgnoreCase(element.getChildText("username"))) {
                    tournament = Integer.parseInt(element.getChild("tournament").getText());
                }
            }
        }
        return tournament;
    }

    /**
     * Method called to update the tournament points of a player
     */
    public synchronized void updateTournament(String username, int change) throws Exception {
        Element root = xmldoc.getRootElement();
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                if (username.equalsIgnoreCase(element.getChildText("username"))) {
                    int past = Integer.parseInt(element.getChild("tournament").getText());
                    int current = past + change;
                    element.getChild("tournament").setText(String.valueOf(current));
                }
            }
        }
        writeXML();
        readXML();
    }

    public int getPoints(String username) throws Exception {
        Element root = xmldoc.getRootElement();
        int points = -1;
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = (Element) nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                if (username.equalsIgnoreCase(element.getChildText("username"))) {
                    points = Integer.parseInt(element.getChild("points").getText());
                }
            }
        }
        return points;
    }

    public synchronized void updatePoints(String username, int change) throws Exception {
        Element root = xmldoc.getRootElement();
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = (Element) nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                if (username.equalsIgnoreCase(element.getChildText("username"))) {
                    int past = Integer.parseInt(element.getChild("points").getText());
                    int current = past + change;
                    element.getChild("points").setText(String.valueOf(current));
                }
            }
        }
        writeXML();
        readXML();
    }

    /**
     * See if the username and password are valid
     *
     * Note: Rooms can be closed by making the amount of needed points equal to
     * -1.
     *
     * @param name
     * @param password
     * @param port
     * @return Message to tell if user was authenticated or what kind of error
     * the user activated.
     */
    public String authenticate(String name, String password, int port) {
        if (port == DaifugoHub.ACCOUNT) {
            String regex = "^[A-Z][A-Za-z\\s]{1,10}$";
            if (!name.isEmpty() && !password.isEmpty() && name.matches(regex)) {
                for (Player player: players) {
                    if (player.getUsername().equals(name)) {
                        return "duplicateAccountCreation"; 
                    }
                }
                Element player = new Element("player");
                Element usernameElement = new Element("username").setText(name);
                Element passwordElement = new Element("password").setText(password);
                Element pointsElement = new Element("points").setText("0");
                Element tournamentElement = new Element("tournament").setText("0");
                player.addContent(usernameElement);
                player.addContent(passwordElement);
                player.addContent(pointsElement);
                player.addContent(tournamentElement);
                Element root = xmldoc.getRootElement();
                root.addContent(player);
                writeXML();
                readXML();
                return "accountCreated";
            }
            return "failedAccountCreation";
        }
        int points = neededPoints(port);
        if (points == -1) {
            return "closedError";
        }
        for (Player p : players) {
            /* Make sure username matches password and that the player is not currently logged in */
            if (p.getUsername().equals(name)) {
                if (p.getPassword().equals(password)) {
                    if (!p.isPlaying()) {
                        if (port != DaifugoHub.TOURNAMENT && p.getPoints() >= points) {
                            p.setPlaying(true);
                            return "authenticated";
                        } else if (port == DaifugoHub.TOURNAMENT && p.getTournament() >= points) {
                            p.setPlaying(true);
                            return "authenticated";
                        } else {
                            return "pointsError";
                        }
                    } else {
                        return "doubleLoginError";
                    }
                } else {
                    return "combinationError";
                }
            }
        }
        return "combinationError";
    }

    public void logout(String name) {
        for (Player p : players) {
            if (p.getUsername().equals(name)) {
                p.setPlaying(false);
            }
        }
    }

    /**
     * Called after the updatePoints method to write the new values to the XML
     * file
     */
    private synchronized void writeXML() {
        PrintWriter out;
        try {
            FileOutputStream stream = new FileOutputStream(tracker);
            out = new PrintWriter(stream);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(xmldoc, new FileWriter(tracker));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void readXML() {
        players = new ArrayList<>();
        tracker = new File(getUserhome() + "/daifugo.xml");
        try {
            SAXBuilder builder = new SAXBuilder();
            xmldoc = builder.build(tracker);

        } catch (IOException | JDOMException e) {
            System.out.println(e.getMessage());
        }
        try {
            readDocument(xmldoc);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void readDocument(Document xmldoc) throws Exception {
        Element root = xmldoc.getRootElement();
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                String username = element.getChildText("username");
                String password = element.getChildText("password");
                int points = Integer.parseInt(element.getChildText("points"));
                int tournament = Integer.parseInt(element.getChildText("tournament"));
                players.add(new Player(username, password, points, tournament));
            }
        }
    }

    /**
     * Returns the needed points for each port. Returns -1 if room is closed.
     *
     * @param port
     * @return
     */
    private int neededPoints(int port) {
//        if (port == DaifugoHub.NEOPHYTE) {
//            return -200;
//        } else if (port == DaifugoHub.AMATEUR) {
//            return 100;
//        } else if (port == DaifugoHub.HIMAJIN) {
//            return 300;
//        } else if (port == DaifugoHub.TOURNAMENT) {
//            return -30;
//        } else {
//            return 0;
//        }
        return -50;
    }

    /**
     * @return the userhome
     */
    public String getUserhome() {
        return userhome;
    }
}
