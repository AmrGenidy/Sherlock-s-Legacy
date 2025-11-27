package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class WatsonHintResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String message;
    private final boolean isActualHint;

    @JsonCreator
    public WatsonHintResponseDTO(
            @JsonProperty("message") String message,
            @JsonProperty("actualHint") boolean isActualHint) {
        this.message = message;
        this.isActualHint = isActualHint;
    }

    public String getMessage() {
        return message;
    }

    public boolean isActualHint() {
        return isActualHint;
    }
}