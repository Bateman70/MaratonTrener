package com.jostein.maratontrener;

import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainContainerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private ImageView parallaxBackground;
    private Matrix matrix = new Matrix();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        parallaxBackground = findViewById(R.id.parallaxBackground);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Sync BottomNav with ViewPager
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0, true);
            else if (id == R.id.nav_buddies) viewPager.setCurrentItem(1, true);
            else if (id == R.id.nav_log) viewPager.setCurrentItem(2, true);
            else if (id == R.id.nav_stats) viewPager.setCurrentItem(3, true);
            else if (id == R.id.nav_profile) viewPager.setCurrentItem(4, true);
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updateBackgroundParallax(position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomNavigationView.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomNavigationView.setSelectedItemId(R.id.nav_buddies); break;
                    case 2: bottomNavigationView.setSelectedItemId(R.id.nav_log); break;
                    case 3: bottomNavigationView.setSelectedItemId(R.id.nav_stats); break;
                    case 4: bottomNavigationView.setSelectedItemId(R.id.nav_profile); break;
                }
            }
        });

        // Set default page
        int startTab = getIntent().getIntExtra("SELECT_TAB", R.id.nav_home);
        bottomNavigationView.setSelectedItemId(startTab);
        
        // Ensure background is initialized once layout is done
        parallaxBackground.post(() -> updateBackgroundParallax(viewPager.getCurrentItem(), 0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
    }

    private void updateBackgroundParallax(int position, float offset) {
        Drawable drawable = parallaxBackground.getDrawable();
        if (drawable == null) return;

        int viewWidth = parallaxBackground.getWidth();
        int viewHeight = parallaxBackground.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        // 1. Scale to fill height and add a width buffer
        float scale = (float) viewHeight / (float) drawableHeight;
        float scaledWidth = drawableWidth * scale;
        
        // We need extra width to move the image RIGHT without gaps.
        // We'll ensure the image is 40% wider than the screen.
        if (scaledWidth < viewWidth * 1.4f) {
            scale = (viewWidth * 1.4f) / drawableWidth;
            scaledWidth = drawableWidth * scale;
        }

        float maxScroll = scaledWidth - viewWidth;
        float progress = (position + offset) / 4.0f; // 5 pages total = 4 intervals
        if (progress > 1.0f) progress = 1.0f;

        // 2. Transform: Facing Right, Moving Right
        matrix.reset();
        matrix.postScale(scale, scale);
        
        // At Dashboard (progress 0), we shift the image LEFT (-maxScroll)
        // so that the runner (who is on the left of the photo) is on the LEFT of the screen.
        // As progress increases, tx moves from -maxScroll towards 0 (Moving RIGHT).
        float tx = -maxScroll + (maxScroll * progress);
        
        // Anchor to bottom to keep the ground and runner visible
        float dy = viewHeight - (drawableHeight * scale);

        matrix.postTranslate(tx, dy);
        parallaxBackground.setImageMatrix(matrix);
        parallaxBackground.invalidate();
    }

    public void switchToTab(int navId) {
        bottomNavigationView.setSelectedItemId(navId);
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new DashboardFragment();
                case 1: return new FeedFragment();
                case 2: return new LogFragment();
                case 3: return new StatsFragment();
                case 4: return new ProfileFragment();
                default: return new DashboardFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}