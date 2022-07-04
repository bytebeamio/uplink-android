package io.bytebeam.uplink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import io.bytebeam.uplink.common.*;
import io.bytebeam.uplink.common.exceptions.ConfiguratorUnavailableException;
import io.bytebeam.uplink.common.exceptions.UplinkNotConfiguredException;
import io.bytebeam.uplink.common.exceptions.UplinkTerminatedException;

import static io.bytebeam.uplink.common.Constants.*;

public class Uplink implements ServiceConnection {
    private static final String TAG = "UplinkMessenger";
    private final Context context;
    private final UplinkStateCallback serviceStateCallback;
    private Messenger serviceHandle;
    private UplinkServiceState state = UplinkServiceState.UNINITIALIZED;

    public UplinkServiceState getState() {
        return state;
    }

    /**
     * Spawns an instance of the uplink.
     *
     * @param context              Current application context
     * @param uplinkReadyCallback callback that will be invoked when the service is ready to be used
     */
    public Uplink(
            Context context,
            UplinkStateCallback uplinkReadyCallback
    ) throws ConfiguratorUnavailableException {
        if (!configuratorAvailable(context)) {
            throw new ConfiguratorUnavailableException();
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
    public void subscribe(ActionSubscriber subscriber) throws UplinkTerminatedException {
        stateAssertion();
        Messenger messenger = new Messenger(
                new Handler(
                        Looper.getMainLooper(),
                        (message) -> {
                            Bundle b = message.getData();
                            b.setClassLoader(UplinkAction.class.getClassLoader());
                            subscriber.processAction(b.getParcelable(DATA_KEY));
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

    private void callMethod(int method, Parcelable arg) {
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
     * To be called when the client is done using the service
     * The uplink service will kill the process it was running in
     * The instance must not be used after this method is called
     */
    public void dispose() {
        try {
            stateAssertion();
            context.unbindService(this);
            state = UplinkServiceState.FINISHED;
        } catch (UplinkTerminatedException e) {
            Log.w(TAG, "Uplink service terminated before dispose was called");
        } catch (UplinkNotConfiguredException e) {}
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        state = UplinkServiceState.CONNECTED;
        serviceHandle = new Messenger(service);
        serviceStateCallback.onUplinkReady();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (state != UplinkServiceState.FINISHED) {
            state = UplinkServiceState.CRASHED;
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        Log.e(TAG, "uplink binding died");
    }

    @Override
    public void onNullBinding(ComponentName name) {
        Log.i(TAG, "uplink service not ready");
        state = UplinkServiceState.NOT_READY;
        serviceStateCallback.onServiceNotConfigured();
        context.unbindService(this);
    }

    private void stateAssertion() throws UplinkTerminatedException {
        switch (state) {
            case NOT_READY:
                throw new UplinkNotConfiguredException();
            case CRASHED:
                throw new UplinkTerminatedException();
            case UNINITIALIZED:
                throw new IllegalStateException("attempt to use service before initialization is complete");
            case FINISHED:
                throw new IllegalStateException("attempt to use service after it was disposed");
        }
    }

    public static boolean configuratorAvailable(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID));
        return context.getPackageManager().queryIntentServices(intent, 0).size() != 0;
    }
}

