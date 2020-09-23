package models;

public class Message {

  private boolean moveValidity;

  private int code;

  private String message;

  /**.
   * @param valid if it is a valid movement
   * @param c the code, which is usually 100
   * @param m the message wants to show
   */
  public Message(boolean valid, int c, String m) {
    this.moveValidity = valid;
    this.code = c;
    this.message = m;
  }
}
