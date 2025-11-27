package ui.util;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Custom OutputStream that redirects output to a JavaFX TextArea.
 * Ensures UI updates happen on the JavaFX Application Thread.
 */
public class TextAreaOutputStream extends OutputStream {
  private final TextArea textArea;
  private final StringBuilder buffer = new StringBuilder();
  private GameOutputParser parser;

  public TextAreaOutputStream(TextArea textArea) {
    this.textArea = textArea;
  }

  /**
   * Sets the output parser for parsing game messages.
   */
  public void setParser(GameOutputParser parser) {
    this.parser = parser;
  }

  @Override
  public void write(int b) {
    // Convert byte to char and append to buffer
    buffer.append((char) b);

    // If we encounter a newline, flush the buffer to the TextArea
    if (b == '\n') {
      flush();
    }
  }

  @Override
  public void write(byte[] b, int off, int len) {
    String text = new String(b, off, len, StandardCharsets.UTF_8);
    buffer.append(text);

    // If the text contains a newline, flush
    if (text.contains("\n")) {
      flush();
    }
  }

  @Override
  public void flush() {
    if (buffer.length() > 0) {
      final String text = buffer.toString();
      buffer.setLength(0); // Clear buffer

      // Parse the text for game events if parser is set
      if (parser != null && text.contains("\n")) {
        String[] lines = text.split("\n");
        for (String line : lines) {
          parser.parseLine(line);
        }
      }

      // Update UI on JavaFX Application Thread
      Platform.runLater(() -> {
        textArea.appendText(text);
        // By nesting Platform.runLater, we schedule the scroll to happen
        // on a subsequent pulse of the JavaFX application thread. This gives
        // the layout-engine time to recalculate the view bounds after a text change,
        // ensuring the scroll goes to the correct final position.
        Platform.runLater(() -> textArea.setScrollTop(Double.MAX_VALUE));
      });
    }
  }
}
