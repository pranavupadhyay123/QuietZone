package co.pranavlabs.autosilent;

import static co.pranavlabs.autosilent.MainActivity.CIRCLE_LATITUDE;
import static co.pranavlabs.autosilent.MainActivity.CIRCLE_LONGITUDE;
import static co.pranavlabs.autosilent.MainActivity.MAX_CIRCLES;
import static co.pranavlabs.autosilent.MainActivity.PREFS_NAME;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class ForegroundService extends Service {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int LOCATION_REQUEST_INTERVAL = 5000; // 5 seconds
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("Your app is running in the background")
                .build();

        startForeground(1, notification);

        requestLocationUpdates();

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Handle location updates here
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d("LocationUpdate", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                    // Check if inside circle radius
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        checkIfInsideAnyCircle(new LatLng(location.getLatitude(), location.getLongitude()));
                    }
                }
            }
        };
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private static LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        /* 5 secs */
        long UPDATE_INTERVAL = 5000;
        locationRequest.setInterval(UPDATE_INTERVAL);
        /* 2 sec */
        long FASTEST_INTERVAL = 2000;
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkIfInsideAnyCircle(LatLng point) {
        boolean insideAnyCircle = false;
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < MAX_CIRCLES; i++) {
            float circleLat = sharedPreferences.getFloat(CIRCLE_LATITUDE + i, 0);
            float circleLng = sharedPreferences.getFloat(CIRCLE_LONGITUDE + i, 0);
            if (circleLat != 0 && circleLng != 0) {
                LatLng circleCenter = new LatLng(circleLat, circleLng);
                float[] distance = new float[1];
                Location.distanceBetween(point.latitude, point.longitude, circleCenter.latitude, circleCenter.longitude, distance);
                if (distance[0] <= 100) { // Assuming the radius is 100 meters
                    insideAnyCircle = true;
                    break;
                }
            }
        }
        if (insideAnyCircle) {
            toggleNotificationMode(true); // Enable DND mode
            Log.d("ForegroundService", "You are inside. DND mode enabled.");
        } else {
            toggleNotificationMode(false); // Disable DND mode
            Log.d("ForegroundService", "You are outside. DND mode disabled.");
        }
    }

    private void toggleNotificationMode(boolean enableDnd) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (enableDnd) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                }

            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
        }
    }
}
