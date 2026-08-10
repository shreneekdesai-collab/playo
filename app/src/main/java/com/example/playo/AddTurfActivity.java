package com.example.playo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Form to add or edit a turf listing. For a complete implementation this
 * should connect to a backend; here it validates and finishes.
 */
public class AddTurfActivity extends AppCompatActivity {

    private ShapeableImageView turfPhoto;
    private TextInputEditText inputTurfName;
    private TextInputEditText inputPrice;
    private TextInputEditText inputLocation;
    private SwitchMaterial switchAvailability;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    turfPhoto.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_turf);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Optional pre-fill when editing ---
        String turfName = getIntent().getStringExtra("turf_name");

        // --- Bind views ---
        turfPhoto = findViewById(R.id.turfPhoto);
        inputTurfName = findViewById(R.id.inputTurfName);
        inputPrice = findViewById(R.id.inputPrice);
        inputLocation = findViewById(R.id.inputLocation);
        switchAvailability = findViewById(R.id.switchAvailability);

        if (turfName != null) {
            inputTurfName.setText(turfName);
        }

        FloatingActionButton pickPhoto = findViewById(R.id.btnPickPhoto);
        pickPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // --- Publish action ---
        findViewById(R.id.btnPublish).setOnClickListener(v -> publishListing());
    }

    /** Validates the form and confirms the listing was published. */
    private void publishListing() {
        if (inputTurfName.getText().toString().trim().isEmpty()
                || inputPrice.getText().toString().trim().isEmpty()
                || inputLocation.getText().toString().trim().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.fill_required), Snackbar.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, getString(R.string.add_turf) + " saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}