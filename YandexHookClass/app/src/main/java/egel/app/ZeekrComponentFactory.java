package egel.app;

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
        scaleApp = ScaleApp.Instance();
        scaleApp.initAppLoadHook();
        scaleApp.initHook();

        // Инициализируем хуки для перехвата манёвров
        maneuverView = ManeuverView.Instance();
        maneuverView.initHooks();
    }
    private static ScaleApp scaleApp;
    private static ManeuverView maneuverView;

    @NonNull
    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Application application = super.instantiateApplication(cl, className);
        
        // Устанавливаем контекст приложения для ManeuverView
        // Используем сам объект application, а не getApplicationContext() который может быть null на этом этапе
        if (maneuverView != null && application != null) {
            maneuverView.setAppContext(application);
        }
        
        return application;
    }

//    @NonNull
//    @Override
//    public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
//
//        Activity activity = super.instantiateActivity(cl, className, intent);
//
//        return activity;
//    }
}
