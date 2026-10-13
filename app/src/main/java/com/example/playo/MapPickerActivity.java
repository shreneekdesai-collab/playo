package com.example.playo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lets an owner choose a turf location without requiring a Google Maps API key. */
public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";
    private static final double INDIA_LATITUDE = 20.5937;
    private static final double INDIA_LONGITUDE = 78.9629;

    private MapView mapView;
    private TextView coordinatesView;
    private TextView addressView;
    private AppCompatEditText searchInput;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private String currentAddress = "";
    private Runnable pendingMapUpdate;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        configureOsmdroid();
        setContentView(R.layout.activity_map_picker);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        MaterialToolbar toolbar = findViewById(R.id.mapPickerToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        setupToolbarInsets(toolbar);

        mapView = findViewById(R.id.mapView);
        coordinatesView = findViewById(R.id.mapCoordinates);
        addressView = findViewById(R.id.mapAddress);
        searchInput = findViewById(R.id.mapSearchInput);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(this);
        copyrightOverlay.setCopyrightNotice("(c) OpenStreetMap contributors");
        mapView.getOverlays().add(copyrightOverlay);
        mapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                scheduleMapUpdate();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                scheduleMapUpdate();
                return true;
            }
        });

        setupSearch();
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> moveToCurrentLocation());
        findViewById(R.id.btnConfirmLocation).setOnClickListener(v -> confirmLocation());

        setInitialMapPosition();
    }

    private void configureOsmdroid() {
        Context context = getApplicationContext();
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.load(context, PreferenceManager.getDefaultSharedPreferences(context));
        configuration.setUserAgentValue(getPackageName());
        File basePath = new File(getCacheDir(), "osmdroid");
        File tileCache = new File(basePath, "tiles");
        if (!basePath.exists() && !basePath.mkdirs()) {
            Log.w("MapPickerActivity", "Could not create osmdroid cache directory");
        }
        if (!tileCache.exists() && !tileCache.mkdirs()) {
            Log.w("MapPickerActivity", "Could not create osmdroid tile cache directory");
        }
        configuration.setOsmdroidBasePath(basePath);
        configuration.setOsmdroidTileCache(tileCache);
    }

    private void setupToolbarInsets(MaterialToolbar toolbar) {
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = baseToolbarHeight + systemBars.top;
            view.setLayoutParams(params);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    private void setupSearch() {
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchForPlace();
                return true;
            }
            return false;
        });
    }

    private void setInitialMapPosition() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
            moveMap(new GeoPoint(intent.getDoubleExtra(EXTRA_LATITUDE, INDIA_LATITUDE),
                    intent.getDoubleExtra(EXTRA_LONGITUDE, INDIA_LONGITUDE)), 17.0);
            return;
        }
        moveMap(new GeoPoint(INDIA_LATITUDE, INDIA_LONGITUDE), 5.0);
        if (hasLocationPermission()) {
            moveToCurrentLocation();
        }
    }

    private void searchForPlace() {
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            return;
        }
        if (!Geocoder.isPresent()) {
            showMessage("Could not find that place, move the map instead");
            return;
        }
        geocoderExecutor.execute(() -> {
            try {
                List<Address> results = new Geocoder(this, Locale.getDefault())
                        .getFromLocationName(query, 1);
                if (results == null || results.isEmpty()) {
                    showMessage("Could not find that place, move the map instead");
                    return;
                }
                Address result = results.get(0);
                String address = result.getAddressLine(0);
                mainHandler.post(() -> {
                    currentAddress = address == null ? "" : address;
                    moveMap(new GeoPoint(result.getLatitude(), result.getLongitude()), 17.0);
                    updateLocationViews(false);
                });
            } catch (IOException | RuntimeException exception) {
                Log.e("MapPickerActivity", "Place search failed", exception);
                showMessage("Could not find that place, move the map instead");
            }
        });
    }

    private void moveToCurrentLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null || (!isProviderEnabled(locationManager, LocationManager.GPS_PROVIDER)
                && !isProviderEnabled(locationManager, LocationManager.NETWORK_PROVIDER))) {
            showMessage("Turn on location or move the map manually");
            return;
        }
        int priority = hasFineLocationPermission()
                ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(priority, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        showMessage("Could not get your location, move the map manually");
                        return;
                    }
                    moveMap(new GeoPoint(location.getLatitude(), location.getLongitude()), 17.0);
                })
                .addOnFailureListener(error -> {
                    Log.e("MapPickerActivity", "Current location failed", error);
                    showMessage("Could not get your location, move the map manually");
                });
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (granted) {
            moveToCurrentLocation();
        } else {
            showMessage("Location permission was denied, move the map manually");
        }
    }

    private boolean hasLocationPermission() {
        return hasFineLocationPermission()
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isProviderEnabled(LocationManager manager, String provider) {
        try {
            return manager.isProviderEnabled(provider);
        } catch (RuntimeException exception) {
            Log.e("MapPickerActivity", "Could not check location provider", exception);
            return false;
        }
    }

    private void moveMap(GeoPoint point, double zoom) {
        mapView.getController().setZoom(zoom);
        mapView.getController().setCenter(point);
        scheduleMapUpdate();
    }

    private void scheduleMapUpdate() {
        if (pendingMapUpdate != null) {
            mainHandler.removeCallbacks(pendingMapUpdate);
        }
        pendingMapUpdate = () -> updateLocationViews(true);
        mainHandler.postDelayed(pendingMapUpdate, 300);
    }

    private void updateLocationViews(boolean reverseGeocode) {
        if (mapView == null) {
            return;
        }
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        coordinatesView.setText(String.format(Locale.US, "%.5f, %.5f",
                center.getLatitude(), center.getLongitude()));
        if (!reverseGeocode || !Geocoder.isPresent()) {
            addressView.setText(currentAddress);
            return;
        }
        final double latitude = center.getLatitude();
        final double longitude = center.getLongitude();
        geocoderExecutor.execute(() -> {
            String address = "";
            try {
                List<Address> results = new Geocoder(this, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1);
                if (results != null && !results.isEmpty()) {
                    address = results.get(0).getAddressLine(0);
                }
            } catch (IOException | RuntimeException exception) {
                Log.e("MapPickerActivity", "Reverse geocoding failed", exception);
            }
            String resolvedAddress = address == null ? "" : address;
            mainHandler.post(() -> {
                GeoPoint latest = (GeoPoint) mapView.getMapCenter();
                if (Math.abs(latest.getLatitude() - latitude) < 0.0001
                        && Math.abs(latest.getLongitude() - longitude) < 0.0001) {
                    currentAddress = resolvedAddress;
                    addressView.setText(currentAddress);
                }
            });
        });
    }

    private void confirmLocation() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, center.getLatitude());
        result.putExtra(EXTRA_LONGITUDE, center.getLongitude());
        result.putExtra(EXTRA_ADDRESS, currentAddress == null ? "" : currentAddress);
        setResult(RESULT_OK, result);
        finish();
    }

    private void showMessage(String message) {
        mainHandler.post(() -> Snackbar.make(findViewById(R.id.mapFrame), message,
                Snackbar.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (pendingMapUpdate != null) {
            mainHandler.removeCallbacks(pendingMapUpdate);
        }
        geocoderExecutor.shutdownNow();
        if (mapView != null) {
            mapView.onDetach();
        }
        super.onDestroy();
    }
}
