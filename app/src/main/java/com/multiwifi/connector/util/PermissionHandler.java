package com.multiwifi.connector.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.multiwifi.connector.R;
import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {
    
    private final Context context;
    
    // Permissions needed for the app
    private final String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
    };
    
    public PermissionHandler(Context context) {
        this.context = context;
    }
    
    public boolean areAllPermissionsGranted() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // For Android 10+, check for background location if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        
        return true;
    }
    
    public void requestRequiredPermissions(int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Check each permission
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        // For Android 10+, request background location separately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request initial permissions first, then request background location in a separate dialog
                if (permissionsToRequest.isEmpty()) {
                    requestBackgroundLocationPermission(requestCode);
                }
            }
        }
        
        // Request permissions if any are needed
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    (Activity) context, 
                    permissionsToRequest.toArray(new String[0]), 
                    requestCode
            );
        }
    }
    
    private void requestBackgroundLocationPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Show explanation dialog first
            new AlertDialog.Builder(context)
                    .setTitle(R.string.background_location_title)
                    .setMessage(R.string.background_location_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Request the permission
                        ActivityCompat.requestPermissions(
                                (Activity) context,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                requestCode
                        );
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
        }
    }
    
    public void showPermissionExplanationDialog() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.permissions_required_title)
                .setMessage(R.string.permissions_required_settings_message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }
}
