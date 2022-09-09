package io.bytebeam.uplink;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.bytebeam.uplink.common.ActionResponse;
import io.bytebeam.uplink.common.ActionSubscriber;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Uplink {
    private static final Gson gson = new Gson();
    public static final String TAG = "UPLINK_SDK";
    private UplinkConnectionState state = UplinkConnectionState.UNINITIALIZED;
    private Socket client;
    private PrintWriter out;
    private final List<ActionSubscriber> subscribers = new ArrayList<>();

    /**
     * @param config
     */
    public Uplink(ConnectionConfig config) {
        Thread init = new Thread(() -> initTask(config));
        init.start();
        try {
            init.join();
        } catch (InterruptedException e) {}
    }

    public void subscribe(ActionSubscriber subscriber) {
        synchronized (subscribers) {
            subscribers.add(subscriber);
        }
    }

    public void sendData(UplinkPayload payload) {
        synchronized (out) {
            out.write(payload.toJson());
            out.write('\n');
            out.flush();
        }
    }

    public void respondToAction(ActionResponse response) {
        sendData(response.toPayload());
    }

    public boolean connected() {
        return state == UplinkConnectionState.CONNECTED;
    }

    public void dispose() {
        state = UplinkConnectionState.CLOSED;
        try {
            client.close();
        } catch (IOException e) {}
    }

    private void initTask(ConnectionConfig config) {
        try {
            client = new Socket(config.host, config.port);
            out = new PrintWriter(client.getOutputStream(), false);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            new Thread(() -> readerTask(in)).start();
            state = UplinkConnectionState.CONNECTED;
        } catch (IOException e) {
            state = UplinkConnectionState.DISCONNECTED;
        }
    }

    private void readerTask(BufferedReader in) {
        while (connected()) {
            String line;
            try {
                line = in.readLine();
                Log.e(TAG, line);
            } catch (IOException e) {
                break;
            }
            try {
                UplinkAction action = gson.fromJson(line, UplinkAction.class);
                synchronized (subscribers) {
                    for (ActionSubscriber subscriber : subscribers) {
                        subscriber.processAction(action);
                    }
                }
            } catch (JsonSyntaxException e) {
                Log.e(TAG, String.format("received invalid json from uplink: \"%s\"", line));
            }
        }
    }

    public enum UplinkConnectionState {
        UNINITIALIZED,
        CONNECTED,
        CLOSED,
        DISCONNECTED
    }
}
