package com.example.playo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;

/**
 * Lists all bookings made by the user. Each row opens BookingDetailsActivity.
 */
public class MyBookingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_booking);

        // --- Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- Edge-to-edge: keep the toolbar below the status bar ---
        View root = findViewById(android.R.id.content);
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

        RecyclerView recycler = findViewById(R.id.bookingsRecycler);
        TextView emptyView = findViewById(R.id.emptyView);

        // --- Build sample data ---
        ArrayList<Booking> bookings = buildSampleBookings();

        BookingAdapter adapter = new BookingAdapter(bookings, this::openBookingDetails);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        emptyView.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);

        // Sync booking count into the shared header stats.
        SharedPreferences prefs = getSharedPreferences("playo_prefs", MODE_PRIVATE);
        prefs.edit().putInt("booking_count", bookings.size()).apply();
    }

    /** Launches the details screen for the tapped booking. */
    private void openBookingDetails(Booking booking) {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("turf_name", booking.getTurfName());
        intent.putExtra("location", booking.getLocation());
        intent.putExtra("date", booking.getDate());
        intent.putExtra("time", booking.getTimeSlot());
        intent.putExtra("status", booking.getStatus());
        intent.putExtra("amount", booking.getAmount());
        intent.putExtra("image", booking.getImageRes());
        startActivity(intent);
    }

    /** Provides sample bookings to render the list (replace with a data source later). */
    private ArrayList<Booking> buildSampleBookings() {
        ArrayList<Booking> list = new ArrayList<>();
        list.add(new Booking("Green Field Turf", "Sector 21, Pune",
                "05 Aug 2026", "06:00 - 07:00 PM", "Confirmed", 1500, R.drawable.user));
        list.add(new Booking("Striker Football Ground", "Kothrud, Pune",
                "03 Aug 2026", "08:00 - 09:00 AM", "Pending", 1200, R.drawable.user));
        list.add(new Booking("Smash Badminton Court", "Baner, Pune",
                "28 Jul 2026", "07:00 - 08:00 PM", "Cancelled", 800, R.drawable.user));
        return list;
    }
}