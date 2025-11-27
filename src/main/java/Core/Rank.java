package Core;

import java.io.Serializable;
import java.util.Objects;

public class Rank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String rankName;
    private final int maxDeductions;
    private final String description;
    private final boolean isDefault;

    public Rank(String rankName, int maxDeductions, String description, boolean isDefault) {
        this.rankName = rankName;
        this.maxDeductions = maxDeductions;
        this.description = description;
        this.isDefault = isDefault;
    }

    // Getters
    public String getRankName() { return rankName; }
    public int getMaxDeductions() { return maxDeductions; }
    public String getDescription() { return description; }
    public boolean isDefault() { return isDefault; }

    @Override
    public String toString() {
        return rankName; // For easy display
    }

    // Optional: equals and hashCode for comparisons
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rank rank = (Rank) o;
        return Objects.equals(rankName, rank.rankName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rankName);
    }
}