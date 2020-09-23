package controllers;

import com.google.gson.Gson;
import io.javalin.Javalin;
import java.io.IOException;
import java.util.Queue;
import models.GameBoard;
import models.Message;
import models.Move;
import models.Player;
import org.eclipse.jetty.websocket.api.Session;

class PlayGame {

  private static final int PORT_NUMBER = 8080;
  private static Gson gson = new Gson();
  private static Javalin app;
  private static GameBoard gameboard;
  private static Player p1;
  private static Player p2;

  /** Main method of the application.
   * @param args Command line arguments
   */
  public static void main(final String[] args) {

    app = Javalin.create(config -> {
      config.addStaticFiles("/public");
    }).start(PORT_NUMBER);

    // Test Echo Server
    app.post("/echo", ctx -> {
      ctx.result(ctx.body());
    });

    // Redirect to new game
    app.get("/", ctx -> {
      ctx.redirect("/newgame");
    });
    
    // Start a new game
    app.get("/newgame", ctx -> {
      gameboard = new GameBoard();
      ctx.redirect("/tictactoe.html");
    });
    
    // Player 1 starts the game
    app.post("/startgame", ctx -> {
      String str = ctx.body();
      char type = str.charAt(str.length() - 1);
      p1 = new Player(type, 1);
      
      gameboard.setPlayer1(p1);
      ctx.status(201); // created
      ctx.result(gson.toJson(gameboard));
    });
    
    // Player 2 joins the game
    app.get("/joingame", ctx -> {
      char type = 'O';
      if (p1.getType() == 'O') {
        type = 'X';
      }
      p2 = new Player(type, 2);
      gameboard.setPlayer2(p2);
      ctx.status(302); // redirected
      ctx.redirect("/tictactoe.html?p=2");
      sendGameBoardToAllPlayers(gson.toJson(gameboard));
    });
    
    // movement
    app.post("/move/:playerId", ctx -> {
      int id = Integer.parseInt(ctx.pathParam("playerId"));
      String str = ctx.body();
      int x = Character.getNumericValue(str.charAt(2));
      int y = Character.getNumericValue(str.charAt(6));
      Move move;
      if (id == 1) {
        move = new Move(p1, x, y);
      } else {
        move = new Move(p2, x, y);
      }
      String message = gameboard.validMove(move);
      System.out.println(message);
      if (message != "") {
        Message mes = new Message(false, 100, message);
        ctx.result(gson.toJson(mes));
      } else {
        // make movement
        gameboard.makeMovement(move);
        Message mes = new Message(true, 100, "");
        ctx.result(gson.toJson(mes));
      }
      System.out.println(gameboard);
      sendGameBoardToAllPlayers(gson.toJson(gameboard));
    });

    // Web sockets - DO NOT DELETE or CHANGE
    app.ws("/gameboard", new UiWebSocket());
  }

  /** Send message to all players.
   * @param gameBoardJson Gameboard JSON
   * @throws IOException Websocket message send IO Exception
   */
  private static void sendGameBoardToAllPlayers(final String gameBoardJson) {
    Queue<Session> sessions = UiWebSocket.getSessions();
    for (Session sessionPlayer : sessions) {
      try {
        sessionPlayer.getRemote().sendString(gameBoardJson);
      } catch (IOException e) {
        // Add logger here
      }
    }
  }

  public static void stop() {
    app.stop();
  }
}
