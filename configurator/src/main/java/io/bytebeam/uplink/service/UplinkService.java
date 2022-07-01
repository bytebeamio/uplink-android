package io.bytebeam.uplink.service;

import android.app.Service;
import android.content.Intent;
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
    public static String TAG = "UplinkService";
    List<Messenger> subscribers = new ArrayList<>();
    long uplink = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
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
        return new Messenger(new Handler(Looper.myLooper(), this::handleMessage)).getBinder();
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