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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Communicates with a running uplink instance using TCP JSON protocol specified in
 * <a href="https://github.com/bmcpt/uplink/blob/main/docs/apps.md">uplink docs</a>
 * Creating an instance of this class will connect to uplink. `dispose` method must
 * be called when you're done using it.
 * Uplink instances are Thread-safe, so you can reuse them freely across multiple threads.
 */
public class Uplink {
    private static final Gson gson = new Gson();
    public static final String TAG = "UPLINK_SDK";
    private final AtomicReference<UplinkConnectionState> state = new AtomicReference<>(UplinkConnectionState.UNINITIALIZED);
    private Socket client;
    private PrintWriter out;
    private final List<ActionSubscriber> subscribers = new ArrayList<>();

    /**
     * @param address The host+port at which uplink is listening for connections
     * @throws IOException will be thrown if the client was unable to connect (uplink not running/already connected to someone else)
     */
    public Uplink(ConnectionConfig address) throws IOException {
        Thread init = new Thread(() -> initTask(address));
        init.start();
        try {
            init.join();
        } catch (InterruptedException e) {}
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
        } catch (IOException e) {}
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
