package Core;

public class GameObject {
  private String name;
  private String description;
  private String examine; // Detailed examination text
  private String deduce; // Deduction text
  private String spriteImage; // <-- ADD THIS
  private int x;              // <-- ADD THIS
  private int y; 

  // Constructor with all fields
  public GameObject(String name, String description, String examine, String deduce) {
    this.name = name;
    this.description = description;
    this.examine = examine;
    this.deduce = deduce;
}

  // Getters
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getExamine() {
    return examine;
  }

  public String getDeduce() {
    return deduce;
  }

  // Deduce method (no longer abstract)
  public String deduce() {
    return deduce;
  }

}
