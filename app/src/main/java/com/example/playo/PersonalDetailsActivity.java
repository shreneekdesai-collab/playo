package com.example.playo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Personal Details screen. All text fields are loaded from and saved to the
 * signed-in user's Firestore document under users/{uid}; only the profile
 * photo is persisted locally with SharedPreferences.
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personal_details);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        // --- Toolbar back navigation ---
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

        // Email belongs to the Firebase account and can't be edited here
        // without re-authentication, so show it read-only.
        inputEmail.setEnabled(false);

        loadUserFromFirestore();

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

    /** Restores the locally stored profile photo, if any. */
    private void loadSavedProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String imageUri = prefs.getString(PROFILE_IMAGE_KEY, null);
        if (imageUri != null) {
            profilePhoto.setImageURI(Uri.parse(imageUri));
        }
    }

    /** Loads the signed-in user's profile from their Firestore document. */
    private void loadUserFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String uid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String mobile = doc.getString("mobile");
                        String gender = doc.getString("gender");
                        String dob = doc.getString("dob");
                        String address = doc.getString("address");
                        inputName.setText(name != null ? name : "");
                        inputEmail.setText(email != null ? email : currentUser.getEmail());
                        inputMobile.setText(mobile != null ? mobile : "");
                        inputGender.setText(gender != null ? gender : "", false);
                        inputDob.setText(dob != null ? dob : "");
                        inputAddress.setText(address != null ? address : "");
                    } else {
                        // Fallback to Auth data if the Firestore doc is somehow missing.
                        inputEmail.setText(currentUser.getEmail());
                    }
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("PersonalDetails", "Failed to load profile", e));
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

    /** Persists the profile to Firestore. */
    private void saveProfile() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String mobile = inputMobile.getText().toString().trim();
        String gender = inputGender.getText().toString().trim();
        String dob = inputDob.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();

        // Mirror name/email so the Profile header stays in sync.
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("user_name", name)
                .putString("user_email", email)
                .apply();

        // Persist all editable fields to the user's Firestore document in one update.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("mobile", mobile);
            updates.put("gender", gender);
            updates.put("dob", dob);
            updates.put("address", address);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .update(updates)
                    .addOnFailureListener(e ->
                            android.util.Log.e("PersonalDetails", "Failed to update profile", e));
        }

        Snackbar.make(findViewById(android.R.id.content), getString(R.string.profile_saved), Snackbar.LENGTH_SHORT).show();
    }
}
