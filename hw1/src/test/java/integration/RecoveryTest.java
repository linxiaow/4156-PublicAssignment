package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import controllers.PlayGame;
import java.util.concurrent.TimeUnit;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import models.GameBoard;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class RecoveryTest {
  
  /**.
   * Before all, starting the server
   */
  @BeforeAll
  public static void init() {
    PlayGame.main(null);
    System.out.println("Before All, starting the server");
  }
  
  /**
   * This method starts a new game before every test run. It will run every time before a test.
   */
  @BeforeEach
  public void startNewGame() {
    Unirest.get("http://localhost:8080/").asString();
    System.out.println("Starting a new game......");
  }

  /**.
   * Test 1: 
   * Every time a new game starts, 
   * the database table(s) must be cleaned.
   */
  @Test 
  public void testNewGame() {
    Unirest.get("http://localhost:8080/newgame").asString();
    GameBoard board = getBoard();
    
    // test if the board status is cleared
    assertEquals(false, board.getGameStatus()); 
    assertEquals(null, board.getP1());
    assertEquals(null, board.getP2());
    
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        assertEquals('\u0000', board.getPiece(row, col));
      }
    }    
    
  }
  
  /**.
   * Test 1: 
   * Every time a new game starts, 
   * the database table(s) must be cleaned.
   */
  @Test 
  public void testNewGameAfterSeveralMoves() {
    // even if we make some moves and release a new game
    // it should be cleared table
    this.startAndJoin('X');
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();   
    
    // then we start a new game
    Unirest.get("http://localhost:8080/newgame").asString();
    GameBoard board = getBoard();
    
    // test if the board status is cleared
    assertEquals(false, board.getGameStatus()); 
    assertEquals(null, board.getP1());
    assertEquals(null, board.getP2());
    
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        assertEquals('\u0000', board.getPiece(row, col));
      }
    }    
    
  }
  
  /**.
   * Test 2: If the application crashes after a move, 
   * the application must reboot with the game 
   * board's last move.
   * In this test, 1 chose X and it is 1's move
   */
  @Test
  public void testCrashMoveP1Turn() {
    // p1 starts and p2 joins, and p1 makes the first move   
    this.startAndJoin('X');
    // p1 makes a move and p2 makes a move
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=1").asString();
   
    // system crushes and restart
    this.triggerCrush();
    
    // get the board status
    GameBoard board = getBoard();
    assertEquals(true, board.getGameStatus()); 
    assertEquals(1, board.getP1().getID());
    assertEquals(2, board.getP2().getID());
    assertEquals('X', board.getP1().getType());
    assertEquals('O', board.getP2().getType());
    
    assertEquals('X', board.getPiece(1, 1));
    assertEquals('O', board.getPiece(2, 1));
    
    assertEquals(1, board.getTurn());    
    
    // not allowed to move twice
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    GameBoard newBoard = getBoard();
    assertEquals('\u0000', newBoard.getPiece(0, 1));    
    
    // still can make the next move
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    GameBoard newBoard2 = getBoard();
    assertEquals('X', newBoard2.getPiece(0, 1));
    
  }
  
  /**.
   * test 2, in this time
   * P1 choose O and after crush
   * it should be P2's turn
   */
  @Test
  public void testCrashMoveP2Turn() {
    // p1 starts and p2 joins, and p1 makes the first move   
    this.startAndJoin('O');
    // p1 makes a move and p2 makes a move
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    Unirest.post("http://localhost:8080/move/2").body("x=2&y=1").asString();
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
   
    // system crushes and restart
    this.triggerCrush();
    
    // get the board status
    GameBoard board = getBoard();
    assertEquals(true, board.getGameStatus()); 
    assertEquals(1, board.getP1().getID());
    assertEquals(2, board.getP2().getID());
    assertEquals('O', board.getP1().getType());
    assertEquals('X', board.getP2().getType());
    
    assertEquals('O', board.getPiece(1, 1));
    assertEquals('X', board.getPiece(2, 1));
    assertEquals('O', board.getPiece(0, 1)); 
    
    assertEquals(2, board.getTurn());      
  }
  
  /**.
   * Test 3: the application crashes 
   * after the game was a draw,
   * but no new game started, 
   * the application must reboot to show the draw game board.
   */
  @Test
  public void testRecoverDraw() {
    this.startAndJoin('X');
    /*
     *  X O X
     *  X O O
     *  O X X
     *  and draw
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

    this.triggerCrush();
    GameBoard board = getBoard();
    
    assertEquals(true, board.isGameDraw());  
    assertEquals(0, board.getWinner());    
  }
  
  /**.
   * Test 4 If the application crashes after the game has 
   * ended with a winner, but no new game started, 
   * This test is for p1
   */
  @Test
  public void testWinnerP1Declared() {
    this.startAndJoin('X');
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
    this.triggerCrush();
    GameBoard board = getBoard();    
    assertEquals(1, board.getWinner());     
  }
  
  /**.
   * Test 4 If the application crashes after the game has 
   * ended with a winner, but no new game started, 
   * This test is for p2
   */
  @Test
  public void testWinnerP2Declared() {
    this.startAndJoin('X');
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
    this.triggerCrush();
    
    GameBoard board = getBoard();
    assertEquals(2, board.getWinner());
  }
  
  /**.
   * Test 5: If player 1 had started a game and the application crashed, 
   * then the application must be able to reboot with player 1 
   * as part of the game board.
   */
  @Test
  public void testP1StartsAndCrash() {
    System.out.println("Testing p1 start and crush...");
    Unirest.post("http://localhost:8080/startgame").body("type=X").asString();
    this.triggerCrush();
    
    GameBoard board = getBoard();
    assertEquals(1, board.getP1().getID());
    assertEquals('X', board.getP1().getType());
    assertEquals(null, board.getP2()); // p2 is not selected yet
    assertEquals(false, board.getGameStatus()); // not started
    
    // then p2 can declare
    Unirest.get("http://localhost:8080/joingame").asString();
    GameBoard newBoard = getBoard();
    assertEquals(2, newBoard.getP2().getID());
    assertEquals('O', newBoard.getP2().getType());    
  }
  
  /**.
   * Test 6: If player 2 had joined a game and the application crashed, 
   * then the application must be able to reboot 
   * with player 2 as part of the game board with 
   * the corresponding game board status.
   */
  @Test
  public void testTwoPlayersJoinAndCrash() {
    System.out.println("testing two players join and crush...");
    this.startAndJoin('X');
    this.triggerCrush();
    
    GameBoard board = getBoard();
    assertEquals(1, board.getP1().getID());
    assertEquals('X', board.getP1().getType());
    assertEquals(2, board.getP2().getID());
    assertEquals('O', board.getP2().getType());
    assertEquals(true, board.getGameStatus()); // the game started
    assertEquals(1, board.getTurn());
    
    // then p2 can still not able to move
    Unirest.post("http://localhost:8080/move/2").body("x=0&y=1").asString();
    GameBoard newBoard = getBoard();
    assertEquals('\u0000', newBoard.getPiece(0, 1));    
    
    // still can make the next move
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    GameBoard newBoard2 = getBoard();
    assertEquals('X', newBoard2.getPiece(0, 1));
  }
  
  /**.
   * Test 7: a new game was created and the application crashed, 
   * it should reboot to a new game board.
   */
  @Test
  public void testGameCreatedAndCrush() {
    // the game is started
    this.triggerCrush();
    
    // the game is not started, and users did not select
    GameBoard board = getBoard();
    assertEquals(null, board.getP1());
    assertEquals(null, board.getP2());
    assertEquals(false, board.getGameStatus()); // the game not started  
  }
  
  /**.
   * Test 8: If the application crashed after a player made an invalid move, 
   * the application must reboot with the last valid move state.
   * In this case, make a move that is not the turn
   */
  @Test
  public void testOnePlayerMakesTwoMovesAndCrash() {
    System.out.println("testOnePlayerMakesTwoMovesAndCrush...");
    this.startAndJoin('X');
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    
    // then p1 makes the second move (invalid) and crush
    Unirest.post("http://localhost:8080/move/1").body("x=0&y=1").asString();
    this.triggerCrush();
    
    GameBoard board = getBoard();
    assertEquals(true, board.getGameStatus()); 
    assertEquals(1, board.getP1().getID());
    assertEquals(2, board.getP2().getID());
    assertEquals('X', board.getP1().getType());
    assertEquals('O', board.getP2().getType());
    
    assertEquals('X', board.getPiece(1, 1));
    assertEquals('\u0000', board.getPiece(0, 1)); // this is invalid move    
  }
  
  /**.
   * Test 8: If the application crashed after a player made an invalid move, 
   * the application must reboot with the last valid move state.
   * In this case, make a move to an occupied cell
   */  
  @Test
  public void testOneMakesAnOccupiedMoveAndCrash() {
    System.out.println("testOneMakesAnOccupiedMoveAndCrush...");
    this.startAndJoin('X');
    Unirest.post("http://localhost:8080/move/1").body("x=1&y=1").asString();
    
    // then p2 makes an occupied move (invalid) and crush
    Unirest.post("http://localhost:8080/move/2").body("x=1&y=1").asString();
    this.triggerCrush();
    
    GameBoard board = getBoard();
    assertEquals(true, board.getGameStatus()); 
    assertEquals(1, board.getP1().getID());
    assertEquals(2, board.getP2().getID());
    assertEquals('X', board.getP1().getType());
    assertEquals('O', board.getP2().getType());
    
    assertEquals('X', board.getPiece(1, 1)); // not O 
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
  

  /**.
   * @param p1Choice X or O, which is p1's choice
   * 
   *        this will do the P1 start and P2 join
   */
  private void startAndJoin(char p1Choice) {
    String queryBody = "type=" + p1Choice;
    Unirest.post("http://localhost:8080/startgame").body(queryBody).asString();
    Unirest.get("http://localhost:8080/joingame").asString();    
  }
  
  /**.
   * @return the GameBoard result for checking
   */
  private GameBoard getBoard() {
    HttpResponse<String> response = Unirest.get("http://localhost:8080/gameboard").asString();
    String responseBody = (String) response.getBody();
    JSONObject jsonObject = new JSONObject(responseBody);
    Gson gson = new Gson();
    GameBoard gameBoard = gson.fromJson(jsonObject.toString(), GameBoard.class);
    return gameBoard;
  }
  
  private void triggerCrush() {
    PlayGame.stop();
    try {
      // give time to start over
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    System.out.println("System crushes...");
    PlayGame.main(null); 
  }
  
}
