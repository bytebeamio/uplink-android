package io.bytebeam.console;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.bytebeam.console.databinding.ActivityMainBinding;
import io.bytebeam.uplink.Uplink;

import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Uri filePath;
    private String authConfig = "{\n" +
            "  \"project_id\": \"test\",\n" +
            "  \"broker\": \"demo.bytebeam.io\",\n" +
            "  \"port\": 8883,\n" +
            "  \"device_id\": \"1040\",\n" +
            "  \"authentication\": {\n" +
            "    \"ca_certificate\": \"-----BEGIN CERTIFICATE-----\\n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\\na0cIevjvYAtVysoXInV0kg==\\n-----END CERTIFICATE-----\\n\",\n" +
            "    \"device_certificate\": \"-----BEGIN CERTIFICATE-----\\nMIIEajCCAlKgAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\\ndGViZWFtLmlvMB4XDTIxMDkxMzA4MzQ0MVoXDTMxMDkxMzA4MzQ0MVowHjENMAsG\\nA1UEChMEdGVzdDENMAsGA1UEAxMEMTA0MDCCASIwDQYJKoZIhvcNAQEBBQADggEP\\nADCCAQoCggEBAM8WTP1+5KknjTHg3K2O5EDcyO1ayFrMp95x6Gh1mTVvTAz7lE9v\\nNYfOST3c76IB7AD34ufpwW3Cixb0SmrgtLFoJeTa/KZTIW3MdHdJegizFw3vca5Z\\nP+JYhpwo2Jt6exPRCM6YpIbOQHQAX98UFtEYxWQvsSUBg1k/fvbGqbjuPmkyRcpr\\nbvNMzJ1JzfjLySu1scsDHmSnceTX7TFLXvp8Zo7EV6LVuZnwKEksT89oq0pDMTS1\\nfdLEV/XCvgHPX1SPjXb0Wfiyx370d5UQqRSB4D1Rtv/kzS5KyG2ht87r9iybgE1f\\n+K6jdQxU6x+32tD5SRdqdAhe3mdR0pg8JGkCAwEAAaNZMFcwDgYDVR0PAQH/BAQD\\nAgWgMBMGA1UdJQQMMAoGCCsGAQUFBwMCMB8GA1UdIwQYMBaAFKl/MTbLrZ0gurne\\nOmAfBHO+LHz+MA8GA1UdEQQIMAaCBDEwNDAwDQYJKoZIhvcNAQELBQADggIBAGEc\\nE0LGURSs3E1TmqM2vUijNwxWz+T7LwriFdgBwx5to6yGafd80ZMxdN7chAnv0uTp\\nlvvWMIoW4JtdeYK1fi7H7dEFyu5LO2dEfbFcdF41BGQim3aXMhf5QGPQgCrjt3sM\\nbeKOj+Vb4935DNyYdaVS7fFzLvIVDLBRbX0jJyJY4wiREV2y+E3Rek5DlLWxMcDN\\nf1l7iWyi+EiSlP6CyPg+I1mzpGKX1ZPpiaVX9tYGCNntS8nxQFqVOdWJC92XnpjM\\n97gI24xCtzcL+oT2bpglXDVhoILcRiX5jEjAp3m5Tt5oRnTQIHLNA+a3pZFDNe9L\\nAnCkFDfYUBrgletCSfN37Ij+PjSaP6A0OqySf0JgthtYQSlHceSh7FifFlXtRcAF\\nUltOWhAorjxGLNsBs6+F+8Bpu7WsVnVB61O6QJMHJCslN0bQdWabh8GiL04bS1Uq\\nITpaZyQagTE1UnkjcQQOCij2/L+WLFteUHIs8KKVPb6N8NG23buKEvU7QHzw6Jl8\\nUjOOFO992mwcVSLrBhkDZC4BSI4JevkVciExWD+SZTyJMfLDi98DBB8qml0v+Rp9\\n3Ds7/28h8hN4qOm2cSJCZwtUAVUvJfifB3v5Fz/PFx9dIzZErjo9NgVMJ38jRFfm\\nq619b/c7CjdaBko69lohxUVd0PqPOnIU24n++RP1\\n-----END CERTIFICATE-----\\n\",\n" +
            "    \"device_private_key\": \"-----BEGIN RSA PRIVATE KEY-----\\nMIIEpAIBAAKCAQEAzxZM/X7kqSeNMeDcrY7kQNzI7VrIWsyn3nHoaHWZNW9MDPuU\\nT281h85JPdzvogHsAPfi5+nBbcKLFvRKauC0sWgl5Nr8plMhbcx0d0l6CLMXDe9x\\nrlk/4liGnCjYm3p7E9EIzpikhs5AdABf3xQW0RjFZC+xJQGDWT9+9sapuO4+aTJF\\nymtu80zMnUnN+MvJK7WxywMeZKdx5NftMUte+nxmjsRXotW5mfAoSSxPz2irSkMx\\nNLV90sRX9cK+Ac9fVI+NdvRZ+LLHfvR3lRCpFIHgPVG2/+TNLkrIbaG3zuv2LJuA\\nTV/4rqN1DFTrH7fa0PlJF2p0CF7eZ1HSmDwkaQIDAQABAoIBAEVYmGuC5JtobTW4\\nsO1FnlXCGV6yOcl+IvCwgD0KtEVagcMPM/jtqqVRhOE8bNp5fkhMuiUi9+0DaoRD\\nRfBIUvndgGMEmfoweE9GWfHgHwduwVefSRgzNtta/aipXO+jsjdOln5oSyABTUAL\\nKA+RsJpQizkjZ1SXDx8Bzkhg+lC8jKqv5pMn65jrAR9aU8fulbowi6ruQ5GKEO9p\\nc9OynIbbXGW0eBcr3laYHC9o/dMAnJRrNMeKRetSc0Vqe+Q58KMn3mDJA8F7Jm/2\\nGVRs8WVDza/EDSD3Tumsm2DhQtHVCtSSa9qO8qa0NZ6YE4zZbJt/gUhVT/zCwUGK\\nq7z9eqECgYEA8qN2i0H6hhwFEpHdAjugqibLMK+2IdNIVwNxLJS9rivwM6HHfHuJ\\n8mZiyHramL7tFCM2zDXzxaXtyWIKAyrLwUdWqwnKt54tfJsgMHyZdJ1eaYE6hlyT\\n0FvrhzDcm+MIJv8YW96sLGWUnUcK/Ozd2ZPVzCOm9grIxAker7T8MW0CgYEA2n2p\\nDMg+KHO5Xd0ZGRMolQlebsZ/O8tfxqpQkw7XjQBhvz5CYb+YPHnRI4beeU9aHeUA\\n8uccdMGV+PyXv9MA2rhdeKn3uZuZzPETCy2Te6SMTDWMsVZwXiqWoMGgdg5Hp2eW\\n3+Ci/H08tLJBS/N52E6oe7e0jqW3iODxHuzw3W0CgYA6b/QVBgb4VbdDCa5Y41OG\\np2E4kJkk/GXnzwRq4EfustZfGQ+ag4Ztwwr3jd8n+pPOzcxc0oGrkJL8dYhDywLX\\nwf61ot4X6xi5cgMGqnurAlvCvUUDJzjSbdED9lirkrpb6gRL3A1LhAuO9ZVH5SRp\\nSpmrWMrVZzODQ08IsmYq4QKBgQCcRx1PgzrSfFOuC6MUCFwSnezplxkSj9klpFSV\\nmxwaQpenzsR0XjJpr0gj/SfL5TI0B8Sx+RSlfoHi4ek4z5fg2dYhpJEINX/A0v4o\\nFKVU3tFrATJs9cLR1+x9d4Fqb7RYzQNhhq+NoZZ2OLnztWcFjN1+AFwpW+b3BM3y\\nrM9r0QKBgQC44PbeFPO60XkfjhxgS/ud749dC8aOu7RMSonn6gtKQT5OZ2UFvola\\nFganFZNzpZD0RZLaBsaaXp5XAK6dUrQLKISZCrWyqBVJCnbzXshQTPZ/B/hRmR2k\\nkrDEmcCz7O5rbahhpJvoSbOY+75hYeFoMIxQOd2kXFB+GANoNN1NCQ==\\n-----END RSA PRIVATE KEY-----\\n\"\n" +
            "  }\n" +
            "}";
    private Uplink uplink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initUplink(view);
            }
        });
    }

    public void initUplink(View view) {
        try {
//            selectConfig();
            if (authConfig.isEmpty()) {
                throw new Exception("Empty auth config");
            }
            // Edit persistence config
            String path = Environment.getExternalStorageDirectory().getPath() + "/uplink";
            JSONObject config = new JSONObject(authConfig);
            String broker = config.getString("broker");
            JSONObject persist = new JSONObject("{\n" +
                    "    \"path\": \"" + path + "\",\n" +
                    "    \"max_file_size\": 104857600,\n" +
                    "    \"max_file_count\": 3\n" +
                    "  },\n");
            config.put("persistence", persist);
            authConfig = config.toString();

            uplink = new Uplink(authConfig);
            Snackbar.make(getCurrentFocus(), "Connecting to: " + broker, Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("Couldn't start uplink", e.toString());
            Snackbar.make(view, "Couldn't connect: " + e, Snackbar.LENGTH_LONG).show();
        }
    }

//    static final int REQUEST_JSON_GET = 1;

//    public void selectConfig() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("application/json");
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(intent, REQUEST_JSON_GET);
//        }
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_JSON_GET && resultCode == RESULT_OK) {
//            filePath = data.getData();
//            File file = new File(filePath.getPath());
//            try {
//                authConfig = getConfig(file);
//            } catch (Exception e) {
//                Snackbar.make(getCurrentFocus(), "Couldn't load config: " + e, Snackbar.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    public static String getConfig(File file) throws IOException {
//        final InputStream inputStream = new FileInputStream(file);
//        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//        final StringBuilder stringBuilder = new StringBuilder();
//
//        boolean done = false;
//
//        while (!done) {
//            final String line = reader.readLine();
//            done = (line == null);
//
//            if (line != null) {
//                stringBuilder.append(line);
//            }
//        }
//
//        reader.close();
//        inputStream.close();
//
//        return stringBuilder.toString();
//    }

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