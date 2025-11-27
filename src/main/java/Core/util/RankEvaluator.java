package Core.util;

import Core.Detective;
import Core.Rank;
import JsonDTO.CaseFile;
import java.util.Comparator;
import java.util.List;
import JsonDTO.CaseData;

public class RankEvaluator {


    public static Rank evaluate(int sessionDeduceCount, CaseData caseFile) {
    if (caseFile == null || caseFile.getRankingTiers() == null || caseFile.getRankingTiers().isEmpty()) {
        // Return a sensible default if ranking data is missing from the case file
        return new Rank("Investigator", 999, "The case was solved.", true);
    }

    List<CaseFile.RankTierData> tiers = caseFile.getRankingTiers();

    // Sort tiers by maxDeductions, ascending, to ensure we find the tightest (best) fit first
    tiers.sort(Comparator.comparingInt(CaseFile.RankTierData::getMaxDeductions));

    Rank defaultRank = null;

    // Find the first tier the player qualifies for
    for (CaseFile.RankTierData tier : tiers) {
        if (tier.isDefaultRank()) {
            // Store the default rank but don't return it yet, as a better rank might match
            defaultRank = new Rank(tier.getRankName(), Integer.MAX_VALUE, tier.getDescription(), true);
        } else if (sessionDeduceCount <= tier.getMaxDeductions()) {
            // This is the best rank the team has achieved.
            return new Rank(tier.getRankName(), tier.getMaxDeductions(), tier.getDescription(), false);
        }
    }
    
    // If the team's deduce count was too high for any specific tier, return the default rank.
    // If no default was specified, create a generic fallback.
    return defaultRank != null ? defaultRank : new Rank("Investigator", 999, "The case was solved.", true);
}
}