package com.zeekr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.CoreComponentFactory;

@SuppressLint("RestrictedApi")
public class ZeekrComponentFactory  extends CoreComponentFactory {

    public ZeekrComponentFactory()
    {
        zoomHook = ZoomHook.Instance();
        zoomHook.initAppLoadHook();
        zoomHook.initHook();


    }
    private static ZoomHook zoomHook;

    @NonNull
    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Application application = super.instantiateApplication(cl, className);
        return application;
    }

    @NonNull
    @Override
    public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        Activity activity = super.instantiateActivity(cl, className, intent);

        return activity;
    }
}
