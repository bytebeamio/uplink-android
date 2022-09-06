package io.bytebeam.uplink;

public class ConnectionConfig {
    String host = "localhost";
    int port = 5555;

    ConnectionConfig withHost(String host) {
        this.host = host;
        return this;
    }

    ConnectionConfig withPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(String.format("port %d is out of range", port));
        }
        this.port = port;
        return this;
    }
}