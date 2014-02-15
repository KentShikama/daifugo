package daifugo.messages;

import java.io.Serializable;

/**
 * A message of this type will be sent by the hub to all
 * connected clients when a new client joins the chat room
 */
public class ClientConnectedMessage implements Serializable {

    public String newClientID;  // The ID number of the client who has connected.

    public ClientConnectedMessage(String newClientID) {
        this.newClientID = newClientID;
    }

}