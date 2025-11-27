package common.dto;

import java.io.Serializable;

public class ExamAnswerSelectedDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int questionIndex;
    private final String slotId;
    private final String choiceId;

    public ExamAnswerSelectedDTO(int questionIndex, String slotId, String choiceId) {
        this.questionIndex = questionIndex;
        this.slotId = slotId;
        this.choiceId = choiceId;
    }

    public int getQuestionIndex() {
        return questionIndex;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getChoiceId() {
        return choiceId;
    }
}
