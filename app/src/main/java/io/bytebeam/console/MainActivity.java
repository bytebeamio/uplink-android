package io.bytebeam.console;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.bytebeam.console.databinding.ActivityMainBinding;
import io.bytebeam.uplink.ActionCallback;
import io.bytebeam.uplink.Uplink;

import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity implements ActionCallback {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
//    private Uri filePath;
    private String authConfig;
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

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                try {
                    time.setToNow();
                    long timestamp = time.toMillis(false);
                    JSONObject payload = new JSONObject();
                    payload.put("stream", "current_time");
                    payload.put("timestamp", timestamp);
                    payload.put("sequence", sequence++);
                    JSONObject current_time = new JSONObject();
                    current_time.put("current_time", timestamp);
                    payload.put("payload", current_time);

                    uplink.send(String.valueOf(payload), "current_time");
                } catch (Exception e) {
                    Log.e("Uplink Send", e.toString());
                }
            }
        });
    }

    @Override
    public void recvdAction(String action) {
        Log.i("Uplink recv", action);
    }

    public void initUplink() {
        try {
//            selectConfig();
            // Edit persistence config
            JSONObject config = new JSONObject();
            config.put("project_id", "test");
            config.put("broker", "demo.bytebeam.io");
            config.put("port", 1883);
            config.put("device_id", "1082");
//            JSONObject auth = new JSONObject();
//            auth.put("ca_certificate", "-----BEGIN CERTIFICATE-----\nMIIFrDCCA5SgAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTIxMDkwMjExMDYyM1oXDTMxMDkwMjExMDYyM1owdzEOMAwG\nA1UEBhMFSW5kaWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxv\ncmUxFzAVBgNVBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDAS\nBgNVBAoTC0J5dGViZWFtLmlvMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC\nAgEAr/bnOa/8AUGZmd/s+7rejuROgeLqqU9X15KKfKOBqcoMyXsSO65UEwpzadpw\nMl7GDCdHqFTymqdnAnbhgaT1PoIFhOG64y7UiNgiWmbh0XJj8G6oLrW9rQ1gug1Q\n/D7x2fUnza71aixiwEL+KsIFYIdDuzmoRD3rSer/bKOcGGs0WfB54KqIVVZ1DwsU\nk1wx5ExsKo7gAdXMAbdHRI2Szmn5MsZwGL6V0LfsKLE8ms2qlZe50oo2woLNN6XP\nRfRL4bwwkdsCqXWkkt4eUSNDq9hJsuINHdhO3GUieLsKLJGWJ0lq6si74t75rIKb\nvvsFEQ9mnAVS+iuUUsSjHPJIMnn/J64Nmgl/R/8FP5TUgUrHvHXKQkJ9h/a7+3tS\nlV2KMsFksXaFrGEByGIJ7yR4qu9hx5MXf8pf8EGEwOW/H3CdWcC2MvJ11PVpceUJ\neDVwE7B4gPM9Kx02RNwvUMH2FmYqkXX2DrrHQGQuq+6VRoN3rEdmGPqnONJEPeOw\nZzcGDVXKWZtd7UCbcZKdn0RYmVtI/OB5OW8IRoXFYgGB3IWP796dsXIwbJSqRb9m\nylICGOceQy3VR+8+BHkQLj5/ZKTe+AA3Ktk9UADvxRiWKGcejSA/LvyT8qzz0dqn\nGtcHYJuhJ/XpkHtB0PykB5WtxFjx3G/osbZfrNflcQZ9h1MCAwEAAaNCMEAwDgYD\nVR0PAQH/BAQDAgKEMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFKl/MTbLrZ0g\nurneOmAfBHO+LHz+MA0GCSqGSIb3DQEBCwUAA4ICAQAlus/uKic5sgo1d2hBJ0Ak\ns1XJsA2jz+OEdshQHmCCmzFir3IRSuVRmDBaBGlJDHCELqYxKn6dl/sKGwoqoAQ5\nOeR2sey3Nmdyw2k2JTDx58HnApZKAVir7BDxbIbbHmfhJk4ljeUBbertNXWbRHVr\ncs4XBNwXvX+noZjQzmXXK89YBsV2DCrGRAUeZ4hQEqV7XC0VKmlzEmfkr1nibDr5\nqwbI+7QWIAnkHggYi27lL2UTHpbsy9AnlrRMe73upiuLO7TvkwYC4TyDaoQ2ZRpG\nHY+mxXLdftoMv/ZvmyjOPYeTRQbfPqoRqcM6XOPXwSw9B6YddwmnkI7ohNOvAVfD\nwGptUc5OodgFQc3waRljX1q2lawZCTh58IUf32CRtOEL2RIz4VpUrNF/0E2vts1f\npO7V1vY2Qin998Nwqkxdsll0GLtEEE9hUyvk1F8U+fgjJ3Rjn4BxnCN4oCrdJOMa\nJCaysaHV7EEIMqrYP4jH6RzQzOXLd0m9NaL8A/Y9z2a96fwpZZU/fEEOH71t3Eo3\nV/CKlysiALMtsHfZDwHNpa6g0NQNGN5IRl/w1TS1izzjzgWhR6r8wX8OPLRzhNRz\n2HDbTXGYsem0ihC0B8uzujOhTHcBwsfxZUMpGjg8iycJlfpPDWBdw8qrGu8LeNux\na0cIevjvYAtVysoXInV0kg==\n-----END CERTIFICATE-----\n");
//            auth.put("device_certificate", "-----BEGIN CERTIFICATE-----\nMIIEajCCAlKgAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTIyMDEwNDA1NDI1OFoXDTMyMDEwNDA1NDI1OFowHjENMAsG\nA1UEChMEdGVzdDENMAsGA1UEAxMEMTA4MjCCASIwDQYJKoZIhvcNAQEBBQADggEP\nADCCAQoCggEBAJsP5W81kSj1dr/PR2nnuTXFJtzvYVyN31xeQVWVBmEmepdL2WvC\nGYKb3nNkanMnMlq6eas8nI545cI1qqn0fmYs7V+K6vukbqVZdp+dcdUl4hr1t8dD\nbUolNpJ3gbe5x7ZjHBdc5GeXVhURsXdyCfy2x63gKgRDXulk15XKtJQMpN0dtm2W\nK0LQAthpqrl8JxV5bdNVmtqpBW94vU0KHonOD0xI6WMkzw1ucCWZ9SlXBCizc9kr\nDjJCOEOFTM0R9DMN4rFrudQ0ABOy0aaqkc+sDRLibbwPkRjiPUD4o92zviAZ67L0\nv3wUQl5xmXGltGa2z7juhY7P7BDJIuIFZQcCAwEAAaNZMFcwDgYDVR0PAQH/BAQD\nAgWgMBMGA1UdJQQMMAoGCCsGAQUFBwMCMB8GA1UdIwQYMBaAFKl/MTbLrZ0gurne\nOmAfBHO+LHz+MA8GA1UdEQQIMAaCBDEwODIwDQYJKoZIhvcNAQELBQADggIBAEDB\nJkRDXq25vEBbstifxo4MypbDd36T8WOMGp7/G8IlyhHo2TZMvW1nI2RT9xOgV/FY\nIoqwAOi90/wTlw1oNvqJxXFFLxpLM9ZLN9OrBgtJmuPfgp6RFE2aibHgML50BEab\nwPPMRna61Ba/1T6tRxumgsm8+R5Qj/zPUTPwE+rDb+qjrrNU9PrXWgNqoPPhkvK0\nrYjodNPXAVXL43TaUTEwTpdNb/BXSqkL2IwiW69GH4UHCBmQaYEpfelDJH2fGNSH\nK2OcScR3IM3RVQ3pQwPJUCOjs7dDOTl4EXD0XvxjPpj93H9Y5JITaK9eUjOk+7m0\nTXGii9tBnUyjtD+V7yAV/7u2hXSPK1BECykmEq08E7+qS8iWK5lEfkW/LkuZQC98\nba2c1I+nmgsIoDuJJYxuTKbwa/+GQfcLCCkgZ8pOkX8ePIi4INqf3rytjLz6dhhb\nssM6xVtFHliF/38cFXJBace0/OiicMWjW/sgXWa6RsB7E7eyIeFGn8f68ZCiK+h/\nhhAoQg1aMU0KCj+s8M1ZjzjcDNZqHEIPLagZgd22khyIzCu6X0JZVc13GCHEfxa9\nC+Hw/wHuP5pK0QsEL/AwyibpLvo/46pGmzvoOYAUPnYBSaNoktstkGzIHpngzkvN\neqNdy/8aDV388zKXYSMx72XrfVIICJu/Inh9GH3N\n-----END CERTIFICATE-----\n");
//            auth.put("device_private_key", "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEAmw/lbzWRKPV2v89Haee5NcUm3O9hXI3fXF5BVZUGYSZ6l0vZ\na8IZgpvec2RqcycyWrp5qzycjnjlwjWqqfR+ZiztX4rq+6RupVl2n51x1SXiGvW3\nx0NtSiU2kneBt7nHtmMcF1zkZ5dWFRGxd3IJ/LbHreAqBENe6WTXlcq0lAyk3R22\nbZYrQtAC2GmquXwnFXlt01Wa2qkFb3i9TQoeic4PTEjpYyTPDW5wJZn1KVcEKLNz\n2SsOMkI4Q4VMzRH0Mw3isWu51DQAE7LRpqqRz6wNEuJtvA+RGOI9QPij3bO+IBnr\nsvS/fBRCXnGZcaW0ZrbPuO6Fjs/sEMki4gVlBwIDAQABAoIBADZ0O6d1UVfn897i\nRPr9JH6skLxP2IovTHxcoWcToZzmbXDKcz0zec/zOwidAAEWh8ly6R1oeLZT4KP2\nQsvSj70EFAxUdbcPhMfOhikBmqM23ZOILRTuKeg671I6Y7SIqojzfz75IUD71YAq\nqX7/7l/wNGlsanT6z8742fjBqe6/ggbh4JHanZBtTSQlt8AtP4PwLWkQzEiED2bp\nWzlIvfT8ugISI8sgPcjeIP0TlJtV+buQ1lMKwrmOa4+Dj12upJ5qY1EIwayJ0f9L\nczZBPkuHskDgOtl4P5lNjLjNwqtEZ/Rb+vKR1Nsxn5EEmpc88O9OYi45ACOv8Ylj\nLEaGQDkCgYEAxJDCl8RV7gZElqf7WtBTpgrQInsn1r3sdDaz7Pz7SOJuZdk6dXir\nQUfA2BgWtSegnK202PJPMB/LyZKoE8fGvPYI5vFvcnzWZUoG8MpwvlpDAolaKPg3\nmWcK/bz9US+pZFLpsQ0ly/OxTg5mhZwbnei5Ii1x0ThKW2LQpUeEZzUCgYEAyfKM\nZsxGmNXeN520485GBG00eYwk2j5vnjbE8hkLBxxIJcaF7arT3Sg4JVkntbO11g+j\nbfoDFEtKEmQ2hV4UNVMTn94mlVhIiuFeszmiHziccZwApY/xGBRKWbqdCaI0XE6N\nPNrm3ITEI+bQ6zEZeZBGHiYyisve0KgLlO6lFssCgYEAqZ6lXON+p0RfYYYZX7dP\nx4OjMW4G5cbESVB/GO0BRlamn1rBmGcFmPJ7Fb5Lsg09CpbW7TLDZKq7ZvkX8uG8\nvIivC+KhojDZrVQhAx4eBhTLqF+wHpR7HfQORwETs0AmszzdDfxjdkiW4t2IWJlq\nN2yAfV6rzbf+ajeuBkHdnlUCgYBhUU8zorFKqZWiu48WUKsRKwcko9KGkZv8ZGxY\nNpVH4esquRaFR9M9OkqERQiL2YSBGZwqPVcKipWxczWK46FdaSGF4uo5AghDVQPr\n3pQv83oAjasKHemTLwP9ZZ6Tq+ULrpcFgn/KOPjETFDVZh3epRYFJWcp18ESUEj0\nhmRsLQKBgE1ZmFync7ltNX6qzbHYQaDaozgR5/Twj/WV3dCEw+aPbb4jtEIpN/+0\nDm2QAZQu2l9i8RSZ2Az/HWpgpJCR0tRs30yQqvB8yVzlkCyaZY1lhRaLtEGIOKAl\np6SOESkLP3ryKGPvHyH//OsJkC8v4kMY8t15LOZcFgqx6ElLc27P\n-----END RSA PRIVATE KEY-----\n");
//            config.put("authentication", auth);
            authConfig = config.toString();
            uplink = new Uplink(authConfig);
            uplink.subscribe(this);
            this.recvdAction("Subscription performed");
        } catch (Exception e) {
            Log.e("Couldn't start uplink", e.toString());
            Snackbar.make(getCurrentFocus(), "Couldn't connect: " + e, Snackbar.LENGTH_LONG).show();
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