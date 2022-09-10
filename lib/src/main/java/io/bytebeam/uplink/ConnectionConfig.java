package io.bytebeam.uplink;

/**
 * Address at which uplink is listening for TCP connections
 */
public class ConnectionConfig {
    String host = "localhost";
    int port = 5555;

    public ConnectionConfig withHost(String host) {
        this.host = host;
        return this;
    }

    public ConnectionConfig withPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(String.format("port %d is out of range", port));
        }
        this.port = port;
        return this;
    }
}