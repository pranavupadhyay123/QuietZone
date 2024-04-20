package co.pranavlabs.quietzone;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private boolean notificationPermissionRequested = false;
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PREF_LATITUDE = "latitude";
    private static final String PREF_LONGITUDE = "longitude";
    static final String CIRCLE_LATITUDE = "circle_latitude";
    static final String CIRCLE_LONGITUDE = "circle_longitude";
    public static final int MAX_CIRCLES = 3;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private SharedPreferences sharedPreferences;
    private int clickCounter = 0;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!notificationPermissionRequested) {
            requestNotificationPermission();
        }

        startForegroundService(new Intent(this, ForegroundService.class));

        ImageButton button = findViewById(R.id.clickBtn);
        button.setOnClickListener(this::showPopupMenu);

        Button btnFindLocation = findViewById(R.id.btn_find_location);
        btnFindLocation.setOnClickListener(v -> moveCameraToCurrentLocation());
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.clear_storage) {
                onClearStorageClicked(view);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        setUpMap();
    }

    private void setUpMap() {
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        enableMyLocation();
        restoreCircles();
        moveCameraToCurrentLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if (clickCounter < MAX_CIRCLES) {
            addCircle(latLng);
            clickCounter++;
        }
    }

    private void addCircle(LatLng position) {
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(position)
                .radius(100)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(70, 0, 0, 255)));
        saveCirclePosition(position);
    }

    private void saveCirclePosition(LatLng position) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(CIRCLE_LATITUDE + clickCounter, (float) position.latitude);
        editor.putFloat(CIRCLE_LONGITUDE + clickCounter, (float) position.longitude);
        editor.apply();
    }

    private void restoreCircles() {
        LatLng firstCirclePosition = null;
        for (int i = 0; i < MAX_CIRCLES; i++) {
            float circleLat = sharedPreferences.getFloat(CIRCLE_LATITUDE + i, 0);
            float circleLng = sharedPreferences.getFloat(CIRCLE_LONGITUDE + i, 0);
            if (circleLat != 0 && circleLng != 0) {
                LatLng circlePosition = new LatLng(circleLat, circleLng);
                addCircle(circlePosition);
                clickCounter++;
                if (firstCirclePosition == null) {
                    firstCirclePosition = circlePosition;
                }
            }
        }
        if (firstCirclePosition != null) {
            moveCameraToPosition(firstCirclePosition);
        }
    }

    private void moveCameraToPosition(LatLng position) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 15);
        mMap.moveCamera(cameraUpdate);
    }

    private void checkIfInsideAnyCircle(LatLng point) {
        boolean insideAnyCircle = false;
        for (int i = 0; i < MAX_CIRCLES; i++) {
            float circleLat = sharedPreferences.getFloat(CIRCLE_LATITUDE + i, 0);
            float circleLng = sharedPreferences.getFloat(CIRCLE_LONGITUDE + i, 0);
            if (circleLat != 0 && circleLng != 0) {
                LatLng circleCenter = new LatLng(circleLat, circleLng);
                float[] distance = new float[1];
                Location.distanceBetween(point.latitude, point.longitude, circleCenter.latitude, circleCenter.longitude,
                        distance);
                if (distance[0] <= 100) {
                    insideAnyCircle = true;
                    break;
                }
            }
        }
        if (insideAnyCircle) {
            toggleNotificationMode(true);
            System.out.println("You are inside. DND mode enabled.");
        } else {
            toggleNotificationMode(false);
            System.out.println("You are outside. DND mode disabled.");
        }
    }

    private final Handler mHandler = new Handler();
    private static final long DELAY_INTERVAL = 10000;

    @Override
    protected void onResume() {
        super.onResume();
        startLocationCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        startLocationCheck();
    }

    private void startLocationCheck() {
        mHandler.postDelayed(mRunnable, DELAY_INTERVAL);
    }

    private void stopLocationCheck() {
        mHandler.removeCallbacks(mRunnable);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            LatLng currentLocation = getCurrentLocation();
            if (currentLocation != null) {
                checkIfInsideAnyCircle(currentLocation);
            }

            mHandler.postDelayed(this, DELAY_INTERVAL);
        }
    };

    private LatLng currentLocation;

    private LatLng getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return null;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {

                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            }
        });

        return currentLocation;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivityForResult(intent, NOTIFICATION_PERMISSION_REQUEST_CODE);
                notificationPermissionRequested = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    System.out.println("ok done");
                } else {
                    requestNotificationPermission();
                }
            }
        }
    }

    private void toggleNotificationMode(boolean enableDnd) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (enableDnd) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    sendNotification("Do Not Disturb Enabled", "DND mode has been enabled.");
                }
            } else {

                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                sendNotification("Do Not Disturb Disabled", "DND mode has been disabled.");
            }
        }
    }

    public void onClearStorageClicked(View view) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        mMap.clear();
        clickCounter = 0;
    }

    private void sendNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelId = "DND_Notifications";
            CharSequence channelName = "DND Notifications";
            String channelDescription = "Channel for DND mode notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setAutoCancel(true);

            assert notificationManager != null;
            notificationManager.notify(0, builder.build());
        }
    }

    private void moveCameraToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    moveCameraToPosition(currentLatLng);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

}