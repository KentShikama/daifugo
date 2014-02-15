package daifugo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class HubMain {

    private final static int[] PORTS = {23548, 23549, 23550, 23551};
    private ArrayList<DaifugoHub> esoterics = new ArrayList<>();

    HubMain() {
        createHubs();
        detectQuit();
    }

    private void createHubs() {
        try {
            for (int i = 0; i < PORTS.length; i++) {
                esoterics.add(new DaifugoHub(PORTS[i]));
            }
        } catch (IOException e) {
            System.out.println("Can't create listening socket. Shutting down.");
        }
    }

    private void detectQuit() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNext()) {
                String quit = scanner.next();
                if (quit.equals("q")) {
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new HubMain();
    }
}
