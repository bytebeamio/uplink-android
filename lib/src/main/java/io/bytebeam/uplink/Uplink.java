package io.bytebeam.uplink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import io.bytebeam.uplink.service.ActionSubscriber;
import io.bytebeam.uplink.service.UplinkService;
import io.bytebeam.uplink.service.UplinkTerminatedException;
import io.bytebeam.uplink.types.ActionResponse;
import io.bytebeam.uplink.types.UplinkAction;
import io.bytebeam.uplink.types.UplinkPayload;

import static io.bytebeam.uplink.service.UplinkService.*;

enum ServiceState {
    UNINITIALIZED,
    CONNECTED,
    CRASHED,
    FINISHED
}

public class Uplink implements ServiceConnection {
    private final Context context;
    private final ServiceReadyCallback serviceReadyCallback;
    private Messenger serviceHandle;
    private ServiceState state = ServiceState.UNINITIALIZED;

    public ServiceState getState() {
        return state;
    }

    /**
     * Spawns an instance of the uplink.
     *
     * @param context              Current application context
     * @param authConfig           authorization json configuration
     * @param uplinkConfig         uplink toml configuration
     * @param enableLogging        whether android logs should be reported as well
     * @param serviceReadyCallback callback that will be invoked when the service is ready to be used
     */
    public Uplink(
            Context context,
            String authConfig,
            String uplinkConfig,
            boolean enableLogging,
            ServiceReadyCallback serviceReadyCallback
    ) {
        this.context = context;
        this.serviceReadyCallback = serviceReadyCallback;
        Intent intent = new Intent(context, UplinkService.class);
        intent.putExtra(AUTH_CONFIG_KEY, authConfig);
        intent.putExtra(UPLINK_CONFIG_KEY, uplinkConfig);
        intent.putExtra(ENABLE_LOGGING_KEY, enableLogging);
        context.bindService(
                intent,
                this,
                Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND
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
        stateAssertion();
        callMethod(SEND_DATA, response.toPayload());
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
            state = ServiceState.FINISHED;
        } catch (UplinkTerminatedException e) {
            Log.w(TAG, "Uplink service terminated before dispose was called");
        }
    }

    /**
     * To be used for testing
     */
    public void crash() throws UplinkTerminatedException {
        stateAssertion();
        callMethod(CRASH, null);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        state = ServiceState.CONNECTED;
        serviceHandle = new Messenger(service);
        serviceReadyCallback.uplinkReady();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (state != ServiceState.FINISHED) {
            state = ServiceState.CRASHED;
        }
    }

    private void stateAssertion() throws UplinkTerminatedException {
        switch (state) {
            case UNINITIALIZED:
                throw new IllegalStateException("attempt to use service before initialization is complete");
            case CRASHED:
                throw new UplinkTerminatedException();
            case FINISHED:
                throw new IllegalStateException("attempt to use service after it was disposed");
        }
    }
}

