package daifugo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class HubMain {

    private final static int[] PORT = {23548, 23549, 23550, 23551};
    ArrayList<DaifugoHub> esoterics = new ArrayList<>();

    HubMain() {
        try {
            for (int i = 0; i < PORT.length; i++) {
                esoterics.add(new DaifugoHub(PORT[i]));
            }
        } catch (IOException e) {
            System.out.println("Can't create listening socket.  Shutting down.");
        }
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNext()) {
                String quit = scanner.next();
                if (quit.equals("9")) {
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
