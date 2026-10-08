package uk.co.gtrmotorcycles.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private static final int BACKGROUND_REQUEST = 101;
    private static final int NOTIFICATION_REQUEST = 102;
    private TextView status;
    private TextView lastLocation;
    private Button toggle;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 3000);
        }
    };
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
        lastLocation = new TextView(this);
        lastLocation.setTextColor(Color.rgb(255,234,0));
        lastLocation.setPadding(12,0,12,8);
        lastLocation.setText("Last location: waiting");
        toggle = new Button(this);
        toggle.setText("START GPS");
        toggle.setBackgroundColor(Color.rgb(255,234,0));
        toggle.setTextColor(Color.BLACK);
        controls.addView(toggle);
        root.addView(controls);
        root.addView(lastLocation);
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
        handler.post(refresh);
    }
    private void requestAndStart(Button button) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        if (Build.VERSION.SDK_INT == 29 && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_REQUEST);
            return;
        }
        if (Build.VERSION.SDK_INT >= 30 && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                .setTitle("Allow background GPS")
                .setMessage("On the next screen select Permissions > Location > Allow all the time. Return to GTR and press START GPS again.")
                .setPositiveButton("OPEN SETTINGS", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("CANCEL", null)
                .show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
            return;
        }
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Turn on Location/GPS, then press START GPS again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        Intent service = new Intent(this, LocationService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            status.setText("GPS background tracking is active");
            button.setText("STOP GPS");
            Toast.makeText(this, "GPS tracking started.", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            status.setText("GPS could not start");
            Toast.makeText(this, "GPS error: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void stopTracking(Button button) {
        stopService(new Intent(this, LocationService.class));
        status.setText("GPS background tracking is off");
        button.setText("START GPS");
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            requestAndStart(toggle);
        } else if (requestCode == BACKGROUND_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            requestAndStart(toggle);
        } else if (requestCode == NOTIFICATION_REQUEST) {
            requestAndStart(toggle);
        } else {
            Toast.makeText(this, "Location permission is required for GPS tracking.", Toast.LENGTH_LONG).show();
        }
    }
    private void refreshStatus() {
        String value = getSharedPreferences("tracking", MODE_PRIVATE).getString("last_location", null);
        long time = getSharedPreferences("tracking", MODE_PRIVATE).getLong("last_location_time", 0);
        if (value != null) lastLocation.setText("Last location: " + value + " · " + android.text.format.DateFormat.format("HH:mm:ss", time));
        if (LocationService.isRunning) {
            status.setText("GPS background tracking is active");
            toggle.setText("STOP GPS");
        }
    }
    @Override protected void onDestroy() {
        handler.removeCallbacks(refresh);
        super.onDestroy();
    }
}
