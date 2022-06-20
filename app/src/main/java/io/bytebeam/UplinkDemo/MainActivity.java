package io.bytebeam.UplinkDemo;

import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import io.bytebeam.uplink.ConfigBuilder;
import io.bytebeam.uplink.generated.*;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ActionCallback {
    private Uplink uplink;
    Time time = new Time();
    long sequence = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initUplink();

        findViewById(R.id.send_btn).setOnClickListener(view -> {
            try {
                time.setToNow();
                long timestamp = time.toMillis(false);
                JSONObject json = new JSONObject();
                json.put("current_time", timestamp);
                String data = String.valueOf(json);
                UplinkPayload payload = new UplinkPayload("current_time", timestamp, sequence++, data);
                Log.i("Uplink Send", data);
                uplink.send(payload);
            } catch (Exception e) {
                Log.e("Uplink Send", e.toString());
            }
        });
    }

    @Override
    public void recvdAction(UplinkAction action) {
        // Print to log received action
        String actionId = action.getId();
        String payload = action.getPayload();
        String actionName = action.getName();
        Log.i("Uplink Recv", "id: " + actionId + "name: " + actionName + "; payload: " + payload);

        Executor executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            // A demonstration of how apps can also pass progress, success and failure messages regarding an action in execution to the cloud
            if (Integer.parseInt(actionId) % 2 == 0) {
                sendResponse(ActionResponse.failure(actionId, "Action failed"));
            } else {
                for (int i = 0; i < 10; i++) {
                    sendResponse(new ActionResponse(actionId, "Running", (short) (i * 10)));
                }
                sendResponse(ActionResponse.success(actionId));
            }
        });
    }

    public void sendResponse(ActionResponse actionResponse) {
        // Send action responses
        try {
            uplink.respond(actionResponse);

            Thread.sleep(1000);
        } catch (Exception e) {
            Log.e("Uplink Respond", e.toString());
        }
    }

    public void initUplink() {
        String baseFolder = getBaseContext().getExternalFilesDir("").getPath();
        // NOTE: base is String contents of config file
        InputStream in_s = getResources().openRawResource(R.raw.device_1076);
        Scanner sc = new Scanner(in_s);
        StringBuilder sb = new StringBuilder();
        while(sc.hasNext()){
            sb.append(sc.nextLine());
        }
        String base = sb.toString();
        try {
            ConfigBuilder config = new ConfigBuilder(base)
                    .setOta(true, baseFolder + "/ota-file")
                    .setPersistence(baseFolder + "/uplink", 104857600, 3);

            uplink = new Uplink(config.build());
            uplink.subscribe(this);
        } catch (Exception e) {
            Log.e("Couldn't start uplink", e.toString());
        }
    }

}