package io.bytebeam.UplinkDemo

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.ActionSubscriber
import io.bytebeam.uplink.NativeApi
import io.bytebeam.uplink.types.UplinkAction
import io.bytebeam.uplink.types.UplinkPayload
import java.util.Date

fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader().use { it.readText() }

const val TAG = "==APP=="

class MainActivity : AppCompatActivity(), ActionSubscriber {
    var idx: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val uplink = NativeApi.createUplink(
            resources.getRawTextFile(R.raw.device_1160),
            """
                [persistence]
                path = "${applicationInfo.dataDir}/uplink"
            """.trimIndent(),
            this
        )
        findViewById<Button>(R.id.send_btn).setOnClickListener {
            Log.e(TAG, idx.toString())
            NativeApi.sendData(uplink, UplinkPayload("metrics", idx++, Date().time, "{}"))
        }
        findViewById<TextView>(R.id.dbg).text = "test"
    }

    override fun processAction(action: UplinkAction) {
        Log.e(TAG, action.toString())
    }

    //    @Override
    //    public void recvdAction(UplinkAction action) {
    //        // Print to log received action
    //        String actionId = action.getId();
    //        String payload = action.getPayload();
    //        String actionName = action.getName();
    //        Log.i("Uplink Recv", "id: " + actionId + "name: " + actionName + "; payload: " + payload);
    //
    //        Executor executor = Executors.newSingleThreadExecutor();
    //
    //        executor.execute(() -> {
    //            // A demonstration of how apps can also pass progress, success and failure messages regarding an action in execution to the cloud
    //            if (Integer.parseInt(actionId) % 2 == 0) {
    //                sendResponse(ActionResponse.failure(actionId, "Action failed"));
    //            } else {
    //                for (int i = 0; i < 10; i++) {
    //                    sendResponse(new ActionResponse(actionId, "Running", (short) (i * 10)));
    //                }
    //                sendResponse(ActionResponse.success(actionId));
    //            }
    //        });
    //    }
    //
    //    public void sendResponse(ActionResponse actionResponse) {
    //        // Send action responses
    //        try {
    //            uplink.respond(actionResponse);
    //
    //            Thread.sleep(1000);
    //        } catch (Exception e) {
    //            Log.e("Uplink Respond", e.toString());
    //        }
    //    }
    //
    //    public void initUplink() {
    //        String baseFolder = getBaseContext().getExternalFilesDir("").getPath();
    //        // NOTE: base is String contents of config file
    //        InputStream in_s = getResources().openRawResource(R.raw.device_1076);
    //        Scanner sc = new Scanner(in_s);
    //        StringBuilder sb = new StringBuilder();
    //        while(sc.hasNext()){
    //            sb.append(sc.nextLine());
    //        }
    //        String base = sb.toString();
    //        try {
    //            ConfigBuilder config = new ConfigBuilder(base)
    //                    .setOta(true, baseFolder + "/ota-file")
    //                    .setPersistence(baseFolder + "/uplink", 104857600, 3);
    //
    //            uplink = new Uplink(config.build());
    //            uplink.subscribe(this);
    //        } catch (Exception e) {
    //            Log.e("Couldn't start uplink", e.toString());
    //        }
    //    }
}