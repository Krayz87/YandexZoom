package com.zeekr;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class HudDisplayManager {

    private String TAG = "YandexHud";
    private final Context context;
    private final DisplayManager displayManager;
    private HudPresentation presentation;


    public HudDisplayManager(Context context) {
        this.context = context;
        this.displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    public void showOnHudDisplay() {

        Display hudDisplay = findHudDisplay();

        if (hudDisplay != null) {
            presentation = new HudPresentation(context, hudDisplay);
            try {
                presentation.show();
                Log.d("HudDisplay", "Display Name:" + hudDisplay.getName() + " HudPresentation");

            } catch (WindowManager.InvalidDisplayException e) {
                Log.d("HudDisplay", "ERROR " + e.getMessage());
            }
        }
        Log.d("HudDisplay", "NotHudDisplay");
    }

    public void dismiss() {
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
    }

    private Display findHudDisplay() {
        Display[] displays = displayManager.getDisplays();
        for (Display display : displays) {
            if (display.getDisplayId() == 2) {
                return display;
            }
        }
        return null;
    }
}

