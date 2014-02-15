package daifugo;

public class Player {
    
    private String username;
    private String password;
    private int points;
    private int tournament;
    private boolean playing = false; // When server starts no players have logged in

    Player(String username, String password, int points, int tournament) {
        this.username = username;
        this.password = password;
        this.points = points;
        this.tournament = tournament;
    }

    @Override
    public String toString() {
        return "Username: " + getUsername() + ". Password: " + getPassword() + ". Number of Points: " + getPoints() + ".";
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the points
     */
    public int getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * @return the playing
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * @param playing the playing to set
     */
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    /**
     * @return the tournament
     */
    public int getTournament() {
        return tournament;
    }

    /**
     * @param tournament the tournament to set
     */
    public void setTournament(int tournament) {
        this.tournament = tournament;
    }

}
