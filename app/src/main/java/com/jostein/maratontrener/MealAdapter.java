package com.jostein.maratontrener;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jostein.maratontrener.models.Ingredient;
import com.jostein.maratontrener.models.Meal;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private final Context context;
    private final List<Meal> mealsList;
    private final Set<String> favoriteIds = new HashSet<>();
    private final Set<String> expandedIds = new HashSet<>();
    private final OnFavoriteClickListener favoriteClickListener;
    
    private boolean scalePortions = false;
    private double scaleFactor = 1.0;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Meal meal, boolean isFavorite);
    }

    private int activeTab = 1;
    private int currentCalendarWeek = 1;

    public void setActiveTab(int activeTab, int currentCalendarWeek) {
        this.activeTab = activeTab;
        this.currentCalendarWeek = currentCalendarWeek;
    }

    public MealAdapter(Context context, List<Meal> mealsList, OnFavoriteClickListener favoriteClickListener) {
        this.context = context;
        this.mealsList = mealsList;
        this.favoriteClickListener = favoriteClickListener;
    }

    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds.clear();
        if (favoriteIds != null) {
            this.favoriteIds.addAll(favoriteIds);
        }
        notifyDataSetChanged();
    }

    public void setScalePortions(boolean scalePortions, double scaleFactor) {
        this.scalePortions = scalePortions;
        this.scaleFactor = scaleFactor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Meal meal = mealsList.get(position);

        holder.textEmoji.setText(meal.getEmoji());
        holder.textType.setText(meal.getType().toUpperCase());
        if (activeTab == 1) {
            holder.textWeek.setText("• Uke " + currentCalendarWeek);
        } else {
            holder.textWeek.setText("• Uke " + meal.getWeekNumber());
        }
        holder.textTitle.setText(meal.getTitle());
        holder.textAdvice.setText(meal.getAdvice());

        // Calculate and format nutrition macros
        double calories = meal.getBaseCalories();
        double carbs = meal.getBaseCarbs();
        double protein = meal.getBaseProtein();
        double fat = meal.getBaseFat();

        if (scalePortions) {
            calories *= scaleFactor;
            carbs *= scaleFactor;
            protein *= scaleFactor;
            fat *= scaleFactor;
        }

        holder.textCalories.setText(String.format(Locale.getDefault(), "%.0f", calories));
        holder.textCarbs.setText(String.format(Locale.getDefault(), "%.0fg", carbs));
        holder.textProtein.setText(String.format(Locale.getDefault(), "%.0fg", protein));
        holder.textFat.setText(String.format(Locale.getDefault(), "%.0fg", fat));

        // Favorite Star setup
        boolean isFav = favoriteIds.contains(meal.getId());
        if (isFav) {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFavorite.setImageTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.electric_lime)));
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setImageTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.text_secondary)));
        }

        holder.btnFavorite.setOnClickListener(v -> {
            boolean nextFav = !isFav;
            if (nextFav) {
                favoriteIds.add(meal.getId());
            } else {
                favoriteIds.remove(meal.getId());
            }
            notifyItemChanged(position);
            favoriteClickListener.onFavoriteClick(meal, nextFav);
        });

        // Expand/Collapse Details
        boolean isExpanded = expandedIds.contains(meal.getId());
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.textTapHint.setText(isExpanded ? "Tapp for å lukke oppskrift" : "Tapp for å vise oppskrift og ingredienser");

        // Click on item to expand
        holder.itemView.setOnClickListener(v -> {
            if (isExpanded) {
                expandedIds.remove(meal.getId());
            } else {
                expandedIds.add(meal.getId());
            }
            notifyItemChanged(position);
        });

        // Build Ingredients list
        StringBuilder sb = new StringBuilder();
        for (Ingredient ing : meal.getIngredients()) {
            double amount = ing.getBaseAmount();
            if (scalePortions && amount > 0) {
                amount *= scaleFactor;
            }
            
            sb.append("• ");
            if (amount > 0) {
                if (amount == Math.round(amount)) {
                    sb.append(String.format(Locale.getDefault(), "%.0f", amount));
                } else {
                    sb.append(String.format(Locale.getDefault(), "%.1f", amount));
                }
                sb.append(" ").append(ing.getUnit()).append(" ");
            }
            sb.append(ing.getName()).append("\n");
        }
        holder.textIngredients.setText(sb.toString().trim());
        holder.textInstructions.setText(meal.getInstructions());
    }

    @Override
    public int getItemCount() {
        return mealsList != null ? mealsList.size() : 0;
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView textEmoji, textType, textWeek, textTitle, textAdvice;
        TextView textCalories, textCarbs, textProtein, textFat;
        TextView textIngredients, textInstructions, textTapHint;
        ImageButton btnFavorite;
        LinearLayout layoutDetails;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            textEmoji = itemView.findViewById(R.id.textMealEmoji);
            textType = itemView.findViewById(R.id.textMealType);
            textWeek = itemView.findViewById(R.id.textMealWeek);
            textTitle = itemView.findViewById(R.id.textMealTitle);
            textAdvice = itemView.findViewById(R.id.textMealAdvice);
            textCalories = itemView.findViewById(R.id.textMealCalories);
            textCarbs = itemView.findViewById(R.id.textMealCarbs);
            textProtein = itemView.findViewById(R.id.textMealProtein);
            textFat = itemView.findViewById(R.id.textMealFat);
            textIngredients = itemView.findViewById(R.id.textMealIngredients);
            textInstructions = itemView.findViewById(R.id.textMealInstructions);
            textTapHint = itemView.findViewById(R.id.textTapHint);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            layoutDetails = itemView.findViewById(R.id.layoutMealDetails);
        }
    }
}
