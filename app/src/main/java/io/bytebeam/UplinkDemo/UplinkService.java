package io.bytebeam.UplinkDemo;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import io.bytebeam.uplink.NativeApi;
import io.bytebeam.uplink.types.ActionResponse;
import io.bytebeam.uplink.types.UplinkAction;

import java.util.ArrayList;
import java.util.List;

public class UplinkService extends Service {
    public static String TAG = "UplinkService";
    public static String AUTH_CONFIG_KEY = "authConfig";
    public static String UPLINK_CONFIG_KEY = "uplinkConfig";
    public static String DATA_KEY = "parcelData";

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

    @Override
    public boolean onUnbind(Intent intent) {
        uplink = 0;
        subscribers.clear();
        return false;
    }

    private boolean handleMessage(Message message) {
        if (uplink == 0) {
            Log.e(TAG, "messenger of an unbound service is being used, ignoring");
            return true;
        }
        switch (message.what) {
            case SEND_DATA:
                NativeApi.sendData(uplink, message.getData().getParcelable(DATA_KEY));
                break;
            case RESPOND_TO_ACTION:
                NativeApi.respond(uplink, message.getData().getParcelable(DATA_KEY));
                break;
            case SUBSCRIBE:
                subscribers.add(message.replyTo);
                break;
            case CRASH:
                NativeApi.crash();
                break;
            default:
                throw new IllegalArgumentException();
        }
        return true;
    }

    private void uplinkSubscription(UplinkAction uplinkAction) {
        if (uplink == 0) {
            Log.e(TAG, "Action delivered to an unbound service, ignoring");
            return;
        }
        for (Messenger subscriber : subscribers) {
            Message m = new Message();
            m.what = 0;
            Bundle b = new Bundle();
            b.putParcelable(DATA_KEY, uplinkAction);
            m.setData(b);
            try {
                subscriber.send(m);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }
}