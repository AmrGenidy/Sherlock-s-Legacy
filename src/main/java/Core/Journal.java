package Core; // Assuming 'core' is the package name

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Using a generic type E for entries. This could be String for SP, or a DTO for MP.
public class Journal<E> implements Serializable {
  private static final long serialVersionUID = 1L; // For Serializable

  private List<E> entries;

  public Journal() {
    this.entries = new ArrayList<>();
  }

  /**
   * Adds an entry to the journal if it doesn't already exist. The definition of "exists" depends on
   * the .equals() method of type E. For JournalEntryDTO, .equals() should probably compare text,
   * contributor, and timestamp.
   *
   * @param entry The entry to add.
   * @return true if the entry was added, false if it already existed.
   */
  public boolean addEntry(E entry) {
    if (entry == null) {
      return false; // Or throw IllegalArgumentException
    }
    if (!entries.contains(entry)) {
      entries.add(entry);
      return true;
    }
    return false;
  }

  /**
   * Retrieves all entries from the journal.
   *
   * @return An unmodifiable list of entries.
   */
  public List<E> getEntries() {
    return Collections.unmodifiableList(entries); // Return an unmodifiable view
  }

  /** Clears all entries from the journal. */
  public void clearEntries() {
    entries.clear();
  }

  /**
   * Checks if the journal is empty.
   *
   * @return true if the journal has no entries, false otherwise.
   */
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  /**
   * Returns the number of entries in the journal.
   *
   * @return The count of journal entries.
   */
  public int getEntryCount() {
    return entries.size();
  }
}
