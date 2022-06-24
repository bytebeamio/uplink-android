package io.bytebeam.UplinkDemo;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import androidx.annotation.Nullable;
import io.bytebeam.uplink.NativeApi;
import io.bytebeam.uplink.types.UplinkAction;

import java.util.ArrayList;
import java.util.List;

public class UplinkService extends Service {
    public static String AUTH_CONFIG_KEY = "authConfig";
    public static String UPLINK_CONFIG_KEY = "uplinkConfig";
    public static String ACTION_KEY = "uplinkAction";

    // Incoming messenger events
    public static final int SEND_DATA = 0;
    public static final int RESPOND_TO_ACTION = 1;
    public static final int SUBSCRIBE = 2;
    /// for testing, will trigger a segmentation fault
    public static final int CRASH = 3;

    List<Messenger> subscribers = new ArrayList<>();
    long uplink = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        String authString = intent.getStringExtra(AUTH_CONFIG_KEY);
        String configString = intent.getStringExtra(UPLINK_CONFIG_KEY);
        uplink = NativeApi.createUplink(
                authString,
                configString,
                this::uplinkSubscription
        );
        return new Messenger(new Handler(this::handleMessage)).getBinder();
    }

    private boolean handleMessage(Message message) {
        switch (message.what) {
            case SEND_DATA:
                break;
            case RESPOND_TO_ACTION:
                break;
            case SUBSCRIBE:
                subscribers.add(message.replyTo);
                break;
            case CRASH:
                break;
            default:
                throw new IllegalArgumentException();
        }
        return false;
    }

    private void uplinkSubscription(UplinkAction uplinkAction) {
        for (Messenger subscriber : subscribers) {
            Message m = new Message();
            m.what = 0;
            Bundle b = new Bundle();
            b.putParcelable(ACTION_KEY, uplinkAction);
            m.setData(b);
            try {
                subscriber.send(m);
            } catch (RemoteException e) {}
        }
    }
}