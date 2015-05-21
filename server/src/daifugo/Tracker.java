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

    private static final Tracker INSTANCE = new Tracker();

    private enum Mode {
        TOURNAMENT, POINTS;
    }

    private Tracker() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        readXML();
    }

    public static Tracker getInstance() {
        return INSTANCE;
    }

    public int getTournamentPoints(String username) throws Exception {
        return getPoints(username, Mode.TOURNAMENT);
    }

    public int getRegularPoints(String username) throws Exception {
        return getPoints(username, Mode.POINTS);

    }

    private int getPoints(String username, Mode mode) throws Exception, NumberFormatException {
        Element root = xmldoc.getRootElement();
        int points = -1;
        if (!root.getName().equalsIgnoreCase("daifugo")) {
            throw new Exception("Incompatbile File");
        }
        List<Element> nodes = root.getChildren();
        for (int i = 0; i < nodes.size(); i++) {
            Element element = nodes.get(i);
            if (element.getName().equalsIgnoreCase("player")) {
                if (username.equalsIgnoreCase(element.getChildText("username"))) {
                    points = Integer.parseInt(element.getChild(mode.name().toLowerCase()).getText());
                }
            }
        }
        return points;
    }

    public synchronized void updateTournamentPoints(String username, int change) throws Exception {
        updatePoints(username, change, Mode.TOURNAMENT);
    }

    public synchronized void updateRegularPoints(String username, int change) throws Exception {
        updatePoints(username, change, Mode.POINTS);
    }

    private void updatePoints(String username, int change, Mode mode) throws Exception, NumberFormatException {
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
                    element.getChild(mode.name().toLowerCase()).setText(String.valueOf(current));
                }
            }
        }
        writeXML();
        readXML();
    }

    public String authenticate(String name, String password, int port) {
        if (port == DaifugoHub.ACCOUNT) {
            return handleAccountCreation(name, password);
        }
        return handleAccountAuthentication(port, name, password);
    }

    private String handleAccountCreation(String name, String password) {
        String regex = "[A-Za-z0-9\\s]{1,10}$";
        if (!name.isEmpty() && !password.isEmpty() && name.matches(regex)) {
            for (Player player : players) {
                if (player.getUsername().equals(name)) {
                    return "duplicateAccountCreation";
                }
            }
            Element player = createPlayerElement(name, password);
            Element root = xmldoc.getRootElement();
            root.addContent(player);
            writeXML();
            readXML();
            return "accountCreated";
        }
        return "failedAccountCreation";
    }

    private Element createPlayerElement(String name, String password) {
        Element player = new Element("player");
        Element usernameElement = new Element("username").setText(name);
        Element passwordElement = new Element("password").setText(password);
        Element pointsElement = new Element("points").setText("0");
        Element tournamentElement = new Element("tournament").setText("0");
        player.addContent(usernameElement);
        player.addContent(passwordElement);
        player.addContent(pointsElement);
        player.addContent(tournamentElement);
        return player;
    }

    private String handleAccountAuthentication(int port, String name, String password) {
        int points = neededPoints(port);
        for (Player p : players) {
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
            boolean isNew = tracker.createNewFile(); // Create file if it doesn't exist
            if (isNew) {
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(tracker));
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<daifugo>\n" +
                            "  <player>\n" +
                            "    <username>player1</username>\n" +
                            "    <password>1234</password>\n" +
                            "    <points>0</points>\n" +
                            "    <tournament>0</tournament>\n" +
                            "  </player>\n" +
                            "  <player>\n" +
                            "    <username>player2</username>\n" +
                            "    <password>1234</password>\n" +
                            "    <points>0</points>\n" +
                            "    <tournament>0</tournament>\n" +
                            "  </player>\n" +
                            "</daifugo>\n");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            SAXBuilder builder = new SAXBuilder();
            xmldoc = builder.build(tracker);
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
        try {
            readDocument(xmldoc);
        } catch (Exception e) {
            e.printStackTrace();
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

    private int neededPoints(int port) {
        if (port == DaifugoHub.TEST) {
            return -200;
        } else if (port == DaifugoHub.BLITZ) {
            return -30;
        } else if (port == DaifugoHub.TOURNAMENT) {
            return -30;
        } else {
            return -1;
        }
    }

    public String getUserhome() {
        return userhome;
    }
}
