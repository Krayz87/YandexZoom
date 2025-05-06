package com.zeekr;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import java.lang.reflect.Method;

import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

public class ZoomHook extends BroadcastReceiver {

    public Context appContext;
    private static ZoomHook _zoom;
    private float Display_Scale = 1.0f;
    private float Resources_Scale = 1.0f;
    private static final String TAG = "ZoomHook";


    private static String _settingName = "com.zeekr.ZoomApp";

    private static String _dScaleName = "displayScale";
    private static String _rScaleName = "resourcesScale";


    private boolean initialized;

    private boolean registeredReceiver;
    private boolean loadSettings;
    private int originalDensityDpi = -1;
    private int scaledDensityDpi = -1;
    private float originalDensity = -1f;
    private float originalScaledDensity = -1f;

    MethodHook.Unhook displayUnhook;
    MethodHook.Unhook resourcesUnhook;
    MethodHook.Unhook mapKitUnhook;
    HudDisplayManager _hudDisplayManager;
    public static ZoomHook Instance()
    {
        if(_zoom == null)
        {
            _zoom = new ZoomHook();
        }
        return _zoom;
    }

    private ZoomHook()
    {
        PineConfig.debug = false;
        PineConfig.debuggable = false;
        Pine.ensureInitialized();
    }

    public void initHook() {

        if (initialized) {
            return;
        }
        try {
            Log.i(TAG, "Инициализация системного масштабирования...");

            displayUnhook = Pine.hook(Display.class.getDeclaredMethod("getMetrics", DisplayMetrics.class), new MethodHook() { // from class: com.zeekr.CarWizardComponentFactory.1
                @Override // top.canyie.pine.callback.MethodHook
                public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                    try {
                        DisplayMetrics displayMetrics = (DisplayMetrics) callFrame.args[0];
                        if (displayMetrics != null) {
                            float f = displayMetrics.scaledDensity / displayMetrics.density;
                            displayMetrics.density *= Display_Scale;
                            displayMetrics.scaledDensity = displayMetrics.density * f;
                            displayMetrics.densityDpi = (int) (displayMetrics.densityDpi * Display_Scale);
                            Log.d(TAG,"Метрики дисплея масштабированы: density=" + displayMetrics.density + ", scaledDensity=" + displayMetrics.scaledDensity + ", densityDpi=" + displayMetrics.densityDpi);
                        }
                    } catch (Throwable th) {
                        Log.d(TAG,"Ошибка в хуке getMetrics: " + th.getMessage());
                    }
                }
            });

            Log.d(TAG, "Хук на Display.getMetrics установлен");

            // Хук для Resources.getDisplayMetrics()
            try {
                resourcesUnhook = Pine.hook(Resources.class.getDeclaredMethod("getDisplayMetrics"), new MethodHook() {
                    @Override
                    public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        DisplayMetrics metrics = (DisplayMetrics) callFrame.getResult();
                        if (metrics != null) {
                            // Сохраняем оригинальные значения при первом вызове
                            if (originalDensityDpi == -1) {
                                originalDensityDpi = metrics.densityDpi;
                                originalDensity = metrics.density;
                                originalScaledDensity = metrics.scaledDensity;
                                scaledDensityDpi = (int) (originalDensityDpi * Resources_Scale);
                                Log.d(TAG, "Сохранены оригинальные значения DPI: " + originalDensityDpi);
                            }

                            // Проверяем, не применено ли уже масштабирование
                            if (Math.abs(metrics.densityDpi - scaledDensityDpi) > 10) {
                                metrics.density = originalDensity * Resources_Scale;
                                metrics.scaledDensity = originalScaledDensity * Resources_Scale;
                                metrics.densityDpi = scaledDensityDpi;
                                Log.d(TAG, "Применено масштабирование DPI: " + metrics.densityDpi);
                            }
                        }
                    }
                });
                Log.d(TAG, "Хук на Resources.getDisplayMetrics установлен");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке хука на getDisplayMetrics: " + e.getMessage());
            }

            // Хук для MapKitFactory.initialize
            try {
                Class<?> mapKitFactoryClass = Class.forName("com.yandex.mapkit.MapKitFactory");
                Method initializeMethod = mapKitFactoryClass.getDeclaredMethod("initialize", Context.class);
                
                mapKitUnhook = Pine.hook(initializeMethod, new MethodHook() {
                    @Override
                    public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                        Log.d(TAG, "Перехват MapKitFactory.initialize перед вызовом");
                        // Если нужно изменить контекст или другие параметры перед вызовом
                        // callFrame.args[0] = модифицированный контекст;
                    }

                    @Override
                    public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        Context context = (Context) callFrame.args[0];

                        createView(context);

                        Log.d(TAG, "MapKitFactory.initialize успешно выполнен с контекстом: " + (context != null ? context.getClass().getName() : "null"));
                    }
                });
                Log.d(TAG, "Хук на MapKitFactory.initialize установлен");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке хука на MapKitFactory.initialize: " + e.getMessage());
            }

            initialized = true;
            Log.i(TAG, "Системное масштабирование успешно инициализировано");
        } catch (Exception e2) {
            Log.e(TAG, "Ошибка при инициализации хука: " + e2.getMessage());
        }
    }

    public void initAppLoadHook()
    {
        try {
            // Перехватываем метод onCreate у класса Application
            Pine.hook(Application.class.getDeclaredMethod("onCreate"), new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) {
                    Log.d(TAG, "Application.onCreate вызывается");
                }

                @Override
                public void afterCall(Pine.CallFrame callFrame) {
                    Log.d(TAG, "Application.onCreate выполнен");

                    // Получаем экземпляр Application
                    Application app = (Application) callFrame.thisObject;

                    // Получаем контекст приложения
                    appContext = app.getApplicationContext();

                    // Инициализируем настройки локали
                    initSetting();

                    Log.d(TAG, "Контекст получен и настройки инициализированы");
                }
            });

            Log.d(TAG, "Хук на Application.onCreate установлен");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при хуке Application.onCreate", e);
            e.printStackTrace();
        }


    }

    private void initSetting()
    {
        LoadSettings();
        registerReceiver();
    }

    private void createView(Context context)
    {
        _hudDisplayManager = new HudDisplayManager(context);
        _hudDisplayManager.showOnHudDisplay();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Display_Scale = intent.getFloatExtra(_dScaleName, 1.0f);
        Resources_Scale = intent.getFloatExtra(_rScaleName, 1.0f);


        SharedPreferences sharedPreferences = context.getSharedPreferences(_settingName, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Сохраняем данные
        editor.putFloat(_dScaleName, Display_Scale);
        editor.putFloat(_rScaleName, Resources_Scale);

        editor.apply();

        // Сбросить кэшированные значения для пересчета
        originalDensityDpi = -1;
        scaledDensityDpi = -1;
        originalDensity = -1f;
        originalScaledDensity = -1f;

        Log.i(TAG, "Получены новые настройки: Display_Scale=" + Display_Scale + ", Resources_Scale=" + Resources_Scale);

        try {
            if (context != null) {
                Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (restartIntent != null) {
                    // Создаем новый стек активностей
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Добавляем небольшую задержку
                    restartIntent.putExtra("timestamp", System.currentTimeMillis());

                    // Запускаем главную активность
                    context.startActivity(restartIntent);

                    // Завершаем только текущую активность, но не весь процесс
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).finish();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AppRestart", "Failed to restart: " + e.getMessage());
        }
    }

    private void LoadSettings()
    {
        if(appContext == null)
        {
            Log.i(TAG, "LoadSettings: Display_Scale = " + Display_Scale + "; Resources_Scale = " + Resources_Scale);
            return;
        }
        if(loadSettings)
        {
            return;
        }

        Display_Scale = appContext.getSharedPreferences(_settingName, 0).getFloat(_dScaleName, 1);
        Resources_Scale = appContext.getSharedPreferences(_settingName, 0).getFloat(_rScaleName, 1);
        Log.i(TAG, "LoadSettings: Display_Scale = " + Display_Scale + "; Resources_Scale = " + Resources_Scale);

        // Сбросить кэшированные значения для пересчета
        originalDensityDpi = -1;
        scaledDensityDpi = -1;
        originalDensity = -1f;
        originalScaledDensity = -1f;

        loadSettings = true;
    }

    private void registerReceiver()
    {
        if(registeredReceiver)
        {
            return;
        }
        appContext.registerReceiver(this, new IntentFilter(_settingName), Context.RECEIVER_EXPORTED);

        Log.i(TAG, "RegisterReceiver true");

        registeredReceiver = true;
    }
}
