package io.bytebeam.UplinkDemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import io.bytebeam.uplink.ActionSubscriber;
import io.bytebeam.uplink.types.ActionResponse;
import io.bytebeam.uplink.types.UplinkPayload;

import static io.bytebeam.UplinkDemo.UplinkService.*;

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
        context.bindService(
                intent,
                this,
                Context.BIND_AUTO_CREATE
        );
    }

    public void subscribe(ActionSubscriber subscriber) {
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

    public void sendData(UplinkPayload payload) {
        stateAssertion();
        callMethod(SEND_DATA, payload);
    }

    public void respondToAction(ActionResponse response) {
        stateAssertion();
        callMethod(RESPOND_TO_ACTION, response);
    }

    public void destroy() {
        stateAssertion();
        context.unbindService(this);
        state = ServiceState.FINISHED;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        state = ServiceState.CONNECTED;
        serviceHandle = new Messenger(service);
        serviceReadyCallback.ready();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        state = ServiceState.CRASHED;
    }

    private void stateAssertion() {
        String message = null;
        switch (state) {
            case UNINITIALIZED:
                message = "attempt to use service before initialization is complete";
                break;
            case CRASHED:
                message = "uplink service process crashed";
                break;
            case FINISHED:
                message = "attempt to use service after it was disposed";
                break;
        }
        if (message != null) {
            throw new IllegalStateException(message);
        }
    }
}
