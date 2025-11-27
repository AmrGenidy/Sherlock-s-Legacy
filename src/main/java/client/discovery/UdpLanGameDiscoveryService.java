package client.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.NetworkConstants;
import common.dto.LanDiscoveryPacket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdpLanGameDiscoveryService implements LanGameDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(UdpLanGameDiscoveryService.class);
    private final ConcurrentHashMap<String, DiscoveredGame> discoveredGames = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private Thread listenerThread;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void start() {
        if (running) return;
        running = true;
        discoveredGames.clear();
        listenerThread = new Thread(this::listenLoop, "LanDiscoveryListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        logger.info("LAN Discovery listener started.");
    }

    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        logger.info("LAN Discovery listener stopped.");
    }

    private void listenLoop() {
        try (DatagramSocket socket = new DatagramSocket(NetworkConstants.DISCOVERY_PORT)) {
            socket.setSoTimeout(2000); // Unblock every 2 seconds to check the running flag
            while (running) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);

                    LanDiscoveryPacket packetInfo = objectMapper.readValue(packet.getData(), 0, packet.getLength(), LanDiscoveryPacket.class);
                    logger.info("Successfully deserialized discovery packet from {}: Case='{}', Host='{}'", packet.getAddress().getHostAddress(), packetInfo.getCaseTitle(), packetInfo.getHostDisplayName());
                    logger.info("Discovered LAN game: title='{}', host='{}', public={}, joinCode={}",
                            packetInfo.getCaseTitle(), packetInfo.getHostDisplayName(),
                            packetInfo.isPublicGame(), packetInfo.getJoinCode());
                    String hostIp = packet.getAddress().getHostAddress();

                    DiscoveredGame game = new DiscoveredGame(
                            packetInfo.getCaseTitle(),
                            packetInfo.getHostDisplayName(),
                            packetInfo.isPublicGame(),
                            packetInfo.getJoinCode(),
                            hostIp,
                            packetInfo.getTcpPort(),
                            1, // For now, we assume 1 player is in the lobby
                            2,
                            packetInfo.getSessionId()
                    );

                    discoveredGames.put(game.getSessionId(), game);

                } catch (SocketTimeoutException e) {
                    // This is expected, just loop again to check the 'running' flag
                    continue;
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error receiving discovery packet", e);
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                logger.error("Failed to start LAN discovery listener on port {}", NetworkConstants.DISCOVERY_PORT, e);
            }
        }
    }

    @Override
    public List<DiscoveredGame> getCurrentGames() {
        return new ArrayList<>(discoveredGames.values());
    }

    @Override
    public void refreshAsync() {
        // Clearing the list allows it to be repopulated with fresh broadcasts.
        discoveredGames.clear();
        logger.info("Cleared discovered games list for refresh.");
    }

    public Optional<DiscoveredGame> findByCode(String joinCode) {
        return discoveredGames.values().stream()
                .filter(g -> joinCode.equalsIgnoreCase(g.getGameCode()))
                .findFirst();
    }
}
