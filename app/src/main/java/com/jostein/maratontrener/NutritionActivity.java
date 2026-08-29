package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jostein.maratontrener.database.FavoriteMealDao;
import com.jostein.maratontrener.database.FavoriteMealEntity;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.models.Meal;
import com.jostein.maratontrener.repository.MealRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class NutritionActivity extends AppCompatActivity {

    private WorkoutDatabase database;
    private FavoriteMealDao favoriteMealDao;
    private WorkoutDao workoutDao;

    private TextView textProfileDetails;
    private TextView textAdviceContext;
    private SwitchMaterial switchScalePortions;

    private Button btnTabWeek;
    private Button btnTabFavorites;
    private Button btnTabAll;

    private RecyclerView recyclerMeals;
    private MealAdapter adapter;
    private View layoutEmptyState;

    private final List<Meal> displayedMeals = new ArrayList<>();
    private final List<String> favoriteMealIds = new ArrayList<>();

    private int activeTab = 1; // 1 = Week, 2 = Favorites, 3 = All
    private double scaleFactor = 1.0;
    private int currentTrainingWeek = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition);

        // Bind database
        database = WorkoutDatabase.getDatabase(this);
        favoriteMealDao = database.favoriteMealDao();
        workoutDao = database.workoutDao();

        // Bind UI elements
        ImageButton btnBack = findViewById(R.id.btnBackNutrition);
        textProfileDetails = findViewById(R.id.textNutritionProfileDetails);
        textAdviceContext = findViewById(R.id.textNutritionAdviceContext);
        switchScalePortions = findViewById(R.id.switchScalePortions);

        btnTabWeek = findViewById(R.id.btnTabWeek);
        btnTabFavorites = findViewById(R.id.btnTabFavorites);
        btnTabAll = findViewById(R.id.btnTabAll);

        recyclerMeals = findViewById(R.id.recyclerMeals);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Setup toolbar back
        btnBack.setOnClickListener(v -> finish());

        // Setup bottom navigation menu
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setChecked(false);
            }
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = new Intent(this, MainContainerActivity.class);
                intent.putExtra("SELECT_TAB", id);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
                return true;
            });
        }

        // Setup RecyclerView
        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MealAdapter(this, displayedMeals, (meal, isFavorite) -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                if (isFavorite) {
                    favoriteMealDao.insertFavorite(new FavoriteMealEntity(meal.getId()));
                } else {
                    favoriteMealDao.deleteFavoriteById(meal.getId());
                }
                // Refresh list if we are currently looking at the favorites tab
                if (activeTab == 2) {
                    loadRecipesForTab();
                }
                // Sync favorites list with Firebase immediately
                WorkoutUtils.uploadWorkoutsToFirebase(NutritionActivity.this);
            });
        });
        recyclerMeals.setAdapter(adapter);

        // Load profile and setup portion scale
        loadUserProfile();

        // Setup Tabs Click Listeners
        btnTabWeek.setOnClickListener(v -> selectTab(1));
        btnTabFavorites.setOnClickListener(v -> selectTab(2));
        btnTabAll.setOnClickListener(v -> selectTab(3));

        // Setup portion scale check listener
        switchScalePortions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setScalePortions(isChecked, scaleFactor);
        });

        // Load active plan metrics and initial recipe tab list
        determineCurrentWeekAndAdvice();
    }

    private void loadUserProfile() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String name = prefs.getString("userNickname", prefs.getString("userName", "Løper"));
        String weightStr = prefs.getString("userWeight", "");
        String ageStr = prefs.getString("userAge", "");

        double weight = 70.0;
        if (!weightStr.isEmpty()) {
            try {
                weight = Double.parseDouble(weightStr);
            } catch (NumberFormatException ignored) {}
        }
        scaleFactor = weight / 70.0;

        String profileText = "Løper: " + name;
        if (!weightStr.isEmpty()) {
            profileText += " (" + weightStr + " kg)";
        } else {
            profileText += " (70 kg standard)";
        }
        if (!ageStr.isEmpty()) {
            profileText += " • " + ageStr + " år";
        }
        textProfileDetails.setText(profileText);
    }

    private void determineCurrentWeekAndAdvice() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutEntity> workouts = workoutDao.getAllWorkouts();
            
            // Default to calendar week modulo 12 if no plan exists
            Calendar cal = Calendar.getInstance();
            int currentCalendarWeek = cal.get(Calendar.WEEK_OF_YEAR);
            int calculatedWeek = ((currentCalendarWeek - 1) % 12) + 1;
            
            boolean hasLongRunThisWeek = false;
            boolean hasIntenseWorkouts = false;

            if (workouts != null && !workouts.isEmpty()) {
                long todayStart = (System.currentTimeMillis() / 86400000) * 86400000;
                long oneWeekLater = todayStart + (7L * 24 * 60 * 60 * 1000);

                // Find active plan week
                for (WorkoutEntity w : workouts) {
                    if (w.getScheduledDate() >= todayStart && w.getScheduledDate() < oneWeekLater) {
                        if (w.getDistance() >= 15.0) {
                            hasLongRunThisWeek = true;
                        }
                        if ("INTERVALS".equalsIgnoreCase(w.getWorkoutType()) || "TEMPO".equalsIgnoreCase(w.getWorkoutType())) {
                            hasIntenseWorkouts = true;
                        }
                    }
                }
            }

            currentTrainingWeek = currentCalendarWeek;
            final String adviceText;
            if (hasLongRunThisWeek) {
                adviceText = "Basert på ukens treningsplan har du en langtur på over 15 km. " +
                        "Vi anbefaler å øke inntaket av fullkorn (havregrøt, rugbrød) og sunne fettsyrer (laks) " +
                        "for å maksimere karbohydratlagrene og beskytte leddene.";
            } else if (hasIntenseWorkouts) {
                adviceText = "Denne uken inneholder intensive tempo- eller intervalløkter. " +
                        "Fokuser på proteinrike måltider (kyllingfilet, egg, mager meieri) for rask restitusjon " +
                        "og antioksidanter fra grønnsaker og fargerike bær etter øktene.";
            } else {
                adviceText = "Uken har moderate økter eller hviledager. " +
                        "Spis balansert i tråd med Kostrådene. Velg råvarebasert mat og drikk vann som tørstedrikk.";
            }

            // Load favorites list
            List<String> favIds = favoriteMealDao.getAllFavoriteMealIds();
            if (favIds != null) {
                favoriteMealIds.clear();
                favoriteMealIds.addAll(favIds);
            }

            runOnUiThread(() -> {
                textAdviceContext.setText(adviceText);
                adapter.setFavoriteIds(favoriteMealIds);
                selectTab(1); // Select Ukesmeny as default
            });
        });
    }

    private void selectTab(int tabIndex) {
        activeTab = tabIndex;

        // Reset backgrounds of all tab buttons
        btnTabWeek.setBackgroundColor(Color.TRANSPARENT);
        btnTabWeek.setTextColor(getResources().getColor(R.color.white));
        btnTabFavorites.setBackgroundColor(Color.TRANSPARENT);
        btnTabFavorites.setTextColor(getResources().getColor(R.color.white));
        btnTabAll.setBackgroundColor(Color.TRANSPARENT);
        btnTabAll.setTextColor(getResources().getColor(R.color.white));

        // Highlight selected
        if (tabIndex == 1) {
            btnTabWeek.setBackgroundResource(R.drawable.edittext_background);
            btnTabWeek.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
            btnTabWeek.setTextColor(Color.BLACK);
        } else if (tabIndex == 2) {
            btnTabFavorites.setBackgroundResource(R.drawable.edittext_background);
            btnTabFavorites.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
            btnTabFavorites.setTextColor(Color.BLACK);
        } else {
            btnTabAll.setBackgroundResource(R.drawable.edittext_background);
            btnTabAll.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
            btnTabAll.setTextColor(Color.BLACK);
        }

        loadRecipesForTab();
    }

    private void loadRecipesForTab() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Meal> meals = new ArrayList<>();
            if (activeTab == 1) {
                meals = MealRepository.getMealsForWeek(currentTrainingWeek);
            } else if (activeTab == 2) {
                List<String> favIds = favoriteMealDao.getAllFavoriteMealIds();
                for (Meal m : MealRepository.getAllMeals()) {
                    if (favIds != null && favIds.contains(m.getId())) {
                        meals.add(m);
                    }
                }
            } else {
                meals = MealRepository.getAllMeals();
            }

            final List<Meal> finalMeals = meals;
            runOnUiThread(() -> {
                displayedMeals.clear();
                displayedMeals.addAll(finalMeals);
                adapter.setActiveTab(activeTab, currentTrainingWeek);
                adapter.notifyDataSetChanged();

                if (finalMeals.isEmpty()) {
                    recyclerMeals.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    if (activeTab == 2) {
                        ((TextView) findViewById(R.id.textEmptyStateTitle)).setText("Ingen favoritter lagret");
                        ((TextView) findViewById(R.id.textEmptyStateSubtitle)).setText("Tapp stjernen på oppskriftene for å lagre dem her.");
                    } else {
                        ((TextView) findViewById(R.id.textEmptyStateTitle)).setText("Ingen oppskrifter funnet");
                        ((TextView) findViewById(R.id.textEmptyStateSubtitle)).setText("Kunne ikke laste oppskrifter for denne uken.");
                    }
                } else {
                    recyclerMeals.setVisibility(View.VISIBLE);
                    layoutEmptyState.setVisibility(View.GONE);
                }
            });
        });
    }
}
