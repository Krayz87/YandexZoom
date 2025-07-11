package egel.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.CoreComponentFactory;
import androidx.core.app.ComponentActivity;

@SuppressLint("RestrictedApi")
public class ZeekrComponentFactory extends CoreComponentFactory {

    private static final String TAG = "ZeekrComponentFactory";

    public ZeekrComponentFactory()
    {
        android.util.Log.i("ZeekrComponentFactory", "🚀 === ИНИЦИАЛИЗАЦИЯ ZeekrComponentFactory ===");
        
        // Инициализируем системы масштабирования и локализации - ИСПРАВЛЕН
        scaleApp = ScaleApp.Instance();
        scaleApp.initAppLoadHook();
        scaleApp.initHook();
        android.util.Log.i("ZeekrComponentFactory", "✅ ScaleApp инициализирован");

        // Инициализируем хуки для перехвата манёвров (работает)
        maneuverView = ManeuverView.Instance();
        maneuverView.initHooks();
        android.util.Log.i("ZeekrComponentFactory", "✅ ManeuverView инициализирован");

        // Запуск охотника за GuidanceBinding - НОВАЯ УЛУЧШЕННАЯ СИСТЕМА
        // Контекст будет установлен позже в instantiateApplication()
        android.util.Log.i("ZeekrComponentFactory", "🎯 GuidanceHunter готов к запуску");

        
//        // Инициализируем NavigationLayerHook - поиск NavigationLayer интерфейса и BalloonViewListener
//        //navigationLayerHook = NavigationLayerHook.Instance();
//        android.util.Log.i("ZeekrComponentFactory", "🚀 NavigationLayerHook инициализирован");
//
//        // Инициализируем NavigationListenerHook - перехват onRoutesBuilt() методов
//        //navigationListenerHook = NavigationListenerHook.Instance();
//        //navigationListenerHook.initHooks();
//        android.util.Log.i("ZeekrComponentFactory", "🚀 NavigationListenerHook инициализирован");
//
//        // Инициализируем GuidanceHook - поиск живых ссылок на com.yandex.navikit.guidance.Guidance
//        guidanceHook = GuidanceHook.Instance();
//        android.util.Log.i("ZeekrComponentFactory", "🔍 GuidanceHook инициализирован");
        
        // LaneSignTracker отключен - не показал результатов
        // Для восстановления раскомментируйте:
        // laneSignTracker = new LaneSignTracker();
        // android.util.Log.i("ZeekrComponentFactory", "🚀 LaneSignTracker инициализирован");
        //laneSignTracker = null;
        //android.util.Log.i("ZeekrComponentFactory", "⚠️ LaneSignTracker отключен - фокус на NavigationLayerHook");
        
        android.util.Log.i("ZeekrComponentFactory", "🎉 Все системы инициализированы успешно");
    }
    
    private static ScaleApp scaleApp;
    private static ManeuverView maneuverView;
    //private static NavigationLayerHook navigationLayerHook;
    //private static NavigationListenerHook navigationListenerHook;
    //private static GuidanceHook guidanceHook;
    //private static LaneSignTracker laneSignTracker;

    @NonNull
    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Application application = super.instantiateApplication(cl, className);
        
        android.util.Log.i("ZeekrComponentFactory", "🚀 Application создано: " + (application != null ? application.getClass().getSimpleName() : "null"));
        
        // ВАЖНО: Передаем контекст приложения в GuidanceHunter для работы хуков
        if (application != null) {
            try {
                // Устанавливаем контекст для GuidanceHunter (новая система)
                GuidanceHunter.getInstance().setAppContext(application);
                android.util.Log.i("ZeekrComponentFactory", "✅ Контекст передан в GuidanceHunter");
                
                // Запускаем поиск после установки контекста
                GuidanceHunter.getInstance().startHunting();
                android.util.Log.i("ZeekrComponentFactory", "🚀 GuidanceHunter запущен с контекстом");
                
                // Проверяем статус через 3 секунды
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    String searchStatus = GuidanceHunter.getInstance().getSearchStatus();
                    android.util.Log.i("ZeekrComponentFactory", "📊 Статус поиска: " + searchStatus);
                    
                    boolean isReady = GuidanceHunter.getInstance().isReady();
                    
                    if (isReady) {
                        android.util.Log.i("ZeekrComponentFactory", "🎯 GUIDANCEHUNTER ГОТОВ К РАБОТЕ!");
                    } else {
                        android.util.Log.d("ZeekrComponentFactory", "⏳ GuidanceHunter еще ищет объекты...");
                    }
                }, 3000);
                
            } catch (Exception e) {
                android.util.Log.e("ZeekrComponentFactory", "❌ Ошибка передачи контекста в GuidanceHunter: " + e.getMessage(), e);
            }
        }

        return application;
    }

}
