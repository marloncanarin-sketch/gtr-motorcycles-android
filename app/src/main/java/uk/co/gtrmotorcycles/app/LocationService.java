package uk.co.gtrmotorcycles.app;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import java.text.DateFormat;
import java.util.Date;

public class LocationService extends Service implements LocationListener {
    public static volatile boolean isRunning = false;
    private static final String CHANNEL = "gtr_tracking";
    private static final int NOTIFICATION_ID = 27;
    private LocationManager manager;
    @Override public void onCreate() {
        super.onCreate();
        isRunning = true;
        createChannel();
        startForeground(NOTIFICATION_ID, notification("Waiting for GPS location…"));
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 5f, this);
                if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 10f, this);
                Location last = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null) last = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (last != null) onLocationChanged(last);
            } catch (SecurityException error) {
                stopSelf();
            }
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    @Override public void onLocationChanged(Location location) {
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        String value = location.getLatitude() + "," + location.getLongitude();
        getSharedPreferences("tracking", MODE_PRIVATE).edit().putString("last_location", value).putLong("last_location_time", System.currentTimeMillis()).apply();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification("Location updated at " + time));
    }
    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_gtr).setContentTitle("GTR MOTORCYCLES · GPS active").setContentText(text).setOngoing(true).setContentIntent(pending).build();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Background GPS tracking", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible while GTR GPS tracking is active.");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
    @Override public void onDestroy() {
        isRunning = false;
        if (manager != null) manager.removeUpdates(this);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
}
