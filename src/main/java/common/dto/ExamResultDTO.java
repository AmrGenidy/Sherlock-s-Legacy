package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExamResultDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private int score;
  private int totalQuestions;
  private String feedbackMessage;
  private String finalRank;
  private List<String> reviewableAnswersInfo;
  private String winningMessage;
  private boolean caseSolved;

  // No-arg constructor for Jackson
  public ExamResultDTO() {
  }

  @JsonCreator
  public ExamResultDTO(
          @JsonProperty("score") int score,
          @JsonProperty("totalQuestions") int totalQuestions,
          @JsonProperty("feedbackMessage") String feedbackMessage,
          @JsonProperty("finalRank") String finalRank,
          @JsonProperty("reviewableAnswersInfo") List<String> reviewableAnswersInfo,
          @JsonProperty("winningMessage") String winningMessage,
          @JsonProperty("caseSolved") boolean caseSolved) {
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.feedbackMessage = feedbackMessage;
    this.finalRank = finalRank;
    this.reviewableAnswersInfo =
            reviewableAnswersInfo != null ? new ArrayList<>(reviewableAnswersInfo) : new ArrayList<>();
    this.winningMessage = winningMessage;
    this.caseSolved = caseSolved;
  }

  // Legacy Constructor for backward compatibility if needed
  public ExamResultDTO(int score, int totalQuestions, String feedbackMessage, String finalRank, List<String> reviewableAnswersInfo) {
      this(score, totalQuestions, feedbackMessage, finalRank, reviewableAnswersInfo, null, false);
  }

  // --- GETTERS ---
  public int getScore() {
    return score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public String getFeedbackMessage() {
    return feedbackMessage;
  }

  public String getFinalRank() {
    return finalRank;
  }

  public List<String> getReviewableAnswersInfo() {
    return reviewableAnswersInfo;
  }

  public String getWinningMessage() {
      return winningMessage;
  }

  public boolean isCaseSolved() {
      return caseSolved;
  }

  // --- SETTERS for Jackson ---
  public void setScore(int score) {
    this.score = score;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public void setFeedbackMessage(String feedbackMessage) {
    this.feedbackMessage = feedbackMessage;
  }

  public void setFinalRank(String finalRank) {
    this.finalRank = finalRank;
  }

  public void setReviewableAnswersInfo(List<String> reviewableAnswersInfo) {
    this.reviewableAnswersInfo = reviewableAnswersInfo;
  }

  public void setWinningMessage(String winningMessage) {
      this.winningMessage = winningMessage;
  }

  public void setCaseSolved(boolean caseSolved) {
      this.caseSolved = caseSolved;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(feedbackMessage).append("\n");
    sb.append("Score: ").append(score).append("/").append(totalQuestions).append("\n");
    sb.append("Final Rank: ").append(finalRank);
    if (reviewableAnswersInfo != null && !reviewableAnswersInfo.isEmpty()) {
      sb.append("\n\n--- Review of Incorrect/Unanswered Questions ---");
      for (String reviewInfo : reviewableAnswersInfo) {
        sb.append("\n").append(reviewInfo);
      }
    }
    return sb.toString();
  }
}
