package io.bytebeam.uplinkconfigurator.service;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;
import io.bytebeam.uplink.common.Utils;
import io.bytebeam.uplinkconfigurator.R;

import java.util.ArrayList;
import java.util.List;

public class UplinkService extends Service {
    public static String TAG = "UplinkService";
    public static String AUTH_CONFIG_KEY = "authConfig";
    public static String UPLINK_CONFIG_KEY = "uplinkConfig";
    public static String ENABLE_LOGGING_KEY = "enableLogging";
    public static String DATA_KEY = "parcelData";

    // Incoming messenger events
    public static final int SEND_DATA = 0;
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
        boolean enableLogging = intent.getBooleanExtra(ENABLE_LOGGING_KEY, false);
        uplink = NativeApi.createUplink(
                Utils.getRawTextFile(getApplicationContext(), R.raw.auth_config),
                String.format(
                        "[persistence]\n" +
                                "path = \"%s/uplink\"\n" +
                                "\n" +
                                "[streams.battery_stream]\n" +
                                "topic = \"/tenants/{tenant_id}/devices/{device_id}/events/battery_level\"\n" +
                                "buf_size = 1\n",
                        getApplicationContext().getFilesDir().getAbsolutePath()
                ),
                true,
                this::uplinkSubscription
        );
        Log.d(TAG, "Uplink service created");
        return new Messenger(new Handler(this::handleMessage)).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "shutting down uplink service and process");
        subscribers.clear();
        NativeApi.destroyUplink(uplink);
        uplink = 0;
        // forcefully kill the service process to allow the cleanup of the native resources
        Runtime.getRuntime().exit(0);
        return false;
    }

    private boolean handleMessage(Message message) {
        if (uplink == 0) {
            Log.e(TAG, "messenger of an unbound service is being used, ignoring");
            return true;
        }
        switch (message.what) {
            case SEND_DATA:
                Bundle b = message.getData();
                b.setClassLoader(UplinkPayload.class.getClassLoader());
                UplinkPayload payload = b.getParcelable(DATA_KEY);
                Log.d(TAG, String.format("Submitting payload: %s", payload.toString()));
                NativeApi.sendData(uplink, payload);
                break;
            case SUBSCRIBE:
                Log.d(TAG, "adding a subscriber");
                subscribers.add(message.replyTo);
                break;
            case CRASH:
                Log.w(TAG, "crashing the uplink service");
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
        Log.d(TAG, String.format(TAG, "Received action: %s", uplinkAction.toString()));
        for (Messenger subscriber : subscribers) {
            Message m = new Message();
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