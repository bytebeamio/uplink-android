package io.bytebeam.uplink;

import io.bytebeam.uplink.common.ActionResponse;
import io.bytebeam.uplink.common.ActionSubscriber;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Communicates with a running uplink instance using TCP JSON protocol specified in
 * <a href="https://github.com/bmcpt/uplink/blob/main/docs/apps.md">uplink docs</a>
 * Creating an instance of this class will connect to uplink. `dispose` method must
 * be called when you're done using it.
 * Uplink instances are Thread-safe, so you can reuse them freely across multiple threads.
 */
public class Uplink {
    private final AtomicReference<UplinkConnectionState> state = new AtomicReference<>(UplinkConnectionState.UNINITIALIZED);
    private Socket client;
    private PrintWriter out;
    private final List<ActionSubscriber> subscribers = new ArrayList<>();

    /**
     * @param address The host+port at which uplink is listening for connections
     * @throws IOException will be thrown if the client was unable to connect (uplink not running/already connected to someone else)
     */
    public Uplink(ConnectionConfig address, ActionSubscriber actionSubscriber) throws IOException {
        if (actionSubscriber != null) {
            subscribers.add(actionSubscriber);
        }
        Thread init = new Thread(() -> initTask(address));
        init.start();
        try {
            init.join();
        } catch (InterruptedException e) {
        }
        if (state.get() == UplinkConnectionState.DISCONNECTED) {
            throw new IOException("uplink refused to connect");
        }
    }

    /**
     * Add a listener for incoming actions
     */
    public void subscribe(ActionSubscriber subscriber) {
        synchronized (subscribers) {
            subscribers.add(subscriber);
        }
    }

    /**
     * Send some data to uplink. Will throw an IOException if the client is not connected to
     * the uplink instance anymore
     */
    public void sendData(UplinkPayload payload) throws IOException {
        synchronized (out) {
            if (!connected()) {
                throw new IOException(String.format("not connected anymore, state = %s", state.get()));
            }
            out.write(payload.toJson());
            out.write('\n');
            out.flush();
        }
    }

    /**
     * Respond to an action. Will throw an IOException if the client is not connected to
     * the uplink instance anymore
     */
    public void respondToAction(ActionResponse response) throws IOException {
        sendData(response.toPayload());
    }

    /**
     * Checks whether the client in still connected
     */
    public boolean connected() {
        return state.get() == UplinkConnectionState.CONNECTED;
    }

    /**
     * Should be called when the user is done communicating with uplink
     * (e.g. in the `onDestroy` method of an activity).
     */
    public void dispose() {
        state.set(UplinkConnectionState.CLOSED);
        try {
            client.close();
        } catch (IOException e) {
        }
    }

    private void initTask(ConnectionConfig config) {
        try {
            client = new Socket(config.host, config.port);
            out = new PrintWriter(client.getOutputStream(), false);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            new Thread(() -> readerTask(in)).start();
            state.set(UplinkConnectionState.CONNECTED);
        } catch (IOException e) {
            state.set(UplinkConnectionState.DISCONNECTED);
        }
    }

    private void readerTask(BufferedReader in) {
        while (connected()) {
            String line;
            try {
                line = in.readLine();
                if (line == null) {
                    state.set(UplinkConnectionState.DISCONNECTED);
                    break;
                }
            } catch (IOException e) {
                state.set(UplinkConnectionState.DISCONNECTED);
                break;
            }
            try {
                JSONObject o = new JSONObject(line);
                UplinkAction action = new UplinkAction(
                        o.getString("action_id"),
                        o.getString("kind"),
                        o.getString("name"),
                        o.getString("payload")
                );
                synchronized (subscribers) {
                    for (ActionSubscriber subscriber : subscribers) {
                        subscriber.processAction(action);
                    }
                }
            } catch (JSONException e) {
                System.out.printf("received invalid json from uplink: \"%s\"\n", line);
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
