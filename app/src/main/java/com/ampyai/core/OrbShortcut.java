package com.ampyai.core;
import android.accessibilityservice.*;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public final class OrbShortcut extends AccessibilityService {
    private final AccessibilityButtonController.AccessibilityButtonCallback button =
        new AccessibilityButtonController.AccessibilityButtonCallback(){
            @Override public void onClicked(AccessibilityButtonController c){
                startActivity(new Intent(OrbShortcut.this,OrbActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }
        };
    @Override protected void onServiceConnected(){getAccessibilityButtonController().registerAccessibilityButtonCallback(button);}
    @Override public void onDestroy(){getAccessibilityButtonController().unregisterAccessibilityButtonCallback(button);super.onDestroy();}
    @Override public void onAccessibilityEvent(AccessibilityEvent unused){}
    @Override public void onInterrupt(){}
}