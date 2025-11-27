package ui.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Utility class to generate placeholder images for rooms, suspects, and objects
 * when actual image files are not available.
 */
public class PlaceholderImageGenerator {

  /**
   * Creates a placeholder room image with a gradient background.
   */
  public static Image createRoomPlaceholder(String roomName, int width, int height) {
    WritableImage image = new WritableImage(width, height);
    PixelWriter writer = image.getPixelWriter();

    // Create a gradient from dark brown to dark blue (mystery/detective theme)
    Color topColor = Color.web("#2a1a0a");
    Color bottomColor = Color.web("#0a0a2a");

    for (int y = 0; y < height; y++) {
      double ratio = (double) y / height;
      Color lineColor = topColor.interpolate(bottomColor, ratio);
      
      for (int x = 0; x < width; x++) {
        // Add some texture variation
        double variation = (Math.sin(x * 0.01) + Math.cos(y * 0.01)) * 0.05;
        Color pixelColor = lineColor.deriveColor(0, 1, 1 + variation, 1);
        writer.setColor(x, y, pixelColor);
      }
    }

    return image;
  }

  /**
   * Creates a placeholder suspect silhouette image.
   */
  public static Image createSuspectPlaceholder(String suspectName, int size) {
    WritableImage image = new WritableImage(size, size);
    PixelWriter writer = image.getPixelWriter();

    // Background
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        writer.setColor(x, y, Color.TRANSPARENT);
      }
    }

    // Draw a simple silhouette (circle for head, rectangle for body)
    int centerX = size / 2;
    int centerY = size / 2;
    Color silhouetteColor = Color.web("#4a7ba7");

    // Head (circle)
    int headRadius = size / 4;
    int headCenterY = size / 3;
    drawCircle(writer, centerX, headCenterY, headRadius, silhouetteColor);

    // Body (rounded rectangle)
    int bodyTop = headCenterY + headRadius;
    int bodyHeight = size - bodyTop;
    int bodyWidth = size / 2;
    drawRectangle(writer, centerX - bodyWidth / 2, bodyTop, bodyWidth, bodyHeight, silhouetteColor);

    return image;
  }

  /**
   * Creates a placeholder object icon image.
   */
  public static Image createObjectPlaceholder(String objectName, int size) {
    WritableImage image = new WritableImage(size, size);
    PixelWriter writer = image.getPixelWriter();

    // Background
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        writer.setColor(x, y, Color.TRANSPARENT);
      }
    }

    // Draw a simple object representation (diamond shape)
    int centerX = size / 2;
    int centerY = size / 2;
    Color objectColor = Color.web("#8b7355");

    // Diamond/magnifying glass shape
    drawDiamond(writer, centerX, centerY, size / 3, objectColor);

    return image;
  }

  /**
   * Draws a filled circle.
   */
  private static void drawCircle(PixelWriter writer, int cx, int cy, int radius, Color color) {
    for (int y = cy - radius; y <= cy + radius; y++) {
      for (int x = cx - radius; x <= cx + radius; x++) {
        int dx = x - cx;
        int dy = y - cy;
        if (dx * dx + dy * dy <= radius * radius) {
          writer.setColor(x, y, color);
        }
      }
    }
  }

  /**
   * Draws a filled rectangle.
   */
  private static void drawRectangle(PixelWriter writer, int x, int y, int width, int height, Color color) {
    for (int py = y; py < y + height; py++) {
      for (int px = x; px < x + width; px++) {
        writer.setColor(px, py, color);
      }
    }
  }

  /**
   * Draws a diamond shape.
   */
  private static void drawDiamond(PixelWriter writer, int cx, int cy, int size, Color color) {
    for (int y = cy - size; y <= cy + size; y++) {
      for (int x = cx - size; x <= cx + size; x++) {
        int dx = Math.abs(x - cx);
        int dy = Math.abs(y - cy);
        if (dx + dy <= size) {
          writer.setColor(x, y, color);
        }
      }
    }
  }
}
