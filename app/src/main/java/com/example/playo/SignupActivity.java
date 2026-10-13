package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Signup screen with email/password auth. On success it also stores the
 * user's profile (name, email, role, mobile, gender, dob, address) in
 * Firestore under users/{uid}.
 */
public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout mobileLayout;
    private TextInputLayout genderLayout;
    private TextInputLayout dobLayout;
    private TextInputLayout addressLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText mobileInput;
    private AutoCompleteTextView genderInput;
    private TextInputEditText dobInput;
    private TextInputEditText addressInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button playerButton;
    private Button ownerButton;
    private Button googleSignInButton;
    private Button signUpButton;
    private ProgressBar progressBar;
    private String selectedRole = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        // Reserve the system bar insets for the edge-to-edge layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        mobileLayout = findViewById(R.id.signupMobileLayout);
        genderLayout = findViewById(R.id.signupGenderLayout);
        dobLayout = findViewById(R.id.signupDobLayout);
        addressLayout = findViewById(R.id.signupAddressLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        mobileInput = findViewById(R.id.signupMobileInput);
        genderInput = findViewById(R.id.signupGenderInput);
        dobInput = findViewById(R.id.signupDobInput);
        addressInput = findViewById(R.id.signupAddressInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        playerButton = findViewById(R.id.btnRolePlayer);
        ownerButton = findViewById(R.id.btnRoleOwner);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        playerButton.setBackground(playerButton.getBackground().mutate());
        ownerButton.setBackground(ownerButton.getBackground().mutate());
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.signupProgress);

        clearErrorOnChange(nameLayout, nameInput);
        clearErrorOnChange(emailLayout, emailInput);
        clearErrorOnChange(mobileLayout, mobileInput);
        clearErrorOnChange(genderLayout, genderInput);
        clearErrorOnChange(dobLayout, dobInput);
        clearErrorOnChange(addressLayout, addressInput);
        clearErrorOnChange(passwordLayout, passwordInput);
        clearErrorOnChange(confirmPasswordLayout, confirmPasswordInput);

        setupGenderDropdown();
        setupDatePicker();

        // Role selector: selecting one always deselects the other.
        playerButton.setOnClickListener(v -> selectRole("customer"));
        ownerButton.setOnClickListener(v -> selectRole("owner"));
        selectRole("customer");

        signUpButton.setOnClickListener(v -> attemptSignup());
        googleSignInButton.setOnClickListener(v -> attemptGoogleSignIn());

        findViewById(R.id.loginLink).setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
        animateForm();
    }

    private void attemptGoogleSignIn() {
        googleSignInButton.setEnabled(false);
        setSubmitting(true, googleSignInButton);
        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.Callback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                ensureGoogleProfile(user);
            }

            @Override
            public void onFailure(String message, boolean cancelled) {
                resetGoogleSignIn();
                if (!cancelled) {
                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void ensureGoogleProfile(FirebaseUser user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        openMainAfterGoogleSignIn();
                        return;
                    }
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", user.getDisplayName() == null ? "" : user.getDisplayName());
                    profile.put("email", user.getEmail() == null ? "" : user.getEmail());
                    profile.put("role", selectedRole);
                    profile.put("mobile", "");
                    profile.put("gender", "");
                    profile.put("dob", "");
                    profile.put("address", "");
                    profile.put("phoneVerified", false);
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    firestore.collection("users").document(user.getUid()).set(profile)
                            .addOnSuccessListener(unused -> openMainAfterGoogleSignIn())
                            .addOnFailureListener(exception -> handleGoogleProfileFailure(exception));
                })
                .addOnFailureListener(exception -> handleGoogleProfileFailure(exception));
    }

    private void openMainAfterGoogleSignIn() {
        resetGoogleSignIn();
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }

    private void handleGoogleProfileFailure(Exception exception) {
        Log.e("SignupActivity", "Failed to load or create Google profile", exception);
        ErrorLogger.log(this, "Signup", "google_sign_in", exception);
        resetGoogleSignIn();
        Toast.makeText(this, "Google sign-in failed. Please try again", Toast.LENGTH_LONG).show();
    }

    private void resetGoogleSignIn() {
        setSubmitting(false, googleSignInButton);
        googleSignInButton.setEnabled(true);
    }

    /**
     * Sets the selected role and updates both buttons so exactly one of them
     * is highlighted at any time.
     */
    private void selectRole(String role) {
        selectedRole = role; // "customer" or "owner"
        boolean isPlayer = role.equals("customer");

        playerButton.setSelected(isPlayer);
        ownerButton.setSelected(!isPlayer);
    }

    /** Populates the gender dropdown options. */
    private void setupGenderDropdown() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, genders);
        genderInput.setAdapter(adapter);
    }

    /** Opens a date picker when the DOB field is tapped. */
    private void setupDatePicker() {
        dobInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Date date = new Date(selection);
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                dobInput.setText(formatted);
            });
            picker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void attemptSignup() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String mobile = mobileInput.getText() != null ? mobileInput.getText().toString().trim() : "";
        String gender = genderInput.getText() != null ? genderInput.getText().toString().trim() : "";
        String dob = dobInput.getText() != null ? dobInput.getText().toString().trim() : "";
        String address = addressInput.getText() != null ? addressInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirm = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString().trim() : "";

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError("Name is required");
            AnimUtils.shake(nameLayout);
            valid = false;
        }
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            AnimUtils.shake(emailLayout);
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            AnimUtils.shake(emailLayout);
            valid = false;
        }
        if (mobile.isEmpty()) {
            mobileLayout.setError("Mobile number is required");
            AnimUtils.shake(mobileLayout);
            valid = false;
        } else if (mobile.length() < 10 || mobile.length() > 15) {
            mobileLayout.setError("Please enter a valid mobile number");
            AnimUtils.shake(mobileLayout);
            valid = false;
        }
        if (gender.isEmpty()) {
            genderLayout.setError("Please select a gender");
            AnimUtils.shake(genderLayout);
            valid = false;
        }
        if (dob.isEmpty()) {
            dobLayout.setError("Please select your date of birth");
            AnimUtils.shake(dobLayout);
            valid = false;
        }
        if (address.isEmpty()) {
            addressLayout.setError("Address is required");
            AnimUtils.shake(addressLayout);
            valid = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            AnimUtils.shake(passwordLayout);
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            AnimUtils.shake(passwordLayout);
            valid = false;
        }
        if (confirm.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            AnimUtils.shake(confirmPasswordLayout);
            valid = false;
        } else if (!confirm.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
            AnimUtils.shake(confirmPasswordLayout);
            valid = false;
        }
        if (!valid) {
            return;
        }

        setSubmitting(true, signUpButton);
        signUpButton.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        // Save additional profile info to Firestore.
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("name", name);
                        userProfile.put("email", email);
                        userProfile.put("mobile", mobile);
                        userProfile.put("gender", gender);
                        userProfile.put("dob", dob);
                        userProfile.put("address", address);
                        userProfile.put("role", selectedRole); // "customer" or "owner"
                        userProfile.put("createdAt", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(userProfile)
                                .addOnSuccessListener(unused -> {
                                     setSubmitting(false, signUpButton);
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                })
                                 .addOnFailureListener(e -> {
                                      setSubmitting(false, signUpButton);
                                     signUpButton.setEnabled(true);
                                     ErrorLogger.log(this, "Signup", "save_profile", e);
                                     Toast.makeText(this, "Profile save failed: " +
                                            e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        ErrorLogger.log(this, "Signup", "email_signup", task.getException());
                        setSubmitting(false, signUpButton);
                        signUpButton.setEnabled(true);
                        Toast.makeText(this, "Signup failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Clears the inline error on a field as soon as the user starts typing again. */
    private void clearErrorOnChange(TextInputLayout layout, android.widget.EditText input) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void animateForm() {
        View[] fields = {nameLayout, emailLayout, mobileLayout, genderLayout,
                dobLayout, addressLayout, passwordLayout, confirmPasswordLayout,
                playerButton, ownerButton, signUpButton, googleSignInButton};
        for (int index = 0; index < fields.length; index++) {
            AnimUtils.slideUpFadeIn(fields[index], index * Motion.STAGGER_STEP);
        }
    }

    private void setSubmitting(boolean submitting, View button) {
        if (!Motion.enabled(this)) {
            progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
            button.setAlpha(1f);
            return;
        }
        if (submitting) {
            AnimUtils.fadeIn(progressBar);
            button.animate().alpha(0f).setDuration(Motion.DURATION_SHORT)
                    .setInterpolator(Motion.EASE_OUT).start();
        } else {
            AnimUtils.fadeOut(progressBar);
            button.setAlpha(0f);
            button.animate().alpha(1f).setDuration(Motion.DURATION_SHORT)
                    .setInterpolator(Motion.EASE_OUT).start();
        }
    }

    @Override
    protected void onDestroy() {
        AnimUtils.cancelTree(findViewById(R.id.signupRoot));
        super.onDestroy();
    }
}
