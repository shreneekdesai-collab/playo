package com.example.playo;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simple_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getScreenTitle());
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