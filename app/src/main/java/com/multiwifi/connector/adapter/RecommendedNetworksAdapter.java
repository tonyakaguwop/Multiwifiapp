package com.multiwifi.connector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.multiwifi.connector.R;
import com.multiwifi.connector.model.NetworkRecommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for recommended network configurations
 */
public class RecommendedNetworksAdapter extends RecyclerView.Adapter<RecommendedNetworksAdapter.ViewHolder> {
    
    private List<NetworkRecommendation> recommendations;
    private OnRecommendationClickListener listener;
    private int selectedPosition = 0;
    
    /**
     * Listener interface for recommendation clicks
     */
    public interface OnRecommendationClickListener {
        void onRecommendationClick(NetworkRecommendation recommendation);
    }
    
    /**
     * Constructor
     * 
     * @param listener Click listener
     */
    public RecommendedNetworksAdapter(OnRecommendationClickListener listener) {
        this.recommendations = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network_recommendation, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkRecommendation recommendation = recommendations.get(position);
        
        holder.titleText.setText(recommendation.getTitle());
        holder.descriptionText.setText(recommendation.getDescription());
        holder.speedText.setText(String.format("%.1f Mbps", recommendation.getEstimatedSpeed()));
        
        // Set selection state
        holder.radioButton.setChecked(position == selectedPosition);
        
        // Set appropriate icon based on recommendation type
        switch (recommendation.getType()) {
            case SPEED_OPTIMIZED:
                holder.iconImage.setImageResource(android.R.drawable.ic_menu_send);
                break;
            case RELIABILITY_OPTIMIZED:
                holder.iconImage.setImageResource(android.R.drawable.ic_lock_lock);
                break;
            case BALANCED:
                holder.iconImage.setImageResource(android.R.drawable.ic_menu_share);
                break;
            case POWER_SAVING:
                holder.iconImage.setImageResource(android.R.drawable.ic_lock_idle_charging);
                break;
            case CUSTOM:
                holder.iconImage.setImageResource(android.R.drawable.ic_menu_edit);
                break;
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            // Update selection
            int oldSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // Update recommendation selection state
            for (int i = 0; i < recommendations.size(); i++) {
                recommendations.get(i).setSelected(i == selectedPosition);
            }
            
            // Update radio buttons
            notifyItemChanged(oldSelected);
            notifyItemChanged(selectedPosition);
        });
        
        holder.detailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationClick(recommendation);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return recommendations.size();
    }
    
    /**
     * Updates the recommendations list
     * 
     * @param recommendations New list of recommendations
     */
    public void updateRecommendations(List<NetworkRecommendation> recommendations) {
        this.recommendations = recommendations;
        
        // Find the selected recommendation
        selectedPosition = 0;
        for (int i = 0; i < recommendations.size(); i++) {
            if (recommendations.get(i).isSelected()) {
                selectedPosition = i;
                break;
            }
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * Clears all recommendations
     */
    public void clearRecommendations() {
        recommendations.clear();
        selectedPosition = 0;
        notifyDataSetChanged();
    }
    
    /**
     * Gets the list of selected recommendations
     * 
     * @return List containing the selected recommendation
     */
    public List<NetworkRecommendation> getSelectedRecommendations() {
        List<NetworkRecommendation> selected = new ArrayList<>();
        
        if (!recommendations.isEmpty() && selectedPosition >= 0 && selectedPosition < recommendations.size()) {
            selected.add(recommendations.get(selectedPosition));
        }
        
        return selected;
    }
    
    /**
     * ViewHolder class for recommendation items
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleText;
        public TextView descriptionText;
        public TextView speedText;
        public ImageView iconImage;
        public RadioButton radioButton;
        public View detailsButton;
        
        public ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.recommendation_title);
            descriptionText = itemView.findViewById(R.id.recommendation_description);
            speedText = itemView.findViewById(R.id.recommendation_speed);
            iconImage = itemView.findViewById(R.id.recommendation_icon);
            radioButton = itemView.findViewById(R.id.recommendation_radio);
            detailsButton = itemView.findViewById(R.id.recommendation_details_button);
        }
    }
}
