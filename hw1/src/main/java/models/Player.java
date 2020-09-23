package models;

public class Player {

  private char type;

  private int id;
  
  
  /** class constructor.
   * @param ptype X or O based on player selection
   * @param pid player ID
   */
  public Player(char ptype, int pid) {
    this.type = ptype;
    this.id = pid;
  }
  
  /**.
   * @return user piece selection
   */
  public char getType() {
    return this.type;
  }
  
  
  /**.
   * @return user ID
   */
  public int getID() {
    return this.id;
  }
}
