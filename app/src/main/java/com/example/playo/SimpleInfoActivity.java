package com.example.playo;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Base screen for lightweight destinations (Favorites, Help, About, ...).
 * Subclasses override {@link #getScreenTitle()}, {@link #getScreenMessage()}
 * and {@link #getScreenIcon()} to customise the content.
 */
public abstract class SimpleInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getScreenTitle());
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView simpleIcon = findViewById(R.id.simpleIcon);
        TextView simpleMessage = findViewById(R.id.simpleMessage);
        simpleIcon.setImageResource(getScreenIcon());
        simpleMessage.setText(getScreenMessage());
    }

    /** Provide the toolbar title. */
    protected abstract String getScreenTitle();

    /** Provide the centred message body. */
    protected abstract String getScreenMessage();

    /** Provide a representative icon resource. */
    protected int getScreenIcon() {
        return R.drawable.ic_info;
    }
}