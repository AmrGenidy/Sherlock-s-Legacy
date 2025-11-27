package common;

public class NetworkConstants {
  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8888;
  public static final int BUFFER_SIZE = 8192; // For network ByteBuffers (8KB)
  public static final int MAX_PLAYERS_PER_GAME = 2;

  public static final long SELECTOR_TIMEOUT = 1000; // 1 second

  // --- LAN Discovery ---
  public static final int DISCOVERY_PORT = 51515;
  public static final int DISCOVERY_INTERVAL_MS = 1000;

  // Private constructor to prevent instantiation
  private NetworkConstants() {}
}
