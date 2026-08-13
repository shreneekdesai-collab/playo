package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Signup screen with email/password auth. On success it also stores the
 * user's profile (name, email, role) in Firestore under users/{uid}.
 */
public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button playerButton;
    private Button ownerButton;
    private Button signUpButton;
    private ProgressBar progressBar;
    private String selectedRole = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Reserve the system bar insets for the edge-to-edge layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        playerButton = findViewById(R.id.btnRolePlayer);
        ownerButton = findViewById(R.id.btnRoleOwner);
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.signupProgress);

        clearErrorOnChange(nameLayout, nameInput);
        clearErrorOnChange(emailLayout, emailInput);
        clearErrorOnChange(passwordLayout, passwordInput);
        clearErrorOnChange(confirmPasswordLayout, confirmPasswordInput);

        // Role selector: selecting one always deselects the other.
        playerButton.setOnClickListener(v -> selectRole("customer"));
        ownerButton.setOnClickListener(v -> selectRole("owner"));
        selectRole("customer");

        signUpButton.setOnClickListener(v -> attemptSignup());

        findViewById(R.id.loginLink).setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
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

    private void attemptSignup() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirm = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString().trim() : "";

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError("Name is required");
            valid = false;
        }
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email");
            valid = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }
        if (confirm.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            valid = false;
        } else if (!confirm.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
            valid = false;
        }
        if (!valid) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signUpButton.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        // Save additional profile info (name, role) to Firestore.
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("name", name);
                        userProfile.put("email", email);
                        userProfile.put("role", selectedRole); // "customer" or "owner"
                        userProfile.put("createdAt", FieldValue.serverTimestamp());

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(userProfile)
                                .addOnSuccessListener(unused -> {
                                    progressBar.setVisibility(View.GONE);
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    signUpButton.setEnabled(true);
                                    Toast.makeText(this, "Profile save failed: " +
                                            e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        signUpButton.setEnabled(true);
                        Toast.makeText(this, "Signup failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Clears the inline error on a field as soon as the user starts typing again. */
    private void clearErrorOnChange(TextInputLayout layout, TextInputEditText input) {
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
}