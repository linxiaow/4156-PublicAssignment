package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import models.GameBoard;
import models.Move;
import models.Player;

public class Database {
  
  private Connection conn = null;
  private String boardTable = "board";
  private String playerTable = "player";
  
  /**.
   * Constructor
   */
  public Database() {
    this.createConnection();
  }
  
  /**.
   * @param args no use
   *        use to drop table to test
   */
  public static void main(String[] args) {
    Database db = new Database();
    db.createConnection();
    db.dropTable();
  }
  
  /**.
   * Create player table and board table
   * if not exist
   */
  public void createTable() {
    // using format to avoid security issue
    final String boardSql = 
        String.format("CREATE TABLE IF NOT EXISTS %s "
        + "(row INT NOT NULL," 
        + " col INT NOT NULL," 
        + " playerID INT NOT NULL)", this.boardTable);
    this.execute(boardSql);
    System.out.println("boardTable created successfully");
    
    final String playerSql = 
        String.format("CREATE TABLE IF NOT EXISTS %s "
        + "(playerID INT NOT NULL," 
        + " type CHAR NOT NULL)", this.playerTable);
    this.execute(playerSql);
    System.out.println("playerTable created successfully");    
  }
  
  /**.
   * drop all the tables
   */
  public void dropTable() {
    final String dropBoard = String.format("DROP TABLE IF EXISTS %s;", 
        this.boardTable);
    final String dropPlayer = String.format("DROP TABLE IF EXISTS %s;", 
        this.playerTable);   
    this.execute(dropBoard);
    System.out.println("boardTable dropped successfully");  
    this.execute(dropPlayer);
    System.out.println("playerTable dropped successfully");  
  }
  
  /** .
   * @param move insert movement record to database
   */
  public void addMove(Move move) {
    //"(row, col, playerID)"
    String sql = String.format(
        "INSERT INTO %s VALUES(?, ?, ?);", this.boardTable);
    
    PreparedStatement stmt = null;
    try {
      stmt = this.conn.prepareStatement(sql);
      stmt.setInt(1, move.getX());
      stmt.setInt(2, move.getY());
      stmt.setInt(3, move.getPlayerID());
      this.secureUpdate(stmt);
      stmt.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      try { 
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e2) { 
        ;
      }
    }
    System.out.println("update movement");  
  }
  
  
  /**.
   * @param player player to be update
   */
  public void addPlayers(Player player) {
    String sql = String.format(
        // (playerID, type)
        "INSERT INTO %s VALUES(?, ?)", this.playerTable);
    PreparedStatement stmt = null;
    try {
      stmt = this.conn.prepareStatement(sql);
      stmt.setInt(1, player.getID());
      stmt.setString(2, String.valueOf(player.getType()));
      this.secureUpdate(stmt);
      stmt.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e2) { 
        ;
      }
    }
    System.out.println("update player");     
  }
  
  /**.
   * @param board the board from the game
   *        recover board after crush
   * @throws SQLException SQLexception
   */
  public void recover(GameBoard board) {   
    PreparedStatement stmt = null;
    ResultSet rs = null;
    // mapping[0] is p1, mapping[1] is p2
    char[] mapping = {'\u0000', '\u0000'};
    try {
      // row, col, playerID
      final String sql = String.format(
          "SELECT * FROM %s;", this.playerTable);
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      // track number of movement for different
      // counts[0] is p1, counts[1] is p2
      while (rs.next()) {
        int id = rs.getInt("playerID");
        char type = rs.getString("type").charAt(0);
        mapping[id - 1] = type;
      }
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
    }
    
    // set player 1 and player 2
    Player p1 = new Player(mapping[0], 1);
    Player p2 = new Player(mapping[1], 2);
    
    // need to check mapping if it is '\u0000'
    if (mapping[0] != '\u0000') {
      board.setPlayer1(p1);
    }
    if (mapping[1] != '\u0000') {
      board.setPlayer2(p2);
    }
    
    // set game status
    // if any user is null, the game is not started
    // otherwise true
    if ((mapping[0] == '\u0000') || (mapping[1] == '\u0000')) {
      board.setGameStatus(false);
    } else {
      board.setGameStatus(true);
    }
    
    // counting the number of movement
    int[] counts = {0, 0};
    try {      
      // row, col, playerID
      final String sql = String.format(
          "SELECT * FROM %s;", this.boardTable);
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      // track number of movement for different
      // counts[0] is p1, counts[1] is p2
      while (rs.next()) {        
        int x = rs.getInt("row");
        int y = rs.getInt("col");
        int id = rs.getInt("playerID");
        counts[id - 1]++;
        // set board status
        board.setPiece(x, y, mapping[id - 1]);
      }
      rs.close();
      stmt.close();
    } catch (Exception e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }

    }
    
    int turn = 0;
    if (counts[0] == counts[1]) {
      turn = 1;
    } else if (counts[0] == counts[1] + 1) {
      turn = 2;
    } else {
      System.out.println("counts[0] and counts[1] relation wrong!");
    }
    
    // set turn
    board.setTurn(turn);
    
    // update status for win and isDraw
    board.update();
    
    System.out.println("recover completes");
  }
  
  /**.
   * @param sql SQL command.
   *        Execute command 
   *        using statement
   */
  private void execute(String sql) {
    Statement stmt = null;
    try {
      stmt = this.conn.createStatement();
      stmt.executeUpdate(sql);
      stmt.close();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
      //System.exit(0);
    }
  }
  
  /**.
   * @param stmt the statement to be update.
   * @return the result set
   *        Note that rs may by null
   */
  private void secureUpdate(PreparedStatement stmt) {
    // Using pareparedStatement to avoid security problems
    try {
      stmt.executeUpdate();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
  } 
  
  
  /**.
   * @param stmt the statement to be query.
   * @return the result set
   *        Note that rs may by null
   */
  private ResultSet secureQuery(PreparedStatement stmt) {
    // Using pareparedStatement to avoid security problems
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    return rs;
  }
  
  /** .
   * create database connection and store
   * in the private variable
   */
  private void createConnection() {
    try {
      Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection("jdbc:sqlite:jdbcDB.db");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      //System.exit(0);
      return;
    }
    System.out.println("Opened database successfully");    
  }
  
}
