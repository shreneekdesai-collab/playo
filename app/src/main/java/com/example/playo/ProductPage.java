package com.example.playo;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays the details of a single turf, with book-now / review actions,
 * a swipeable image gallery (ViewPager2), and bottom navigation.
 */
public class ProductPage extends AppCompatActivity {

    @Override
    protected void onDestroy() {
        AnimUtils.cancelTree(findViewById(R.id.productScrollView));
        super.onDestroy();
    }

    private String currentName;
    private String currentPrice;
    private String currentTurfId;
    private String turfOwnerId;
    private Double currentLatitude;
    private Double currentLongitude;
    private String currentLocationText = "";
    private double currentPriceAmount;
    private byte[] currentThumbnailBytes;
    private TextInputEditText reviewComment;
    private RatingBar userRating;
    private Button submitReviewButton;
    private ProgressBar reviewProgress;
    private ProgressBar reviewsLoading;
    private TextView reviewSummary;
    private TextView reviewsEmpty;
    private final ArrayList<Review> reviews = new ArrayList<>();
    private ReviewAdapter reviewAdapter;
    private boolean pendingReviewEntrance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.product_page);
        PressEffect.attachToButtons(findViewById(android.R.id.content));
        PressEffect.attach(findViewById(R.id.btnOpenMap));
        setupMapButton();

        // --- Status bar: light icons for dark hero image ---
        setupStatusBar();

        // --- Static toolbar with back arrow ---
        Toolbar toolbar = findViewById(R.id.productToolbar);
        if (toolbar != null) {
            Drawable navIcon = ContextCompat.getDrawable(this, R.drawable.ic_back);
            if (navIcon != null) {
                navIcon.setColorFilter(ContextCompat.getColor(this, R.color.white),
                        PorterDuff.Mode.SRC_ATOP);
                toolbar.setNavigationIcon(navIcon);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());

            // --- Edge-to-edge: grow the toolbar by the status-bar height so its
            //     content never merges with the status bar. The listener is
            //     attached to the coordinator root (not the toolbar) because the
            //     AppBarLayout/CollapsingToolbarLayout consume insets before the
            //     toolbar is reached. ---
            final int baseToolbarHeight = toolbar.getLayoutParams().height;
            NestedScrollView scrollView = findViewById(R.id.productScrollView);
            BottomNavigationView bottomView = findViewById(R.id.bottomView);
            final int baseScrollBottomPadding = scrollView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(
                    findViewById(R.id.coordinatorRoot), (v, insets) -> {
                        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        int topInset = systemBars.top;
                        boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                        int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                        int navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                        Log.d("ProductPage", "WindowInsets topInset=" + topInset);
                        View toolbarView = findViewById(R.id.productToolbar);
                        if (toolbarView != null) {
                            toolbarView.setPadding(toolbarView.getPaddingLeft(), topInset,
                                    toolbarView.getPaddingRight(), toolbarView.getPaddingBottom());
                            ViewGroup.LayoutParams lp = toolbarView.getLayoutParams();
                            lp.height = baseToolbarHeight + topInset;
                            toolbarView.setLayoutParams(lp);
                        }
                        if (imeVisible) {
                            BottomNavHelper.hideImmediately(bottomView);
                        } else {
                            BottomNavHelper.show(bottomView);
                        }
                        int extraBottomPadding = imeVisible ? Math.max(imeBottom, navBottom) : 0;
                        scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                                scrollView.getPaddingRight(), baseScrollBottomPadding + extraBottomPadding);
                        return insets;
                    });
        }

        // --- Flip status-bar/back-arrow icons when the app bar collapses:
        //     white over the dark hero image, dark over the light toolbar scrim ---
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null && toolbar != null) {
            appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
                boolean collapsed = Math.abs(verticalOffset)
                        >= appBar.getHeight() - appBar.getTotalScrollRange();
                applyTopBarStyle(collapsed);
            });
        }

        // --- Read details passed from the list ---
        Intent intent = getIntent();
        String turfId = intent.getStringExtra("TURF_ID");

        // --- Book now ---
        Button btnBookNow = findViewById(R.id.btnBookNow);
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                if (currentTurfId == null || currentTurfId.trim().isEmpty()) {
                    Toast.makeText(ProductPage.this, "This turf cannot be booked yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null && currentUser.getUid().equals(turfOwnerId)) {
                    Toast.makeText(ProductPage.this, "You cannot book your own turf", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent bookIntent = new Intent(ProductPage.this, BookSlotActivity.class);
                bookIntent.putExtra("TURF_ID", currentTurfId);
                bookIntent.putExtra("TURF_NAME", currentName);
                bookIntent.putExtra("TURF_PRICE", currentPrice);
                bookIntent.putExtra("TURF_PRICE_AMOUNT", currentPriceAmount);
                bookIntent.putExtra("OWNER_ID", turfOwnerId);
                if (currentThumbnailBytes != null) {
                    bookIntent.putExtra("TURF_THUMBNAIL", currentThumbnailBytes);
                }
                startActivity(bookIntent);
            });
        }

        // --- Bottom navigation (Profile always reachable) ---
        setupBottomNavigation();

        if (turfId != null) {
            currentTurfId = turfId;
            setupReviewSection(turfId);
            loadTurfFromFirestore(turfId);
        } else {
            View reviewSection = findViewById(R.id.reviewSection);
            if (reviewSection != null) {
                reviewSection.setVisibility(View.GONE);
            }
            applyPlaceholderData(intent);
        }
    }

    private void setupReviewSection(String turfId) {
        reviewComment = findViewById(R.id.editReview);
        userRating = findViewById(R.id.userRating);
        submitReviewButton = findViewById(R.id.btnSubmitReview);
        reviewProgress = findViewById(R.id.reviewProgress);
        reviewsLoading = findViewById(R.id.reviewsLoading);
        reviewSummary = findViewById(R.id.reviewSummary);
        reviewsEmpty = findViewById(R.id.reviewsEmpty);
        userRating.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                AnimUtils.pulse(bar);
            }
        });

        RecyclerView reviewsRecycler = findViewById(R.id.reviewsRecycler);
        reviewsRecycler.setNestedScrollingEnabled(false);
        reviewAdapter = new ReviewAdapter(reviews, getCurrentUserId());
        reviewsRecycler.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecycler.setAdapter(reviewAdapter);

        NestedScrollView scrollView = findViewById(R.id.productScrollView);
        reviewComment.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.post(() -> scrollView.smoothScrollTo(0, reviewComment.getBottom()));
            }
        });

        submitReviewButton.setOnClickListener(v -> submitReview(turfId));
        loadReviews(turfId);
        loadExistingReview(turfId);
    }

    /** Populates the page from the old Intent-extra placeholder path. */
    private void applyPlaceholderData(Intent intent) {
        String name = intent.getStringExtra("TURF_NAME");
        String price = intent.getStringExtra("TURF_PRICE");
        int imageRes = intent.getIntExtra("TURF_IMAGE", R.mipmap.ic_launcher);
        float rating = intent.getFloatExtra("TURF_RATING", 0.0f);

        currentName = name;
        currentPrice = price;
        currentPriceAmount = parsePriceAmount(price);
        currentThumbnailBytes = intent.getByteArrayExtra("TURF_THUMBNAIL");
        String location = intent.getStringExtra("TURF_LOCATION");

        TextView nameView = findViewById(R.id.turfname);
        TextView priceView = findViewById(R.id.price);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        TextView addressView = findViewById(R.id.address);

        nameView.setText(name);
        priceView.setText(price);
        ratingBar.setRating(rating);
        if (location != null) {
            addressView.setText(location);
        }
        currentLocationText = addressView.getText() == null ? ""
                : addressView.getText().toString().trim();
        updateMapButtonVisibility();
        animateDetailCard();

        setupImageGallery(imageRes);
    }

    /** Loads a real turf document from Firestore and populates the page. */
    private void loadTurfFromFirestore(String turfId) {
        FirebaseFirestore.getInstance()
                .collection("turfs")
                .document(turfId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || !doc.exists()) {
                        return;
                    }
                    String name = doc.getString("name");
                    String location = doc.getString("location");
                    Double price = doc.getDouble("price");
                    Double rating = doc.getDouble("rating");
                    currentLatitude = doc.getDouble("latitude");
                    currentLongitude = doc.getDouble("longitude");
                    String description = doc.getString("description");
                    List<String> imageUrls = (List<String>) doc.get("imageUrls");
                    Blob thumbnail = doc.getBlob("thumbnail");
                    turfOwnerId = doc.getString("ownerId");

                    currentName = name;
                    currentPrice = "₹ " + (int) (price != null ? price : 0.0) + " / Hour";
                    currentPriceAmount = price != null ? price : 0.0;
                    currentThumbnailBytes = thumbnail == null ? null : thumbnail.toBytes();

                    TextView nameView = findViewById(R.id.turfname);
                    TextView addressView = findViewById(R.id.address);
                    TextView priceView = findViewById(R.id.price);
                    TextView descriptionView = findViewById(R.id.description);
                    RatingBar ratingBar = findViewById(R.id.ratingBar);

                    nameView.setText(name != null ? name : "Unnamed Turf");
                    addressView.setText(location != null ? location : "");
                    currentLocationText = location == null ? "" : location.trim();
                    updateMapButtonVisibility();
                    priceView.setText(currentPrice);
                    ratingBar.setRating(rating != null ? rating.floatValue() : 0.0f);
                     descriptionView.setText(description != null ? description : "");
                     updateReviewSummary(rating, doc.getLong("reviewCount"));
                     animateDetailCard();

                    loadPhotoGallery(turfId, imageUrls, thumbnail);
                })
                .addOnFailureListener(e ->
                        Log.e("ProductPage", "Failed to load turf", e));
    }

    private void loadExistingReview(String turfId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }
        FirebaseFirestore.getInstance().collection("turfs").document(turfId)
                .collection("reviews").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || !doc.exists()) {
                        return;
                    }
                    Number rating = (Number) doc.get("rating");
                    if (rating != null) {
                        userRating.setRating(rating.floatValue());
                    }
                    String comment = doc.getString("comment");
                    reviewComment.setText(comment != null ? comment : "");
                    submitReviewButton.setText("UPDATE REVIEW");
                })
                .addOnFailureListener(e -> Log.e("ProductPage", "Failed to load user review", e));
    }

    private void submitReview(String turfId) {
        if (isFinishing()) {
            return;
        }
        if (userRating.getRating() < 1) {
            showReviewMessage("Please select a rating");
            return;
        }
        String comment = reviewComment.getText() != null
                ? reviewComment.getText().toString().trim() : "";
        if (comment.length() > 500) {
            showReviewMessage("Comment must be 500 characters or less");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showReviewMessage("Please sign in to review");
            return;
        }
        if (user.getUid().equals(turfOwnerId)) {
            showReviewMessage("You cannot review your own turf");
            return;
        }

        setReviewSaving(true);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (isFinishing()) {
                        return;
                    }
                    String userName = userDoc.getString("name");
                    saveReview(turfId, user.getUid(),
                            userName == null || userName.trim().isEmpty() ? "Player" : userName,
                            userRating.getRating(), comment);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductPage", "Failed to load reviewer profile", e);
                    if (!isFinishing()) {
                        setReviewSaving(false);
                        showReviewMessage("Review could not be posted. Please try again.");
                    }
                });
    }

    private void saveReview(String turfId, String userId, String userName,
                            float rating, String comment) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.DocumentReference turfRef = firestore.collection("turfs")
                .document(turfId);
        com.google.firebase.firestore.DocumentReference reviewRef = turfRef.collection("reviews")
                .document(userId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot turf = transaction.get(turfRef);
            DocumentSnapshot oldReview = transaction.get(reviewRef);
            Number oldRating = oldReview.exists() ? (Number) oldReview.get("rating") : null;
            Number oldAverage = (Number) turf.get("rating");
            Number oldCount = (Number) turf.get("reviewCount");
            long count = oldCount == null ? 0L : oldCount.longValue();
            double average = oldAverage == null ? 0.0 : oldAverage.doubleValue();
            double total = average * count;
            if (oldRating == null) {
                count++;
            } else {
                total -= oldRating.doubleValue();
            }
            total += rating;

            Map<String, Object> review = new HashMap<>();
            review.put("userId", userId);
            review.put("userName", userName);
            review.put("rating", rating);
            review.put("comment", comment);
            review.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            transaction.set(reviewRef, review);

            Map<String, Object> aggregate = new HashMap<>();
            aggregate.put("rating", count == 0 ? 0.0 : total / count);
            aggregate.put("reviewCount", count);
            transaction.update(turfRef, aggregate);
            return null;
        }).addOnSuccessListener(unused -> {
            if (isFinishing()) {
                return;
            }
            setReviewSaving(false);
            reviewComment.setText("");
            submitReviewButton.setText("UPDATE REVIEW");
            showReviewMessage("Review posted");
            pendingReviewEntrance = true;
            loadReviews(turfId);
            reloadReviewSummary(turfId);
        }).addOnFailureListener(e -> {
            Log.e("ProductPage", "Failed to save review", e);
            if (!isFinishing()) {
                setReviewSaving(false);
                showReviewMessage("Review could not be posted. Please try again.");
            }
        });
    }

    private void loadReviews(String turfId) {
        if (isFinishing()) {
            return;
        }
        reviewsLoading.setVisibility(View.VISIBLE);
        reviewsEmpty.setVisibility(View.GONE);
        RecyclerView reviewsRecycler = findViewById(R.id.reviewsRecycler);
        FirebaseFirestore.getInstance().collection("turfs").document(turfId)
                .collection("reviews").orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50).get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing()) {
                        return;
                    }
                    reviews.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Number rating = (Number) doc.get("rating");
                        reviews.add(new Review(doc.getString("userId"),
                                valueOrPlayer(doc.getString("userName")),
                                rating == null ? 0.0f : rating.floatValue(),
                                doc.getString("comment"), doc.getTimestamp("createdAt")));
                    }
                    reviewsLoading.setVisibility(View.GONE);
                     reviewsEmpty.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                     reviewAdapter.notifyDataSetChanged();
                     AnimUtils.staggeredEntrance(reviewsRecycler);
                     if (pendingReviewEntrance) {
                         pendingReviewEntrance = false;
                         reviewsRecycler.post(() -> {
                             if (!isFinishing() && reviewsRecycler.getChildCount() > 0) {
                                 AnimUtils.slideUpFadeIn(reviewsRecycler.getChildAt(0), 0L);
                             }
                         });
                     }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductPage", "Failed to load reviews", e);
                    if (!isFinishing()) {
                        reviewsLoading.setVisibility(View.GONE);
                        reviewsEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void reloadReviewSummary(String turfId) {
        FirebaseFirestore.getInstance().collection("turfs").document(turfId).get()
                .addOnSuccessListener(doc -> {
                    if (!isFinishing()) {
                        updateReviewSummary((Number) doc.get("rating"), doc.getLong("reviewCount"));
                    }
                })
                .addOnFailureListener(e -> Log.e("ProductPage", "Failed to reload review summary", e));
    }

    private void updateReviewSummary(Number average, Long count) {
        if (reviewSummary == null) {
            return;
        }
        double value = average == null ? 0.0 : average.doubleValue();
        long reviewCount = count == null ? 0L : count;
        reviewSummary.setText(String.format(Locale.US, "%.1f (%d reviews)", value, reviewCount));
    }

    private void setReviewSaving(boolean saving) {
        submitReviewButton.setEnabled(!saving);
        reviewProgress.setVisibility(saving ? View.VISIBLE : View.GONE);
        if (saving) {
            submitReviewButton.setText("SAVING...");
        }
    }

    private void showReviewMessage(String message) {
        if (!isFinishing()) {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : user.getUid();
    }

    private String valueOrPlayer(String value) {
        return value == null || value.trim().isEmpty() ? "Player" : value;
    }

    private double parsePriceAmount(String price) {
        if (price == null) {
            return 0.0;
        }
        String numeric = price.replaceAll("[^0-9.]", "");
        try {
            return numeric.isEmpty() ? 0.0 : Double.parseDouble(numeric);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private void loadPhotoGallery(String turfId, List<String> legacyUrls, Blob thumbnail) {
        FirebaseFirestore.getInstance().collection("turfs").document(turfId)
                .collection("photos").orderBy("index")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing()) {
                        return;
                    }
                    List<byte[]> photos = new ArrayList<>();
                    for (DocumentSnapshot photo : snapshot.getDocuments()) {
                        Blob data = photo.getBlob("data");
                        if (data != null) {
                            photos.add(data.toBytes());
                        }
                    }
                    if (!photos.isEmpty()) {
                        setupImageGalleryBytes(photos);
                    } else if (legacyUrls != null && !legacyUrls.isEmpty()) {
                        setupImageGallery(legacyUrls);
                    } else if (thumbnail != null) {
                        photos.add(thumbnail.toBytes());
                        setupImageGalleryBytes(photos);
                    } else {
                        setupImageGallery(R.drawable.user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) {
                        return;
                    }
                    if (legacyUrls != null && !legacyUrls.isEmpty()) {
                        setupImageGallery(legacyUrls);
                    } else if (thumbnail != null) {
                        List<byte[]> photos = new ArrayList<>();
                        photos.add(thumbnail.toBytes());
                        setupImageGalleryBytes(photos);
                    } else {
                        setupImageGallery(R.drawable.user);
                    }
                });
    }

    private void animateDetailCard() {
        View detailCard = findViewById(R.id.reviewSection);
        if (detailCard == null) {
            return;
        }
        AnimUtils.slideUpFadeIn(detailCard, 0L);
    }

    private void configureGalleryMotion(ViewPager2 pager) {
        pager.setPageTransformer((page, position) -> {
            if (!Motion.enabled(ProductPage.this)) {
                page.setAlpha(1f);
                page.setScaleX(1f);
                page.setScaleY(1f);
                return;
            }
            float distance = Math.min(1f, Math.abs(position));
            float scale = 1f - (0.08f * distance);
            page.setAlpha(1f - (0.3f * distance));
            page.setScaleX(scale);
            page.setScaleY(scale);
        });
    }

    /** Sets up the ViewPager2 image gallery with dots indicator. */
    private void setupImageGallery(int primaryImageRes) {
        ViewPager2 pager = findViewById(R.id.productImagePager);
        TabLayout indicator = findViewById(R.id.imageIndicator);
        configureGalleryMotion(pager);

        List<Integer> images = new ArrayList<>();
        images.add(primaryImageRes);

        ProductImagePagerAdapter adapter = new ProductImagePagerAdapter(images, true);
        pager.setAdapter(adapter);
        configureImageIndicator(indicator, images.size());
        if (images.size() > 1) {
            new TabLayoutMediator(indicator, pager, (tab, position) -> {
            }).attach();
        }
    }

    /** Sets up the ViewPager2 gallery from a list of remote image URLs. */
    private void setupImageGallery(List<String> imageUrls) {
        ViewPager2 pager = findViewById(R.id.productImagePager);
        TabLayout indicator = findViewById(R.id.imageIndicator);
        configureGalleryMotion(pager);

        ProductImagePagerAdapter adapter = new ProductImagePagerAdapter(imageUrls);
        pager.setAdapter(adapter);
        configureImageIndicator(indicator, imageUrls.size());
        if (imageUrls.size() > 1) {
            new TabLayoutMediator(indicator, pager, (tab, position) -> {
            }).attach();
        }
    }

    private void setupImageGalleryBytes(List<byte[]> photos) {
        ViewPager2 pager = findViewById(R.id.productImagePager);
        TabLayout indicator = findViewById(R.id.imageIndicator);
        configureGalleryMotion(pager);
        ProductImagePagerAdapter adapter = new ProductImagePagerAdapter(photos);
        pager.setAdapter(adapter);
        configureImageIndicator(indicator, photos.size());
        if (photos.size() > 1) {
            new TabLayoutMediator(indicator, pager, (tab, position) -> {}).attach();
        }
    }

    private void configureImageIndicator(TabLayout indicator, int imageCount) {
        boolean showIndicator = imageCount > 1;
        indicator.setVisibility(showIndicator ? View.VISIBLE : View.GONE);
        indicator.setClickable(false);
        indicator.setFocusable(false);
    }

    private void setupMapButton() {
        View touchTarget = findViewById(R.id.mapButtonTouchTarget);
        View button = findViewById(R.id.btnOpenMap);
        View.OnClickListener openMapListener = v -> openLocationInMaps();
        button.setOnClickListener(openMapListener);
        touchTarget.setOnClickListener(v -> button.performClick());
        updateMapButtonVisibility();
    }

    private void updateMapButtonVisibility() {
        View touchTarget = findViewById(R.id.mapButtonTouchTarget);
        if (touchTarget == null) {
            return;
        }
        boolean hasCoordinates = currentLatitude != null && currentLongitude != null;
        boolean hasText = currentLocationText != null && !currentLocationText.trim().isEmpty();
        touchTarget.setVisibility(hasCoordinates || hasText ? View.VISIBLE : View.GONE);
    }

    private void openLocationInMaps() {
        boolean hasCoordinates = currentLatitude != null && currentLongitude != null;
        String locationText = currentLocationText == null ? "" : currentLocationText.trim();
        if (!hasCoordinates && locationText.isEmpty()) {
            return;
        }

        String query;
        String geoUri;
        if (hasCoordinates) {
            String coordinates = String.format(Locale.US, "%.7f,%.7f",
                    currentLatitude, currentLongitude);
            String turfName = currentName == null || currentName.trim().isEmpty()
                    ? "Turf" : currentName.trim();
            query = coordinates;
            geoUri = "geo:" + coordinates + "?q=" + coordinates
                    + "(" + Uri.encode(turfName) + ")";
        } else {
            query = locationText;
            geoUri = "geo:0,0?q=" + Uri.encode(locationText);
        }

        Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        mapsIntent.setPackage("com.google.android.apps.maps");
        try {
            if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapsIntent);
                return;
            }
        } catch (ActivityNotFoundException exception) {
            Log.e("ProductPage", "Google Maps could not open", exception);
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query="
                        + Uri.encode(query)));
        try {
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            }
        } catch (ActivityNotFoundException exception) {
            Log.e("ProductPage", "No map app or browser could open location", exception);
        }
    }

    private void setupStatusBar() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean darkMode = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(!darkMode);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    /** Applies the correct status-bar/back-arrow icon style for the app bar state.
     *  expanded=true (over the dark hero image)  -> white icons.
     *  collapsed=true (over the light toolbar)   -> dark icons. */
    private void applyTopBarStyle(boolean collapsed) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean darkMode = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if (controller != null) {
            controller.setAppearanceLightStatusBars(collapsed && !darkMode);
            controller.setAppearanceLightNavigationBars(!darkMode);
        }
        Toolbar toolbar = findViewById(R.id.productToolbar);
        if (toolbar != null) {
            Drawable navIcon = toolbar.getNavigationIcon();
            if (navIcon != null) {
                int iconColor = collapsed && !darkMode
                        ? R.color.text_primary : R.color.white;
                navIcon.setColorFilter(ContextCompat.getColor(this, iconColor),
                        PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    /** Sets up the bottom navigation to switch to other main tabs. */
    private void setupBottomNavigation() {
        BottomNavigationView bottomView = findViewById(R.id.bottomView);
        if (bottomView == null) {
            return;
        }
        BottomNavHelper.setup(this, bottomView, R.id.Category);
    }
}
