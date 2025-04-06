package com.multiwifi.connector.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle runtime permissions
 */
public class PermissionHandler {
    private static final String TAG = "PermissionHandler";
    
    /**
     * Interface for permission handling callbacks
     */
    public interface PermissionCallbacks {
        void onPermissionsGranted(int requestCode, List<String> perms);
        void onPermissionsDenied(int requestCode, List<String> perms);
    }
    
    /**
     * Checks if all given permissions are granted
     * 
     * @param context The context
     * @param permissions The permissions to check
     * @return true if all permissions are granted, false otherwise
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context == null || permissions == null || permissions.length == 0) {
            return true;
        }
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Requests the given permissions
     * 
     * @param activity The activity
     * @param requestCode The request code
     * @param permissions The permissions to request
     */
    public static void requestPermissions(Activity activity, int requestCode, String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
    
    /**
     * Processes the permission request results
     * 
     * @param requestCode The request code
     * @param permissions The permissions that were requested
     * @param grantResults The grant results
     * @param callbacks The callbacks
     */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                 @NonNull int[] grantResults, PermissionCallbacks callbacks) {
        List<String> granted = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(permission);
            } else {
                denied.add(permission);
            }
        }
        
        if (!granted.isEmpty() && callbacks != null) {
            callbacks.onPermissionsGranted(requestCode, granted);
        }
        
        if (!denied.isEmpty() && callbacks != null) {
            callbacks.onPermissionsDenied(requestCode, denied);
        }
    }
    
    /**
     * Checks if rationale should be shown for the given permissions
     * 
     * @param activity The activity
     * @param permissions The permissions to check
     * @return true if rationale should be shown for at least one permission, false otherwise
     */
    public static boolean shouldShowRationale(Activity activity, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Opens the app settings
     * 
     * @param context The context
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Log.e(TAG, "No activity found to handle app settings intent");
        }
    }
}
