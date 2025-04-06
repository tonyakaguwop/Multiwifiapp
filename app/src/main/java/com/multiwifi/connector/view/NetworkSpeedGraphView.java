package com.multiwifi.connector.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view for displaying network speed history as a line graph
 */
public class NetworkSpeedGraphView extends View {
    
    private static final int MAX_DATA_POINTS = 30;
    private static final int GRAPH_COLOR = Color.rgb(41, 128, 185);
    private static final int GRID_COLOR = Color.argb(50, 0, 0, 0);
    private static final int TEXT_COLOR = Color.rgb(44, 62, 80);
    
    private List<Double> speedData;
    private List<Double> animatedSpeedData;
    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Path linePath;
    private Path fillPath;
    private double maxSpeed = 10.0; // Initial max speed in Mbps
    private float animationProgress = 1.0f;
    private ValueAnimator animator;
    
    public NetworkSpeedGraphView(Context context) {
        super(context);
        init();
    }
    
    public NetworkSpeedGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public NetworkSpeedGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    /**
     * Initializes the view
     */
    private void init() {
        speedData = new ArrayList<>();
        animatedSpeedData = new ArrayList<>();
        
        // Initialize line paint
        linePaint = new Paint();
        linePaint.setColor(GRAPH_COLOR);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setAntiAlias(true);
        
        // Initialize fill paint
        fillPaint = new Paint();
        fillPaint.setColor(Color.argb(50, 41, 128, 185));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        
        // Initialize grid paint
        gridPaint = new Paint();
        gridPaint.setColor(GRID_COLOR);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        
        // Initialize text paint
        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        
        // Initialize paths
        linePath = new Path();
        fillPath = new Path();
    }
    
    /**
     * Adds a new data point to the graph
     *
     * @param speed The speed value to add in Mbps
     */
    public void addDataPoint(double speed) {
        // Add to data list
        speedData.add(speed);
        
        // Keep only the most recent points
        if (speedData.size() > MAX_DATA_POINTS) {
            speedData.remove(0);
        }
        
        // Update max speed if needed
        if (speed > maxSpeed) {
            maxSpeed = Math.ceil(speed / 10.0) * 10.0;
        }
        
        // Start animation
        startAnimation();
    }
    
    /**
     * Resets the graph
     */
    public void reset() {
        speedData.clear();
        animatedSpeedData.clear();
        maxSpeed = 10.0;
        invalidate();
    }
    
    /**
     * Starts the graph animation
     */
    private void startAnimation() {
        // Update animated speed data
        animatedSpeedData = new ArrayList<>(speedData);
        
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (speedData.isEmpty()) {
            // Draw empty state
            drawEmptyState(canvas);
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        
        int graphWidth = width - paddingLeft - paddingRight;
        int graphHeight = height - paddingTop - paddingBottom;
        
        // Draw grid lines
        drawGrid(canvas, paddingLeft, paddingTop, graphWidth, graphHeight);
        
        // Draw speed graph
        drawGraph(canvas, paddingLeft, paddingTop, graphWidth, graphHeight);
    }
    
    /**
     * Draws empty state when no data is available
     *
     * @param canvas Canvas to draw on
     */
    private void drawEmptyState(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // Draw empty message
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("No speed data yet", width / 2f, height / 2f, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
    }
    
    /**
     * Draws the grid lines and labels
     *
     * @param canvas Canvas to draw on
     * @param left Left position
     * @param top Top position
     * @param width Width of grid
     * @param height Height of grid
     */
    private void drawGrid(Canvas canvas, int left, int top, int width, int height) {
        // Draw horizontal grid lines
        int numHLines = 5;
        for (int i = 0; i <= numHLines; i++) {
            float y = top + height - (i * height / numHLines);
            canvas.drawLine(left, y, left + width, y, gridPaint);
            
            // Draw labels
            if (i > 0) {
                String label = String.format("%.0f", (maxSpeed * i / numHLines));
                canvas.drawText(label, left - 10, y + textPaint.getTextSize() / 3, textPaint);
            }
        }
        
        // Draw vertical grid lines
        int numVLines = 6;
        for (int i = 0; i <= numVLines; i++) {
            float x = left + (i * width / numVLines);
            canvas.drawLine(x, top, x, top + height, gridPaint);
        }
    }
    
    /**
     * Draws the speed graph
     *
     * @param canvas Canvas to draw on
     * @param left Left position
     * @param top Top position
     * @param width Width of graph
     * @param height Height of graph
     */
    private void drawGraph(Canvas canvas, int left, int top, int width, int height) {
        if (animatedSpeedData.isEmpty()) {
            return;
        }
        
        int dataSize = animatedSpeedData.size();
        float xStep = width / (float) (MAX_DATA_POINTS - 1);
        
        // Clear paths
        linePath.reset();
        fillPath.reset();
        
        // Start fill path at bottom-left
        fillPath.moveTo(left, top + height);
        
        // Generate path points
        for (int i = 0; i < dataSize; i++) {
            float x = left + (i * xStep);
            float y = (float) (top + height - (animatedSpeedData.get(i) / maxSpeed * height * animationProgress));
            
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        
        // Close fill path to bottom-right and back to start
        fillPath.lineTo(left + ((dataSize - 1) * xStep), top + height);
        fillPath.close();
        
        // Draw fill and line
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
