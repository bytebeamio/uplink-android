package io.bytebeam.uplink.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;
import io.bytebeam.uplink.common.Utils;
import io.bytebeam.uplink.configurator.R;

import java.util.ArrayList;
import java.util.List;

import static io.bytebeam.uplink.common.Constants.*;

public class UplinkService extends Service {
    public static final String TAG = "UplinkService";
    public static final String KILL_COMMAND_TAG = "KILL_SERVICE";
    public static final String PREFS_NAME = "configuration";
    public static final String PREFS_AUTH_CONFIG_KEY = "auth_config";
    public static final String PREFS_AUTH_CONFIG_NAME_KEY = "auth_config_name";
    List<Messenger> subscribers = new ArrayList<>();
    long uplink = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getBooleanExtra(KILL_COMMAND_TAG, false)) {
            Log.d(TAG, "killing service");
            onUnbind(null);
            return null;
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String authConfig = prefs.getString(PREFS_AUTH_CONFIG_KEY, null);
        if (authConfig == null) {
            Log.d(TAG, "auth config not found, stopping service");
            onUnbind(null);
            return null;
        }

        if (uplink == 0) {
            uplink = NativeApi.createUplink(
                    authConfig,
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
            Log.d(TAG, "uplink native context initialized");
        }

        IBinder result = new Messenger(new Handler(Looper.myLooper(), this::handleMessage)).getBinder();

        return result;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "shutting down uplink service and process");
        subscribers.clear();
        if (uplink != 0) {
            NativeApi.destroyUplink(uplink);
            uplink = 0;
        }
        // forcefully kill the service process to allow the cleanup of the native resources
        System.exit(0);
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