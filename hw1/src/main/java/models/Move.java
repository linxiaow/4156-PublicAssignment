package models;

public class Move {

  private Player player;

  private int moveX;

  private int moveY;
  
  /**.
   * @param p the player who makes the movement
   * @param x the x coordinate to place piece
   * @param y the y coordinate to place the piece
   */
  public Move(Player p, int x, int y) {
    this.player = p;
    this.moveX = x;
    this.moveY = y;
  }
  
  /** get the x coordinate.*/
  public int getX() {
    return moveX;
  }
  
  /** get the y coordinate.*/
  public int getY() {
    return moveY;
  }
  
  /** get the player ID for the movement.*/
  public int getPlayerID() {
    return player.getID();
  }

  /** get the player piece type for this movement.*/
  public char getPlayerType() {
    return player.getType();
  }
}
