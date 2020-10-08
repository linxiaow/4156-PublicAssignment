package unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import models.Move;
import models.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.Database;

public class DatabaseTest {
  
  private Connection conn = null;
  private String boardTable = "board";
  private String playerTable = "player";
  
  /**.
   * before each, clean the tables
   */
  @BeforeEach
  public void cleanAndStart() {
    System.out.println("dropping the tables..."); 
    final String dropBoard = String.format("DROP TABLE IF EXISTS %s;", 
        this.boardTable);
    final String dropPlayer = String.format("DROP TABLE IF EXISTS %s;", 
        this.playerTable);   
    this.execute(dropBoard); 
    this.execute(dropPlayer);
    System.out.println("make connections..."); 
    this.testCreateConnection();
    this.print();
  }
  
  
  /**.
   * test if creating table is successful
   */
  @Test
  public void testCreateTable() {
    Database db = new Database();
    db.createTable(); // create two tables
    
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      final String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'";
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      int tableCount = rs.getInt(1);
      assertEquals(2, tableCount);
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
      assertEquals(true, false);
    }    
  }

  /**.
   * test if dropping table is successful
   */
  @Test
  public void testDropTable() {
    Database db = new Database();
    db.createTable(); 
    db.dropTable(); // drop two tables
    
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      final String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'";
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      int tableCount = rs.getInt(1);
      assertEquals(0, tableCount);
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
      assertEquals(true, false);
    }    
  }
  
  /**.
   * test if move works
   */
  @Test
  public void testMove() {
    System.out.println("tesing movement...");
    Player player = new Player('X', 1);
    Move move = new Move(player, 0, 2);
    Database db = new Database();
    db.createTable();
    db.addMove(move); // add move
    
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      final String sql = String.format(
          "SELECT * FROM %s;", this.boardTable);
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      while (rs.next()) {
        int x = rs.getInt("row");
        int y = rs.getInt("col");
        int id = rs.getInt("playerID");
        assertEquals(x, 0);
        assertEquals(y, 2);    
        assertEquals(id, 1);    
      }
      
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
      assertEquals(true, false);
    }  
  }

  /**.
   * test if add player works
   */
  @Test
  public void testAddPlayer() {
    Player player = new Player('X', 1);
    Database db = new Database();
    db.createTable();
    db.addPlayers(player); // add move
    
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      final String sql = String.format(
          "SELECT * FROM %s;", this.playerTable);
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      while (rs.next()) {
        int id = rs.getInt("playerID");
        char type = rs.getString("type").charAt(0);
        assertEquals(id, 1);
        assertEquals(type, 'X');   
      }
      
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
      assertEquals(true, false);
    }      
  }
  
  /**.
   * main in DB is used for droping table
   */
  @Test
  public void testMain() {
    Database db = new Database();
    db.createTable(); 
    Database.main(null); // drop two tables
    
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      final String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'";
      stmt = this.conn.prepareStatement(sql);
      rs = this.secureQuery(stmt);
      
      int tableCount = rs.getInt(1);
      assertEquals(0, tableCount);
      rs.close();
      stmt.close();
    } catch (Exception e) {
      // may have drop issue
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
      assertEquals(true, false);
    }    
  }
  
  /**
   * print information
   */
  private void print() {
    if (this.conn != null) {
      System.out.println("connection has been established");
    }
    
    System.out.println("player table is named " + this.playerTable);
    System.out.println("board table is named " + this.boardTable);
  }
  
  /**.
   * connection
   */
  private void testCreateConnection() {
    System.out.println("create connection...");
    try {
      Class.forName("org.sqlite.JDBC");
      this.conn = DriverManager.getConnection("jdbc:sqlite:jdbcDB.db");
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      //System.exit(0);
      return;
    }    
  }
  
  /**.
   * @param sql string as sql command
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
}
