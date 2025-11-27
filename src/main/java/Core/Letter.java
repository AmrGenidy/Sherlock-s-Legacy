package Core;

public class Letter {
  private String invitation;
  private String caseDescription;

  public void setInvitation(String invitation) {
    this.invitation = invitation;
  }

  public void setCaseDescription(String caseDescription) {
    this.caseDescription = caseDescription;
  }

  public void displayInvitation() {
    System.out.println(invitation);
  }

  public void displayCaseDescription() {
    System.out.println(caseDescription);
  }
}
