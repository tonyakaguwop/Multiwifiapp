package com.multiwifi.connector.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Utility class for navigation between activities
 */
public class NavigationUtils {
    
    /**
     * Navigate to a new activity
     * 
     * @param currentActivity Current activity
     * @param destinationClass Destination activity class
     */
    public static void navigateTo(Activity currentActivity, Class<?> destinationClass) {
        navigateTo(currentActivity, destinationClass, null, false);
    }
    
    /**
     * Navigate to a new activity with extras
     * 
     * @param currentActivity Current activity
     * @param destinationClass Destination activity class
     * @param extras Bundle of extras to pass to the new activity
     */
    public static void navigateTo(Activity currentActivity, Class<?> destinationClass, Bundle extras) {
        navigateTo(currentActivity, destinationClass, extras, false);
    }
    
    /**
     * Navigate to a new activity, optionally finishing the current one
     * 
     * @param currentActivity Current activity
     * @param destinationClass Destination activity class
     * @param extras Bundle of extras to pass to the new activity
     * @param finishCurrent Whether to finish the current activity
     */
    public static void navigateTo(Activity currentActivity, Class<?> destinationClass, 
                                 Bundle extras, boolean finishCurrent) {
        Intent intent = new Intent(currentActivity, destinationClass);
        
        if (extras != null) {
            intent.putExtras(extras);
        }
        
        currentActivity.startActivity(intent);
        
        if (finishCurrent) {
            currentActivity.finish();
        }
    }
    
    /**
     * Navigate to a new activity and clear the back stack
     * 
     * @param currentActivity Current activity
     * @param destinationClass Destination activity class
     */
    public static void navigateToAndClearStack(Activity currentActivity, Class<?> destinationClass) {
        navigateToAndClearStack(currentActivity, destinationClass, null);
    }
    
    /**
     * Navigate to a new activity with extras and clear the back stack
     * 
     * @param currentActivity Current activity
     * @param destinationClass Destination activity class
     * @param extras Bundle of extras to pass to the new activity
     */
    public static void navigateToAndClearStack(Activity currentActivity, Class<?> destinationClass, Bundle extras) {
        Intent intent = new Intent(currentActivity, destinationClass);
        
        if (extras != null) {
            intent.putExtras(extras);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        currentActivity.startActivity(intent);
        currentActivity.finish();
    }
}
