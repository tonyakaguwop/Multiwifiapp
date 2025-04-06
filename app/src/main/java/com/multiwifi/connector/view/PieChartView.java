package com.multiwifi.connector.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.multiwifi.connector.model.NetworkConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view for displaying a pie chart of network allocation
 */
public class PieChartView extends View {
    
    private static final int[] COLORS = {
        Color.rgb(52, 152, 219), // Blue
        Color.rgb(46, 204, 113), // Green
        Color.rgb(155, 89, 182), // Purple
        Color.rgb(241, 196, 15), // Yellow
        Color.rgb(231, 76, 60),  // Red
        Color.rgb(149, 165, 166) // Gray
    };
    
    private List<NetworkConnection> networks;
    private List<Float> animatedValues;
    private Paint paint;
    private RectF bounds;
    private float animationProgress = 1.0f;
    private ValueAnimator animator;
    
    public PieChartView(Context context) {
        super(context);
        init();
    }
    
    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * Initializes the view
     */
    private void init() {
        networks = new ArrayList<>();
        animatedValues = new ArrayList<>();
        
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        bounds = new RectF();
    }
    
    /**
     * Sets the networks to display in the pie chart
     *
     * @param networks List of networks
     */
    public void setNetworks(List<NetworkConnection> networks) {
        this.networks = new ArrayList<>(networks);
        updateAnimatedValues();
        invalidate();
    }
    
    /**
     * Starts the pie chart animation
     */
    public void startAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
    
    /**
     * Updates the animated values based on network allocation
     */
    private void updateAnimatedValues() {
        animatedValues.clear();
        float totalAllocation = 0;
        
        // Calculate total allocation
        for (NetworkConnection network : networks) {
            totalAllocation += network.getAllocationPercentage();
        }
        
        // If no allocation, use equal distribution
        if (totalAllocation <= 0 && !networks.isEmpty()) {
            float equalShare = 100f / networks.size();
            for (int i = 0; i < networks.size(); i++) {
                animatedValues.add(equalShare / 100f * 360f);
            }
        } else {
            // Convert percentages to degrees
            for (NetworkConnection network : networks) {
                float degrees = (network.getAllocationPercentage() / 100f) * 360f;
                animatedValues.add(degrees);
            }
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Calculate pie chart bounds
        int padding = Math.min(w, h) / 10;
        int size = Math.min(w, h) - (padding * 2);
        int left = (w - size) / 2;
        int top = (h - size) / 2;
        
        bounds.set(left, top, left + size, top + size);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (networks.isEmpty() || animatedValues.isEmpty()) {
            // Draw empty circle if no networks
            paint.setColor(Color.LTGRAY);
            canvas.drawArc(bounds, 0, 360, true, paint);
            return;
        }
        
        float startAngle = 0;
        
        // Draw pie slices
        for (int i = 0; i < animatedValues.size(); i++) {
            // Set color
            paint.setColor(COLORS[i % COLORS.length]);
            
            // Calculate sweep angle with animation
            float sweepAngle = animatedValues.get(i) * animationProgress;
            
            // Draw arc
            canvas.drawArc(bounds, startAngle, sweepAngle, true, paint);
            
            // Update start angle for next slice
            startAngle += sweepAngle;
        }
    }
}
