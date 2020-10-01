package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import controllers.PlayGame;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import models.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestMethodOrder;

//@TestMethodOrder(OrderAnnotation.class)
class GameTest {

  private static final String HttpResponse = null;

  /**
   * . Run only once before the game starts
   */
  @BeforeAll
  public static void init() {
    PlayGame.main(null);
    System.out.println("Before All");
  }

  /**
   * This method starts a new game before every test run. It will run every time before a test.
   */
  @BeforeEach
  public void startNewGame() {
    HttpResponse response = Unirest.get("http://localhost:8080/").asString();
    int restStatus = response.getStatus();
    System.out.println("Starting a new game......");
  }
  
  /** .
   * Test echo works fine
   */
  @Test
  public void testEcho() {
    HttpResponse response = 
        Unirest.post("http://localhost:8080/echo").body("Hello").asString();
    String responseBody = (String) response.getBody();
    assertEquals("Hello", responseBody);
  }

  /**
   * .test newgame endpoint
   */
  @Test
  public void testNewGame() {
    System.out.println("Testing new game");
    HttpResponse response = Unirest.get("http://localhost:8080/newgame").asString();
    int restStatus = response.getStatus();

    // check the response
    assertEquals(restStatus, 200);
    System.out.println("Test New Game");
  }

  /**.
   * Test player 1 starts the game 
   * It will send a post request to the endpoint
   * Test: **game is not started**
   * and the type is correct
   */
  @Test
  public void testStartGame() {
    System.out.println("Testing start new game......");
    HttpResponse response = 
        Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    String responseBody = (String) response.getBody();
    int restStatus = response.getStatus();
    assertEquals(restStatus, 201);
    
    // --------------------------- JSONObject Parsing ----------------------------------
    System.out.println("Start Game Response: " + responseBody);
    
    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    
    // Check if game started after player 1 joins: Game should not start at this point
    assertEquals(false, jsonObject.get("gameStarted"));
    
    // ---------------------------- GSON Parsing -------------------------
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player player1 = gameBoard.getP1();
    // Check if player type is correct
    assertEquals('X', player1.getType());
  }

  /**.
   * Test player 2 make a get request to join gain
   * Test: game is started
   * and it is player1's turn
   */
  @Test
  public void testJoinGame() {
    System.out.println("Testing join a game");
    // beforeEach begins a whole new game
    // make a post to start game from player 1
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    
    // then p2 can join
    HttpResponse response = Unirest.get("http://localhost:8080/joingame").asString();
    int restStatus = response.getStatus();
    assertEquals(restStatus, 200);
    
    // game is started
    // and it is p1's turn
    response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("gameStarted"));
    assertEquals(1, jsonObject.get("turn"));
  }
  
  
  /**.
   * If P1 start by choosing O
   * P2 should automatically chooses 1
   */
  @Test
  public void testP1ChoosesO() {
    System.out.println("Testing P1 chooses 0");
    Unirest.post("http://localhost:8080/startgame").body("type=O").asString();
    
    // then p2 can join
    Unirest.get("http://localhost:8080/joingame").asString();
    
    // game is started
    // and it is p1's turn
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    Player player1 = gameBoard.getP1();
    Player player2 = gameBoard.getP2();
    
    // check there choices
    assertEquals('O', player1.getType());  
    assertEquals('X', player2.getType());  
  }
  
  /**.
   * Player 2 cannot make the first move
   * after p1 start a gain and p2 join a game
   * p2 cannot make the first move
   */
  @Test
  public void testP2NotAbleToMoveFirst() {
    System.out.println("Testing P2 not able to make the first move");
    
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    
    HttpResponse response = Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    String responseBody = (String) response.getBody();
    System.out.println("testP2NotAbleToMoveFirst Response: " + responseBody);
    
    // Parse the response to JSON object
    JSONObject jsonObject = new JSONObject(responseBody);
    
    // check the valid move is false
    assertEquals(false, jsonObject.get("moveValidity"));
    System.out.println("Reason is : " + jsonObject.get("message"));
  }
  
  /**.
   * Player1 can make the first move
   */
  @Test
  public void testP1AbleToMoveFirst() {
    System.out.println("Testing P1 able to make the first move");
    
    // p1 starts and p2 joins, and p1 makes the first move
    // similar structure to testP2NotAbleToMoveFirst
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();
    
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    String responseBody = (String) response.getBody();
    System.out.println("testP1AbleToMoveFirst Response: " + responseBody);
    
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("moveValidity"));
  }
  
  /**.
   * Player1 cannot make the two moves
   */
  @Test
  public void testP1CannotMakeTwoMoves() {
    System.out.println("Testing not able to move twice");
    
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();  
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    
    // then p1 makes the second move
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    String responseBody = (String) response.getBody();
    
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("moveValidity"));    
    System.out.println("Reason is : " + jsonObject.get("message"));
  }
  
  /**.
   * Player 1 cannot move in an occupied cell
   */
  @Test
  public void testP1CannotMakeOccupiedMove() {
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();  
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=2").asString();
    // then p1 makes the second move
    HttpResponse response = Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    String responseBody = (String) response.getBody();
    
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(false, jsonObject.get("moveValidity"));    
    System.out.println("Reason is : " + jsonObject.get("message"));
  }
  
  
  /**.
   * P1 should be able to win the game
   * if a row is occupied
   */
  @Test
  public void testP1ShouldWinInRow() {   
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X X X
     *  O O _
     *  _ _ _
     *  and P1 should win
     *  
     * */
    boolean p1move = true;
    for (int j = 0; j < 4; j++) {
      int i = j / 2;
      
      // p1 and p2 take turns to move
      if (p1move) {
        String requestBody = String.format("x=%d&y=%d", 0, i);
        System.out.println("Request body is: " + requestBody);
        Unirest.post("http://localhost:8080/move/1").body(requestBody).asString();
      } else {
        String requestBody = String.format("x=%d&y=%d", 1, i);
        System.out.println("Request body is: " + requestBody);
        Unirest.post("http://localhost:8080/move/2").body(requestBody).asString();
      }
      p1move = !p1move;
    }
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    
    // check the game status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(1, jsonObject.get("winner"));    
  }
  
  /**.
   * p2 should be able to win the game
   * if a row is occupied
   */
  @Test
  public void testP2ShouldWinInRow() {    
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X X _
     *  O O O
     *  X _ _
     *  and P2 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=2").asString();
    
    // check the game status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(2, jsonObject.get("winner"));    
  }
  
  /**.
   * One player should be able to win the game
   * if a column is occupied
   */
  @Test
  public void testOnePlayerShouldWinInCol() {
    System.out.println("Testing winning in a column");
    
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X O _
     *  X O _
     *  X _ _
     *  and P1 should win
     *  
     * */
    boolean p1move = true;
    for (int j = 0; j < 4; j++) {
      int i = j / 2;
      
      // p1 and p2 take turns to move
      if (p1move) {
        String requestBody = String.format("x=%d&y=%d", i, 0);
        System.out.println("Request body is: " + requestBody);
        Unirest.post("http://localhost:8080/move/1").body(requestBody).asString();
      } else {
        String requestBody = String.format("x=%d&y=%d", i, 1);
        System.out.println("Request body is: " + requestBody);
        Unirest.post("http://localhost:8080/move/2").body(requestBody).asString();
      }
      p1move = !p1move;
    }
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    
    // check the game status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(1, jsonObject.get("winner"));    
  }
  
  /**.
   * p2 should be able to win the game
   * if a column is occupied
   */
  @Test
  public void testP2ShouldWinInCol() {    
    // p1 starts and p2 joins, and p1 makes the first move
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X O X
     *  X O _
     *  _ O _
     *  and P2 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=1").asString();
    
    // check the game status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(2, jsonObject.get("winner"));    
  }
 
  /**.
   * P1 should be able to win the game
   * if a diagonal is occupied
   */
  @Test
  public void testP1ShouldWinInDiag() {
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X O O
     *  _ X _
     *  _ _ X
     *  and P1 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
    
    // check status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(1, jsonObject.get("winner"));    
  }

  /**.
   * P2 should be able to win the game
   * if a diagonal is occupied
   */
  @Test
  public void testP2ShouldWinInDiag() {
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  O X X
     *  _ O _
     *  X _ O
     *  and P2 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=2").asString();
    
    // check status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(2, jsonObject.get("winner"));    
  }
  
  /**.
   * P1 should be able to win the game
   * if a diagonal is occupied in a second patterns
   */
  @Test
  public void testP1ShouldWinInDiag2() {
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  O O X
     *  _ X _
     *  X _ _
     *  and P1 should win
     *  
     * */
    
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=0").asString();
    
    // check status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(1, jsonObject.get("winner"));    
  }
  
  /**.
   * P2 should be able to win the game
   * if a diagonal is occupied in a second patterns
   */
  @Test
  public void testP2ShouldWinInDiag2() {
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();   
    
    /*
     *  X X O
     *  _ O _
     *  O _ X
     *  and P2 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=0").asString();
    
    // check status and p1 should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(2, jsonObject.get("winner"));    
  }
  
  /** .
   * Test: when the board is fully occupied
   * and no one reaches the winning criteria
   * the game should draw
   * and no one wins
   */
  @Test
  public void testTwoPlayersCanDraw() {
    // p1 starts and p2 joins
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    Unirest.get("http://localhost:8080/joingame").asString();  
    /*
     *  X O X
     *  X O O
     *  O X X
     *  and P1 should win
     *  
     * */
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=0").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=2").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=2").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=0").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=0").asString();
    
    // check status and it is a draw
    // no one should win
    HttpResponse response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    assertEquals(true, jsonObject.get("isDraw"));  
    assertEquals(0, jsonObject.get("winner"));
  }
  
  /**
  * This will run every time after a test has finished.
  */
  @AfterEach
  public void finishGame() {
    System.out.println("Game finishes");
  }
  
  /**
   * This method runs only once after all the test cases have been executed.
   */
  @AfterAll
  public static void close() {
    // Stop Server
    PlayGame.stop();
    System.out.println("After All, app is closed");
  }
  
  

}
