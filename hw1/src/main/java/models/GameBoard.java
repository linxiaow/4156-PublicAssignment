package models;

public class GameBoard {

  private Player p1;

  private Player p2;

  private boolean gameStarted;

  private int turn;

  private char[][] boardState;

  private int winner;

  private boolean isDraw;
  
  /**.
   * check and update private member
   * if any line of column or diagonal is of one type
   * then one user wins;
   * .
   * if all spaces are occupied
   * then it is a draw
   */
  private void update() {
    for (int i = 0; i < 3; i++) {
      if (boardState[i][0] == boardState[i][1] 
          && boardState[i][1] == boardState[i][2] 
          && boardState[i][0] != '\u0000') {
        if (p1.getType() == boardState[i][0]) {
          this.winner = 1;
        } else {
          this.winner = 2;
        }
      }
    }
    
    for (int i = 0; i < 3; i++) {
      if (boardState[0][i] == boardState[1][i] 
          && boardState[1][i] == boardState[2][i]
          && boardState[0][i] != '\u0000') {
        if (p1.getType() == boardState[0][i]) {
          this.winner = 1;
        } else {
          this.winner = 2;
        }
      }
    }
    
    if (boardState[0][0] == boardState[1][1] 
        && boardState[1][1] == boardState[2][2]
        && boardState[1][1] != '\u0000') {
      if (p1.getType() == boardState[1][1]) {
        this.winner = 1;
      } else {
        this.winner = 2;
      }      
    }
    
    if (boardState[2][0] == boardState[1][1] 
        && boardState[1][1] == boardState[0][2]
        && boardState[1][1] != '\u0000') {
      if (p1.getType() == boardState[1][1]) {
        this.winner = 1;
      } else {
        this.winner = 2;
      }      
    }
    
    if (this.winner != 0) {
      // no need to continue to check draw
      return;
    }
    
    this.isDraw = true;
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (boardState[i][j] == '\u0000') {
          this.isDraw = false;
        }
      }
    }
    
  }
  
  /** class constructor.
   * set the turn to 1
   * initialize the board
   * and set all the private member to
   * original values
   */
  public GameBoard() {
    this.turn = 1;
    this.boardState = new char[][] {
      {'\u0000', '\u0000', '\u0000'},
      {'\u0000', '\u0000', '\u0000'},
      {'\u0000', '\u0000', '\u0000'}
    };
    this.winner = 0;
    this.gameStarted = false;
    this.isDraw = false;
  }
  
  /**
   * valid move check.
   * check valid move from move object
   * if id is not the turn 
   * or boardState[x][y] is occupied
   * return corresponding message
   * if the message  
   */
  public String validMove(Move move) {
    int id = move.getPlayerID();
    int x = move.getX();
    int y = move.getY();
    String message = "";
    if (id != this.turn) {
      message = "Not your turn!";
    } else if (boardState[x][y] != '\u0000') {
      message = "This cell has been occupied";
    }
    return message;
  }

  
  /**.
   * @param move includes player and cell intended
   */
  public void makeMovement(Move move) {
    /*
     * check if the game ends as well
     * */
    int id = move.getPlayerID();
    char type = move.getPlayerType();
    int x = move.getX();
    int y = move.getY();
    
    if (id == 1) {
      this.turn = 2;
    } else {
      this.turn = 1;
    }
    
    this.boardState[x][y] = type;
    update();
  }
  
  /** set the player 1 in board.*/
  public void setPlayer1(Player p) {
    this.p1 = p;
  }
  
  /** set the player 2 and change the turn to 1.*/
  public void setPlayer2(Player p) {
    this.p2 = p;
    // when p2 is in, game starts
    this.gameStarted = true;
  }
}
