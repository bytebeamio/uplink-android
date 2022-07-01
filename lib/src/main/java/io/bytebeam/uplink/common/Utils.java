package io.bytebeam.uplink.common;

import android.content.Context;
import androidx.annotation.RawRes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {
    public static String getRawTextFile(Context context, @RawRes int id) {
        InputStream resource = context.getResources().openRawResource(id);
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
