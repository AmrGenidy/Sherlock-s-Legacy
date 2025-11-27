package Core;

import java.util.*;

public class DoctorWatson extends MovableCharacter {
  private List<String> hints;
  private List<String> remainingHints;

  // Inherits 'random' from MovableCharacter

  public DoctorWatson(List<String> hints) {
    // Ensure hints list is not null
    this.hints = (hints != null) ? new ArrayList<>(hints) : new ArrayList<>();
    // Initialize remainingHints based on the potentially empty hints list
    this.remainingHints = new ArrayList<>(this.hints);
  }

  /**
   * Provides a random hint from the available list, removing it until reset. Returns the hint text
   * without printing it.
   *
   * @return A hint String, or a default message if no hints are available.
   */
  public String provideHint() {
    // Check if hints were empty to begin with or if remainingHints is empty
    if (this.hints.isEmpty()) {
      return "I'm afraid I have no specific insights for this case.";
    }

    if (this.remainingHints.isEmpty()) {
      // Reset the remaining hints if all have been used
      this.remainingHints.addAll(this.hints);
      // Check again in case the original list was somehow cleared elsewhere (defensive)
      if (this.remainingHints.isEmpty()) {
        return "I seem to be out of thoughts for the moment, perhaps ask again later.";
      }
    }

    return selectAndRemoveHint();
  }

  // Helper method to select and remove a hint
  private String selectAndRemoveHint() {
    if (remainingHints
        .isEmpty()) { // Should not happen if called after checks in provideHint, but safety first
      return "My mind is blank at the moment.";
    }
    int index = random.nextInt(remainingHints.size());
    // Return the hint text directly
    return remainingHints.remove(index);
  }
}
