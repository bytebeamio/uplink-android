package io.bytebeam.uplink.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import io.bytebeam.uplink.common.UplinkAction;
import io.bytebeam.uplink.common.UplinkPayload;

import java.util.ArrayList;
import java.util.List;

import static io.bytebeam.uplink.common.Constants.*;

public class UplinkService extends Service {
    public static final String TAG = "UplinkService";
    List<Messenger> subscribers = new ArrayList<>();
    long uplink = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "creating binder");

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String authConfig = prefs.getString(PREFS_AUTH_CONFIG_KEY, null);
        if (authConfig == null) {
            Log.d(TAG, "auth config not found");
            new Handler(Looper.myLooper()).postDelayed(() -> {onUnbind(null);}, 200);
            return null;
        }

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

        Log.d(TAG, "returning messenger");
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
            case STOP_SERVICE:
                String sudoPass = getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREFS_SERVICE_SUDO_PASS_KEY, null);
                if (sudoPass == null) {
                    Log.e(TAG, "Illegal service state error, privileged operation key not found");
                } else {
                    String pass = message.getData().getString(DATA_KEY, "");
                    if (!pass.equals(sudoPass)) {
                        Log.e(TAG, String.format("privileged operation key mismatch: %s != %s", pass, sudoPass));
                    } else {
                        Log.d(TAG, "stopping service");
                        new Handler(Looper.myLooper()).postDelayed(() -> {onUnbind(null);}, 200);
                    }
                }
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