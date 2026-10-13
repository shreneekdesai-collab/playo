package com.example.playo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/** Lists the authenticated user's confirmed Firestore bookings. */
public class MyBookingsActivity extends AppCompatActivity {

    private ArrayList<Booking> bookings;
    private BookingAdapter adapter;
    private RecyclerView recycler;
    private TextView emptyView;
    private ProgressBar loadingView;

    @Override
    protected void onStop() {
        AnimUtils.cancelTree(recycler);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_booking);
        PressEffect.attachToButtons(findViewById(android.R.id.content));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        final int baseToolbarHeight = toolbar.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + systemBars.top;
            v.setLayoutParams(lp);
            return insets;
        });

        recycler = findViewById(R.id.bookingsRecycler);
        emptyView = findViewById(R.id.emptyView);
        loadingView = findViewById(R.id.loadingView);
        bookings = new ArrayList<>();
        adapter = new BookingAdapter(bookings, this::openBookingDetails);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        loadBookings();
    }

    private void loadBookings() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyState();
            return;
        }
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        recycler.setVisibility(View.GONE);
        FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    bookings.clear();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        bookings.add(mapBooking(document));
                    }
                    Collections.sort(bookings, Comparator.comparing(
                            Booking::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
                    adapter.notifyDataSetChanged();
                    AnimUtils.staggeredEntrance(recycler);
                    loadingView.setVisibility(View.GONE);
                    recycler.setVisibility(bookings.isEmpty() ? View.GONE : View.VISIBLE);
                    emptyView.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(error -> {
                    android.util.Log.e("MyBookingsActivity", "Failed to load bookings", error);
                    showEmptyState();
                });
    }

    private Booking mapBooking(DocumentSnapshot document) {
        Number amount = (Number) document.get("amount");
        return new Booking(
                value(document.getString("bookingId"), document.getId()),
                value(document.getString("userId"), ""),
                value(document.getString("userName"), "Player"),
                value(document.getString("turfId"), ""),
                value(document.getString("turfName"), "Turf"),
                value(document.getString("ownerId"), ""),
                value(document.getString("location"), ""),
                formatDate(document.getString("date")),
                value(document.getString("slotKey"), ""),
                value(document.getString("slotLabel"), ""),
                amount == null ? 0.0 : amount.doubleValue(),
                value(document.getString("paymentMethod"), ""),
                value(document.getString("paymentStatus"), ""),
                value(document.getString("status"), "confirmed"),
                document.getTimestamp("createdAt"), R.drawable.user);
    }

    private String formatDate(String date) {
        if (date == null) return "";
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            return parsed == null ? date : new SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed);
        } catch (Exception exception) {
            return date;
        }
    }

    private String value(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private void showEmptyState() {
        loadingView.setVisibility(View.GONE);
        recycler.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void openBookingDetails(Booking booking) {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("booking_id", booking.getBookingId());
        intent.putExtra("user_id", booking.getUserId());
        intent.putExtra("turf_id", booking.getTurfId());
        intent.putExtra("owner_id", booking.getOwnerId());
        intent.putExtra("turf_name", booking.getTurfName());
        intent.putExtra("location", booking.getLocation());
        intent.putExtra("date", booking.getDate());
        intent.putExtra("time", booking.getTimeSlot());
        intent.putExtra("status", booking.getStatus());
        intent.putExtra("payment_status", booking.getPaymentStatus());
        intent.putExtra("payment_method", booking.getPaymentMethod());
        intent.putExtra("amount", booking.getAmount());
        intent.putExtra("image", booking.getImageRes());
        startActivity(intent);
    }
}
