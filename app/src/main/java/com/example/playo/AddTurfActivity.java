package com.example.playo;

import android.net.Uri;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Form to add a turf listing. Captures photos plus all details, uploads the
 * photos as Firestore Blobs and saves the turf document to Firestore.
 */
public class AddTurfActivity extends AppCompatActivity implements PhotoPreviewAdapter.Listener {

    private TextInputEditText inputTurfName;
    private TextInputEditText inputPrice;
    private TextInputEditText inputLocation;
    private TextInputEditText inputMapLocation;
    private TextInputLayout inputMapLocationLayout;
    private TextInputLayout inputLayoutSport;
    private AutoCompleteTextView inputSport;
    private TextInputEditText inputDescription;
    private TextInputEditText inputContactNumber;
    private SwitchMaterial switchAvailability;
    private MaterialButton btnPublish;
    private ProgressBar publishProgress;
    private RecyclerView photoPreviewRecycler;

    private final List<Uri> selectedPhotos = new ArrayList<>();
    private PhotoPreviewAdapter photoAdapter;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private Double selectedLatitude;
    private Double selectedLongitude;
    private String selectedAddress = "";

    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Intent data = result.getData();
                if (!data.hasExtra(MapPickerActivity.EXTRA_LATITUDE)
                        || !data.hasExtra(MapPickerActivity.EXTRA_LONGITUDE)) {
                    return;
                }
                selectedLatitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0.0);
                selectedLongitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0.0);
                selectedAddress = data.getStringExtra(MapPickerActivity.EXTRA_ADDRESS);
                if (selectedAddress == null) {
                    selectedAddress = "";
                }
                updateMapLocationField();
                if (inputLocation.getText() == null
                        || inputLocation.getText().toString().trim().isEmpty()) {
                    if (!selectedAddress.isEmpty()) {
                        inputLocation.setText(selectedAddress);
                    }
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (uris != null) {
                    selectedPhotos.clear();
                    selectedPhotos.addAll(uris.subList(0, Math.min(uris.size(), 5)));
                    photoAdapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserRole.fetchRole(role -> {
            if (isFinishing()) {
                return;
            }
            if (!"owner".equals(role)) {
                Toast.makeText(this, "Only turf owners can do this", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            setupOwnerScreen(savedInstanceState);
        });
    }

    private void setupOwnerScreen(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_turf);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Edge-to-edge: keep the toolbar below the status bar ---
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topInset = systemBars.top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + topInset;
            v.setLayoutParams(lp);
            return insets;
        });

        // --- Optional pre-fill when editing ---
        String turfName = getIntent().getStringExtra("turf_name");

        // --- Bind views ---
        inputTurfName = findViewById(R.id.inputTurfName);
        inputPrice = findViewById(R.id.inputPrice);
        inputLocation = findViewById(R.id.inputLocation);
        inputMapLocation = findViewById(R.id.inputMapLocation);
        inputMapLocationLayout = findViewById(R.id.inputMapLocationLayout);
        inputLayoutSport = findViewById(R.id.inputLayoutSport);
        inputSport = findViewById(R.id.inputSport);
        inputDescription = findViewById(R.id.inputDescription);
        inputContactNumber = findViewById(R.id.inputContactNumber);
        switchAvailability = findViewById(R.id.switchAvailability);
        btnPublish = findViewById(R.id.btnPublish);
        publishProgress = findViewById(R.id.publishProgress);
        photoPreviewRecycler = findViewById(R.id.photoPreviewRecycler);

        setupSportDropdown();
        setupPhotoPreview();
        inputMapLocation.setOnClickListener(v -> openMapPicker());
        inputMapLocationLayout.setEndIconOnClickListener(v -> openMapPicker());

        if (savedInstanceState != null
                && savedInstanceState.containsKey("selectedLatitude")
                && savedInstanceState.containsKey("selectedLongitude")) {
            selectedLatitude = savedInstanceState.getDouble("selectedLatitude");
            selectedLongitude = savedInstanceState.getDouble("selectedLongitude");
            selectedAddress = savedInstanceState.getString("selectedAddress", "");
            updateMapLocationField();
        }

        if (turfName != null) {
            inputTurfName.setText(turfName);
        }

        // --- Publish action ---
        btnPublish.setOnClickListener(v -> publishListing());
    }

    @Override
    protected void onDestroy() {
        imageExecutor.shutdownNow();
        super.onDestroy();
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        if (selectedLatitude != null && selectedLongitude != null) {
            intent.putExtra(MapPickerActivity.EXTRA_LATITUDE, selectedLatitude);
            intent.putExtra(MapPickerActivity.EXTRA_LONGITUDE, selectedLongitude);
        }
        mapPickerLauncher.launch(intent);
    }

    private void updateMapLocationField() {
        if (inputMapLocation != null && selectedLatitude != null && selectedLongitude != null) {
            inputMapLocation.setText(String.format(Locale.US,
                    "Location selected (%.5f, %.5f)", selectedLatitude, selectedLongitude));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedLatitude != null && selectedLongitude != null) {
            outState.putDouble("selectedLatitude", selectedLatitude);
            outState.putDouble("selectedLongitude", selectedLongitude);
            outState.putString("selectedAddress", selectedAddress);
        }
    }

    /** Populates the sport dropdown with the same categories used on the Category screen. */
    private void setupSportDropdown() {
        String[] sports = {"Cricket", "Football", "Badminton", "Tennis", "Basketball",
                "Swimming", "Hockey", "Volleyball", "Table Tennis", "Kabaddi"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, sports);
        inputSport.setAdapter(adapter);
    }

    /** Sets up the horizontal photo preview row with an "Add Photos" tile. */
    private void setupPhotoPreview() {
        photoAdapter = new PhotoPreviewAdapter(selectedPhotos, this);
        photoPreviewRecycler.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        photoPreviewRecycler.setAdapter(photoAdapter);
    }

    @Override
    public void onAddPhoto() {
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    @Override
    public void onRemovePhoto(int position) {
        if (position >= 0 && position < selectedPhotos.size()) {
            selectedPhotos.remove(position);
            photoAdapter.notifyDataSetChanged();
        }
    }

    /** Validates the form, compresses photos off the main thread, and saves them to Firestore. */
    private void publishListing() {
        String name = inputTurfName.getText() != null ? inputTurfName.getText().toString().trim() : "";
        String priceStr = inputPrice.getText() != null ? inputPrice.getText().toString().trim() : "";
        String location = inputLocation.getText() != null ? inputLocation.getText().toString().trim() : "";
        String sport = inputSport.getText() != null ? inputSport.getText().toString().trim() : "";
        String description = inputDescription.getText() != null ? inputDescription.getText().toString().trim() : "";
        String contactNumber = inputContactNumber.getText() != null ? inputContactNumber.getText().toString().trim() : "";

        if (selectedLatitude == null || selectedLongitude == null) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Please pick the turf location on the map", Snackbar.LENGTH_SHORT).show();
            inputMapLocation.requestFocus();
            findViewById(R.id.addTurfScrollView).post(() ->
                    ((android.widget.ScrollView) findViewById(R.id.addTurfScrollView))
                            .smoothScrollTo(0, inputMapLocation.getTop()));
            return;
        }

        if (sport.isEmpty()) {
            inputLayoutSport.setError(getString(R.string.fill_required));
        } else {
            inputLayoutSport.setError(null);
        }
        if (name.isEmpty() || priceStr.isEmpty() || location.isEmpty() || sport.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.fill_required), Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (selectedPhotos.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.at_least_one_photo), Snackbar.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.fill_required), Snackbar.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Snackbar.make(findViewById(android.R.id.content), "Not signed in", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        // Reserve the Firestore document id before writing the photo subcollection.
        DocumentReference turfRef = FirebaseFirestore.getInstance()
                .collection("turfs").document();
        String turfId = turfRef.getId();

        setPublishing(true);
        imageExecutor.execute(() -> {
            try {
                List<byte[]> photos = new ArrayList<>();
                for (Uri photo : selectedPhotos) {
                    photos.add(ImageUtils.compress(this, photo, 1024, 85));
                }
                byte[] thumbnail = ImageUtils.compress(this, selectedPhotos.get(0), 400, 70, 50 * 1024);

                Map<String, Object> turf = new HashMap<>();
                turf.put("turfId", turfId);
                turf.put("ownerId", uid);
                turf.put("name", name);
                turf.put("price", price);
                turf.put("location", location);
                turf.put("latitude", selectedLatitude);
                turf.put("longitude", selectedLongitude);
                turf.put("mapLink", "https://www.google.com/maps/search/?api=1&query="
                        + selectedLatitude + "," + selectedLongitude);
                turf.put("sport", sport);
                turf.put("description", description);
                turf.put("contactNumber", contactNumber);
                turf.put("available", switchAvailability.isChecked());
                turf.put("thumbnail", Blob.fromBytes(thumbnail));
                turf.put("photoCount", photos.size());
                turf.put("rating", 0.0);
                turf.put("createdAt", FieldValue.serverTimestamp());

                runOnUiThread(() -> turfRef.set(turf)
                        .addOnSuccessListener(unused -> writePhotos(turfRef, photos))
                        .addOnFailureListener(e -> {
                            ErrorLogger.log(this, "AddTurf", "publish_listing", e);
                            showPublishError("Failed to publish. Please try again." );
                        }));
            } catch (Exception e) {
                ErrorLogger.log(this, "AddTurf", "process_photos", e);
                runOnUiThread(() -> showPublishError(
                        "This photo could not be used. Please choose another one."));
            }
        });
    }

    private void writePhotos(DocumentReference turfRef, List<byte[]> photos) {
        com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();
        for (int i = 0; i < photos.size(); i++) {
            Map<String, Object> photo = new HashMap<>();
            photo.put("index", i);
            photo.put("data", Blob.fromBytes(photos.get(i)));
            batch.set(turfRef.collection("photos").document(String.valueOf(i)), photo);
        }
        batch.commit()
                .addOnSuccessListener(unused -> {
                    setPublishing(false);
                    Toast.makeText(this, "Turf published successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> turfRef.delete()
                        .addOnCompleteListener(unused -> {
                            ErrorLogger.log(this, "AddTurf", "save_photos", e);
                            showPublishError("Failed to save photos. Please try again.");
                        }));
    }

    private void showPublishError(String message) {
        setPublishing(false);
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    /** Toggles the in-flight state of the publish flow. */
    private void setPublishing(boolean publishing) {
        publishProgress.setVisibility(publishing ? android.view.View.VISIBLE : android.view.View.GONE);
        btnPublish.setEnabled(!publishing);
    }
}
