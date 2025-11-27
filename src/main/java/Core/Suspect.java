package Core;

public class Suspect extends MovableCharacter {
  private String name;
  private String statement;
  private String clue;

  public Suspect(String name, String statement, String clue) {
    this.name = name;
    this.statement = statement;
    this.clue = clue;
}

  public String getName() {
    return name;
  }

  public String getStatement() {
    return statement;
  }

  public String getClue() {
    return clue;
  }

}
