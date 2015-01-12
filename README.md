Daifugo======Description-----------Daifugo is a Japanese card game in which players strategically try to get rid off all the cards in their hands. Players take turns playing cards from their hands while obeying the restrictions set by previously played cards. The winner is the player who is able to get rid off all of his or her cards first. Please read the [manual](manual.pdf) for a more detailed description.To Play-------Download the [Daifugo.jar](Daifugo.jar) file in the repository and double click the jar file to execute.Note that it automatically connects to the default server that I am currently hosting.Running Your Own Daifugo Server (Configuration Instructions)--------------------------<h3>Requirements</h3>A server with the JRE 1.7+ installed. 30+ players can be hosted with 512MB of RAM.<h3>Instructions</h3>1. Install [Gradle](http://www.gradle.org/installation)2. Modify the HOST constant in ClientMain.java to match the server’s IP address.3. Execute `gradle jar` in the project's working directory.4. Distribute the `client.jar` file which will be automatically built under `/path/to/working/directory/client/build/libs/client.jar`.5. Upload the `server.jar` file which will be automatically built under `/path/to/working/directory/server/build/libs/server.jar` to your server.6. Execute `nohup java -jar /path/to/server/file/server.jar &` on your server.Note that the account details will be stored in an automatically generated file, daifugo.xml, in the server user's home directory.Libraries/Frameworks Used-------------------------* [netgame.common] (http://math.hws.edu/eck/cs124/javanotes6/source/netgame/common/)* [Card](http://math.hws.edu/javanotes/source/Card.java), [Hand](http://math.hws.edu/javanotes/source/Hand.java), and [Deck](http://math.hws.edu/javanotes/source/Deck.java) classes* [jdom 2](http://www.jdom.org)Known Bugs/Limitations----------------------The application was not designed with the intent to be distributed in masses. Passwords are stored in plain text and a linear search algorithm is used for querying usernames. Before any new functionality is added, the Validator class needs to be refactored. The UI for the client needs a redesign.