package io.bytebeam.uplink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import io.bytebeam.uplink.common.*;
import io.bytebeam.uplink.common.exceptions.ConfiguratorNotInstalledException;
import io.bytebeam.uplink.common.exceptions.UplinkServiceNotRunningException;
import io.bytebeam.uplink.common.exceptions.UplinkTerminatedException;

import java.util.List;

import static io.bytebeam.uplink.common.Constants.*;

public class Uplink implements ServiceConnection {
    private static final String TAG = "UplinkMessenger";
    private final Context context;
    private final UplinkStateCallback serviceStateCallback;
    private Messenger serviceHandle;
    private UplinkServiceState state = UplinkServiceState.UNINITIALIZED;

    public synchronized UplinkServiceState getState() {
        return state;
    }

    /**
     * Connects to the uplink service.
     *
     * @param context              Current application context
     * @param uplinkReadyCallback callback that will be invoked when the service is ready to be used
     */
    public Uplink(
            Context context,
            UplinkStateCallback uplinkReadyCallback
    ) throws ConfiguratorNotInstalledException, UplinkServiceNotRunningException {
        if (!configuratorAvailable(context)) {
            throw new ConfiguratorNotInstalledException();
        }
        if (!serviceRunning(context)) {
            throw new UplinkServiceNotRunningException();
        }
        this.context = context;
        this.serviceStateCallback = uplinkReadyCallback;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID));
        context.bindService(
                intent,
                this,
                Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND
        );
    }

    /**
     * Adds a subscriber that will be invoked when a new action is received.
     *
     * @throws UplinkTerminatedException if the uplink service has terminated for some reason
     */
    public synchronized void subscribe(ActionSubscriber subscriber) throws UplinkTerminatedException {
        stateAssertion();
        Messenger messenger = new Messenger(
                new Handler(
                        Looper.getMainLooper(),
                        (message) -> {
                            Bundle b = message.getData();
                            b.setClassLoader(UplinkAction.class.getClassLoader());
                            UplinkAction action = b.getParcelable(DATA_KEY);
                            if (getState() == UplinkServiceState.CONNECTED) {
                                subscriber.processAction(action);
                            } else {
                                Log.w(TAG, String.format("disconnected client recieved action: %s", action.toString()));
                            }
                            return true;
                        }
                )
        );

        Message call = new Message();
        call.what = SUBSCRIBE;
        call.replyTo = messenger;
        try {
            serviceHandle.send(call);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void callMethod(int method, Parcelable arg) {
        Message call = new Message();
        call.what = method;
        Bundle b = new Bundle();
        b.putParcelable(DATA_KEY, arg);
        call.setData(b);
        try {
            serviceHandle.send(call);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a payload to the uplink backend
     *
     * @throws UplinkTerminatedException if the uplink service has terminated for some reason
     */
    public void sendData(UplinkPayload payload) throws UplinkTerminatedException {
        stateAssertion();
        callMethod(SEND_DATA, payload);
    }

    /**
     * Responds to an action that was received from the uplink backend
     *
     * @throws UplinkTerminatedException if the uplink service has terminated for some reason
     */
    public void respondToAction(ActionResponse response) throws UplinkTerminatedException {
        sendData(response.toPayload());
    }

    /**
     * To be called when the client is done using the uplink service.
     */
    public synchronized void dispose() {
        state = UplinkServiceState.DISPOSED;
        context.unbindService(this);
    }

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        state = UplinkServiceState.CONNECTED;
        serviceHandle = new Messenger(service);
        serviceStateCallback.onUplinkReady();
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        if (getState() != UplinkServiceState.DISPOSED) {
            state = UplinkServiceState.SERVICE_STOPPED;
        }
        context.unbindService(this);
    }

    @Override
    public synchronized void onBindingDied(ComponentName name) {
        Log.e(TAG, "uplink binding died");
        state = UplinkServiceState.SERVICE_STOPPED;
        context.unbindService(this);
    }

    @Override
    public synchronized void onNullBinding(ComponentName name) {
        Log.i(TAG, "uplink service not ready");
        state = UplinkServiceState.SERVICE_STOPPED;
        context.unbindService(this);
    }

    private void stateAssertion() throws UplinkTerminatedException {
        switch (getState()) {
            case SERVICE_STOPPED:
                throw new UplinkTerminatedException();
            case UNINITIALIZED:
                throw new IllegalStateException("attempt to use service before initialization is complete");
            case DISPOSED:
                throw new IllegalStateException("attempt to use service after it was disposed");
        }
    }

    public static boolean configuratorAvailable(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID));
        List<ResolveInfo> services = context.getPackageManager().queryIntentServices(intent, 0);
        Log.d(TAG, String.format("Available services: %s", services.toString()));
        return services.size() != 0;
    }

    public static boolean serviceRunning(Context context) {
        try (Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://io.bytebeam.uplink.servicestate"),
                new String[]{},
                null,
                new String[]{},
                null
        )) {
            return cursor != null;
        }
    }
}

