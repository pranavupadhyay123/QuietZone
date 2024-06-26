package co.pranavlabs.quietzone;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
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
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int CIRCLE_RADIUS = 100;
    private static final int MIN_DISTANCE_CHANGE = 10; // Minimum distance change to trigger location update
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SharedPreferences sharedPreferences;
    private boolean locationPermissionGranted;
    private Location lastLocation; // Store last location to calculate distance change

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        locationPermissionGranted = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                handleLocationUpdates(locationResult.getLastLocation());
            }
        };
    }

    private void requestLocationUpdates() {
        if (locationPermissionGranted) {
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private static LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL / 2);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void handleLocationUpdates(Location location) {
        if (location != null) {
            if (lastLocation != null && lastLocation.distanceTo(location) >= MIN_DISTANCE_CHANGE) {
                Log.d("LocationUpdate", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    checkIfInsideAnyCircle(new LatLng(location.getLatitude(), location.getLongitude()));
                }
                lastLocation = location; // Update last location
            } else if (lastLocation == null) {
                lastLocation = location; // Initialize last location
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void checkIfInsideAnyCircle(LatLng point) {
        boolean insideAnyCircle = false;
        for (int i = 0; i < MainActivity.MAX_CIRCLES; i++) {
            float circleLat = sharedPreferences.getFloat(MainActivity.CIRCLE_LATITUDE + i, 0);
            float circleLng = sharedPreferences.getFloat(MainActivity.CIRCLE_LONGITUDE + i, 0);
            if (circleLat != 0 && circleLng != 0) {
                LatLng circleCenter = new LatLng(circleLat, circleLng);
                float[] distance = new float[1];
                Location.distanceBetween(point.latitude, point.longitude, circleCenter.latitude, circleCenter.longitude,
                        distance);
                if (distance[0] <= CIRCLE_RADIUS) {
                    insideAnyCircle = true;
                    break;
                }
            }
        }
        toggleNotificationMode(insideAnyCircle);
    }

    private void toggleNotificationMode(boolean enableDnd) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (enableDnd && notificationManager.isNotificationPolicyAccessGranted()) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
        }
    }
}
