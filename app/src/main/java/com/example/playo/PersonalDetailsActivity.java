package com.example.playo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Personal Details screen. All fields are editable and persisted using
 * SharedPreferences (no Firebase is present in the project yet).
 */
public class PersonalDetailsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "playo_prefs";
    private static final String PROFILE_IMAGE_KEY = "profile_image";

    private TextInputLayout inputLayoutName;
    private TextInputEditText inputName;
    private TextInputEditText inputEmail;
    private TextInputEditText inputMobile;
    private AutoCompleteTextView inputGender;
    private TextInputEditText inputDob;
    private TextInputEditText inputAddress;
    private ShapeableImageView profilePhoto;

    // Launcher for picking a photo from the gallery.
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    profilePhoto.setImageURI(uri);
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putString(PROFILE_IMAGE_KEY, uri.toString()).apply();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        // --- Toolbar back navigation ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Bind views ---
        inputLayoutName = findViewById(R.id.inputLayoutName);
        profilePhoto = findViewById(R.id.profilePhoto);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputMobile = findViewById(R.id.inputMobile);
        inputGender = findViewById(R.id.inputGender);
        inputDob = findViewById(R.id.inputDob);
        inputAddress = findViewById(R.id.inputAddress);

        setupGenderDropdown();
        setupDatePicker();
        loadSavedProfile();

        // --- Photo picker (tap the photo itself) ---
        profilePhoto.setOnClickListener(v -> openImagePicker());

        // --- Save ---
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (validateFields()) {
                saveProfile();
            }
        });

        // --- Cancel ---
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    /** Populates the gender dropdown options. */
    private void setupGenderDropdown() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, genders);
        inputGender.setAdapter(adapter);
    }

    /** Opens a date picker when the DOB field is tapped. */
    private void setupDatePicker() {
        inputDob.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Date date = new Date(selection);
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                inputDob.setText(formatted);
            });
            picker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    /** Loads any previously saved profile values. */
    private void loadSavedProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        inputName.setText(prefs.getString("name", ""));
        inputEmail.setText(prefs.getString("email", ""));
        inputMobile.setText(prefs.getString("mobile", ""));
        inputGender.setText(prefs.getString("gender", ""), false);
        inputDob.setText(prefs.getString("dob", ""));
        inputAddress.setText(prefs.getString("address", ""));

        String imageUri = prefs.getString(PROFILE_IMAGE_KEY, null);
        if (imageUri != null) {
            profilePhoto.setImageURI(Uri.parse(imageUri));
        }
    }

    /** Opens the system gallery to choose a new profile photo. */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /** Validates required fields before saving. */
    private boolean validateFields() {
        if (inputName.getText().toString().trim().isEmpty()) {
            inputLayoutName.setError(getString(R.string.fill_required));
            return false;
        }
        inputLayoutName.setError(null);
        return true;
    }

    /** Persists the profile to SharedPreferences. */
    private void saveProfile() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("name", inputName.getText().toString().trim());
        editor.putString("email", inputEmail.getText().toString().trim());
        editor.putString("mobile", inputMobile.getText().toString().trim());
        editor.putString("gender", inputGender.getText().toString().trim());
        editor.putString("dob", inputDob.getText().toString().trim());
        editor.putString("address", inputAddress.getText().toString().trim());

        // Mirror name/email so the Profile header stays in sync.
        editor.putString("user_name", inputName.getText().toString().trim());
        editor.putString("user_email", inputEmail.getText().toString().trim());
        editor.apply();

        Snackbar.make(findViewById(android.R.id.content), getString(R.string.profile_saved), Snackbar.LENGTH_SHORT).show();
    }
}