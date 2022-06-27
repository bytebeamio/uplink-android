package io.bytebeam.uplink;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import io.bytebeam.uplink.types.ActionResponse;
import io.bytebeam.uplink.types.UplinkPayload;

import static io.bytebeam.uplink.UplinkService.*;

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

    public Uplink(
            Context context,
            String authConfig,
            String uplinkConfig,
            ServiceReadyCallback serviceReadyCallback
    ) {
        this.context = context;
        this.serviceReadyCallback = serviceReadyCallback;
        Intent intent = new Intent(context, UplinkService.class);
        intent.putExtra(AUTH_CONFIG_KEY, authConfig);
        intent.putExtra(UPLINK_CONFIG_KEY, uplinkConfig);
        Log.e(TAG, "triggering bind");
        context.bindService(
                intent,
                this,
                Context.BIND_AUTO_CREATE
        );
    }

    public void subscribe(ActionSubscriber subscriber) throws UplinkTerminatedException {
        stateAssertion();
        Messenger messenger = new Messenger(
                new Handler(
                        Looper.getMainLooper(),
                        (message) -> {
                            subscriber.processAction(message.getData().getParcelable(DATA_KEY));
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

    public void callMethod(int method, Parcelable arg) {
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

    public void sendData(UplinkPayload payload) throws UplinkTerminatedException {
        stateAssertion();
        callMethod(SEND_DATA, payload);
    }

    public void respondToAction(ActionResponse response) throws UplinkTerminatedException {
        stateAssertion();
        callMethod(SEND_DATA, response.toPayload());
    }

    public void dispose() throws UplinkTerminatedException {
        stateAssertion();
        context.unbindService(this);
        state = ServiceState.FINISHED;
    }

    /** To be used for testing */
    public void crash() throws UplinkTerminatedException {
        stateAssertion();
        callMethod(CRASH, null);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.e(TAG, "bind finished");
        state = ServiceState.CONNECTED;
        serviceHandle = new Messenger(service);
        serviceReadyCallback.ready();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        state = ServiceState.CRASHED;
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

