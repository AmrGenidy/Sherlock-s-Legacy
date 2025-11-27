package common.dto.pinboard;

import java.io.Serializable;

public class PinboardLinkDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String startItemId;
    private String endItemId;
    private String color;

    public PinboardLinkDTO() {}

    public PinboardLinkDTO(String startItemId, String endItemId, String color) {
        this.startItemId = startItemId;
        this.endItemId = endItemId;
        this.color = color;
    }

    public String getStartItemId() { return startItemId; }
    public void setStartItemId(String startItemId) { this.startItemId = startItemId; }
    public String getEndItemId() { return endItemId; }
    public void setEndItemId(String endItemId) { this.endItemId = endItemId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
