package io.bytebeam.uplink;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.bytebeam.uplink.common.ActionResponse;
import io.bytebeam.uplink.common.ActionSubscriber;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Uplink {
    private static final Gson gson = new Gson();
    public static final String TAG = "UPLINK_SDK";
    private final Socket client;
    private final PrintWriter out;
    private final List<ActionSubscriber> subscribers = new ArrayList<>();

    /**
     * @param config
     * @throws IOException if an error occurs when establishing connections
     */
    public Uplink(ConnectionConfig config) throws IOException {
        this.client = new Socket(config.host, config.port);
        out = new PrintWriter(client.getOutputStream(), false);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        new Thread(() -> readerTask(in)).start();
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

    private void readerTask(BufferedReader in) {
        while (true) {
            String line;
            try {
                line = in.readLine();
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
}
