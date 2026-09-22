package uk.co.gtrmotorcycles.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 100;
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5,5,5));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(12,10,12,10);
        status = new TextView(this);
        status.setText("GPS background tracking is off");
        status.setTextColor(Color.WHITE);
        controls.addView(status, new LinearLayout.LayoutParams(0,-2,1));
        Button toggle = new Button(this);
        toggle.setText("START GPS");
        toggle.setBackgroundColor(Color.rgb(255,234,0));
        toggle.setTextColor(Color.BLACK);
        controls.addView(toggle);
        root.addView(controls);
        WebView web = new WebView(this);
        web.setWebViewClient(new WebViewClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setGeolocationEnabled(false);
        web.getSettings().setDomStorageEnabled(true);
        web.loadUrl("https://gtr-motorcycles-rental.marloncanarin.chatgpt.site");
        root.addView(web, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        toggle.setOnClickListener(v -> {
            if (LocationService.isRunning) stopTracking(toggle); else requestAndStart(toggle);
        });
    }
    private void requestAndStart(Button button) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        if (Build.VERSION.SDK_INT >= 30 && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Select Permissions > Location > Allow all the time, then return to the app.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            return;
        }
        Intent service = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        status.setText("GPS background tracking is active");
        button.setText("STOP GPS");
    }
    private void stopTracking(Button button) {
        stopService(new Intent(this, LocationService.class));
        status.setText("GPS background tracking is off");
        button.setText("START GPS");
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location allowed. Tap START GPS again to enable background access.", Toast.LENGTH_LONG).show();
        }
    }
}
