package io.bytebeam.UplinkDemo;

import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.bytebeam.UplinkDemo.databinding.ActivityMainBinding;

import io.bytebeam.uplink.ActionCallback;
import io.bytebeam.uplink.ActionResponse;
import io.bytebeam.uplink.ConfigBuilder;
import io.bytebeam.uplink.Uplink;
import io.bytebeam.uplink.UplinkAction;
import io.bytebeam.uplink.UplinkPayload;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ActionCallback {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Uplink uplink;
    Time time = new Time();
    long sequence = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        initUplink();

        binding.fab.setOnClickListener(view -> {
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
                    .setPersistence(baseFolder + "/uplink", 104857600, 3)
                    .enableLogCollector();

            uplink = new Uplink(config.build());
            uplink.subscribe(this);
        } catch (Exception e) {
            Log.e("Couldn't start uplink", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}