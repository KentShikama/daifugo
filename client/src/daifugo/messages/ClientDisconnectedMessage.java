package daifugo.messages;

import java.io.Serializable;

/**
 * A message of this type will be sent by the hub to all
 * remaining connected clients when a client leaves the
 * chat room.
 */
public class ClientDisconnectedMessage implements Serializable {

    public String departingClientID;  // The ID number of the client who has left the chat room.

    public ClientDisconnectedMessage(String departingClientID) {
        this.departingClientID = departingClientID;
    }


}
