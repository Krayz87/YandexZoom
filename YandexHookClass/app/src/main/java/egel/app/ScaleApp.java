package egel.app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

public class ScaleApp extends BroadcastReceiver {

    public Context appContext;
    private static ScaleApp _zoom;
    private float Display_Scale = 1.0f;
    private float Resources_Scale = 1.0f;
    private static final String TAG = "ScaleApp";


    private static String _settingName = "egel.app.settings.ScaleApp";

    private static String _dScaleName = "displayScale";
    private static String _rScaleName = "resourcesScale";
    private static String _packageNameParam = "packageName";
    
    // Константы для принудительной русской локали
    private static String _forceRussianLanguage = "ru";
    private static String _forceRussianCountry = "RU";

    // Переменные для контроля частоты логирования (избежание спама)
    private static long lastResourcesConfigLogTime = 0;
    private static long lastLocaleListLogTime = 0;
    private static long lastConfigurationLocalesLogTime = 0;
    private static final long LOG_INTERVAL_MS = 2000; // Логировать не чаще раза в 2 секунды

    // Флаги для инициализации и состояния
    private boolean initialized;

    private boolean registeredReceiver;
    private boolean loadSettings;
    private int originalDensityDpi = -1;
    private float originalDensity = -1f;
    private float originalScaledDensity = -1f;

    MethodHook.Unhook displayUnhook;
    MethodHook.Unhook resourcesUnhook;
    MethodHook.Unhook configurationUnhook;
    MethodHook.Unhook localeListUnhook;

    public static ScaleApp Instance()
    {
        if(_zoom == null)
        {
            _zoom = new ScaleApp();
        }
        return _zoom;
    }

    private ScaleApp()
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
                            // Сохраняем оригинальные значения только при первом вызове и больше никогда их не меняем
                            if (originalDensityDpi == -1) {
                                originalDensityDpi = metrics.densityDpi;
                                originalDensity = metrics.density;
                                originalScaledDensity = metrics.scaledDensity;
                                Log.d(TAG, "Сохранены оригинальные значения DPI: " + originalDensityDpi);
                            }

                            // Всегда пересчитываем масштабированные значения на основе оригинальных
                            int newScaledDensityDpi = (int) (originalDensityDpi * Resources_Scale);
                            float newDensity = originalDensity * Resources_Scale;
                            float newScaledDensity = originalScaledDensity * Resources_Scale;

                            // Применяем масштабирование только если значения изменились
                            if (Math.abs(metrics.densityDpi - newScaledDensityDpi) > 1) {
                                metrics.density = newDensity;
                                metrics.scaledDensity = newScaledDensity;
                                metrics.densityDpi = newScaledDensityDpi;
                                Log.d(TAG, "Применено масштабирование DPI: " + metrics.densityDpi + " (коэффициент: " + Resources_Scale + ")");
                            }
                        }
                    }
                });
                Log.d(TAG, "Хук на Resources.getDisplayMetrics установлен");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке хука на getDisplayMetrics: " + e.getMessage());
            }

            // Инициализируем хук для принудительного русского языка
            initLocaleHook();
            
            initialized = true;
            Log.i(TAG, "Системное масштабирование и локализация успешно инициализированы");
        } catch (Exception e2) {
            Log.e(TAG, "Ошибка при инициализации хука: " + e2.getMessage());
        }
    }

    /**
     * Система хуков для принудительного русского языка.
     * 
     * АКТИВНЫЕ ХУКИ:
     * 1. Resources.getConfiguration() - основной хук для исправления локали
     * 2. LocaleList.getDefault() - управление списком локалей  
     * 3. Configuration.getLocales() - системные списки локалей
     */
    public void initLocaleHook() {
        try {
            Log.i(TAG, "Инициализация хуков для принудительного русского языка...");

            // Хук 1: Resources.getConfiguration() - основной хук для исправления локали
            try {
                configurationUnhook = Pine.hook(android.content.res.Resources.class.getDeclaredMethod("getConfiguration"), new MethodHook() {
                    @Override
                    public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        try {
                            android.content.res.Configuration config = (android.content.res.Configuration) callFrame.getResult();
                            if (config != null) {
                                // Логируем исходное значение перед изменением
                                String originalLocaleStr = config.locale != null ? config.locale.toString() : "null";
                                
                                java.util.Locale russianLocale = new java.util.Locale(_forceRussianLanguage, _forceRussianCountry);
                                config.locale = russianLocale;
                                
                                // Для Android 7.0+ также устанавливаем LocaleList
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    try {
                                        android.os.LocaleList russianLocaleList = new android.os.LocaleList(russianLocale);
                                        config.setLocales(russianLocaleList);
                                    } catch (Exception localeListException) {
                                        Log.w(TAG, "Ошибка при установке LocaleList: " + localeListException.getMessage());
                                    }
                                }
                                
                                // Ограничиваем частоту логирования для этого часто вызываемого метода
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastResourcesConfigLogTime > LOG_INTERVAL_MS) {
                                    Log.d(TAG, "Resources.getConfiguration() [БЫЛО: " + originalLocaleStr + "] → ru_RU");
                                    lastResourcesConfigLogTime = currentTime;
                                }
                            }
                        } catch (Throwable th) {
                            Log.d(TAG, "Ошибка в хуке Resources.getConfiguration(): " + th.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при установке хука на Resources.getConfiguration(): " + e.getMessage());
            }

            // Хук 2: LocaleList.getDefault() - управление списком локалей (Android 7.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    localeListUnhook = Pine.hook(android.os.LocaleList.class.getDeclaredMethod("getDefault"), new MethodHook() {
                        @Override
                        public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                            try {
                                // Логируем исходное значение перед изменением
                                android.os.LocaleList originalLocaleList = (android.os.LocaleList) callFrame.getResult();
                                String originalStr = originalLocaleList != null ? originalLocaleList.toString() : "null";
                                
                                java.util.Locale russianLocale = new java.util.Locale(_forceRussianLanguage, _forceRussianCountry);
                                android.os.LocaleList russianLocaleList = new android.os.LocaleList(russianLocale);
                                callFrame.setResult(russianLocaleList);
                                
                                // Ограничиваем частоту логирования для этого часто вызываемого метода
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastLocaleListLogTime > LOG_INTERVAL_MS) {
                                    Log.d(TAG, "LocaleList.getDefault() [БЫЛО: " + originalStr + "] → [ru_RU]");
                                    lastLocaleListLogTime = currentTime;
                                }
                            } catch (Throwable th) {
                                Log.d(TAG, "Ошибка в хуке LocaleList.getDefault(): " + th.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Не удалось установить хук на LocaleList.getDefault(): " + e.getMessage());
                }
            }

            // Хук 3: Configuration.getLocales() - системные списки локалей (Android 7.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    Pine.hook(android.content.res.Configuration.class.getDeclaredMethod("getLocales"), new MethodHook() {
                        @Override
                        public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                            try {
                                // Логируем исходное значение перед изменением
                                android.os.LocaleList originalLocaleList = (android.os.LocaleList) callFrame.getResult();
                                String originalStr = originalLocaleList != null ? originalLocaleList.toString() : "null";
                                
                                java.util.Locale russianLocale = new java.util.Locale(_forceRussianLanguage, _forceRussianCountry);
                                android.os.LocaleList russianLocaleList = new android.os.LocaleList(russianLocale);
                                callFrame.setResult(russianLocaleList);
                                
                                // Ограничиваем частоту логирования для этого часто вызываемого метода
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastConfigurationLocalesLogTime > LOG_INTERVAL_MS) {
                                    Log.d(TAG, "Configuration.getLocales() [БЫЛО: " + originalStr + "] → [ru_RU]");
                                    lastConfigurationLocalesLogTime = currentTime;
                                }
                            } catch (Throwable th) {
                                Log.d(TAG, "Ошибка в хуке Configuration.getLocales(): " + th.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "Не удалось установить хук на Configuration.getLocales(): " + e.getMessage());
                }
            }

            Log.i(TAG, "Хуки для принудительного русского языка успешно установлены (3 активных хука)");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации хуков языка: " + e.getMessage());
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

    @Override
    public void onReceive(Context context, Intent intent) {
        // Проверяем, что интент предназначен для данного приложения
        String targetPackageName = intent.getStringExtra(_packageNameParam);
        String currentPackageName = context.getPackageName();
        
        if (targetPackageName == null || !targetPackageName.equals(currentPackageName)) {
            Log.d(TAG, "Интент не предназначен для данного приложения: " + targetPackageName + " != " + currentPackageName);
            return;
        }
        
        Log.d(TAG, "Получен интент для приложения: " + currentPackageName);
        
        Display_Scale = intent.getFloatExtra(_dScaleName, 1.0f);
        Resources_Scale = intent.getFloatExtra(_rScaleName, 1.0f);


        SharedPreferences sharedPreferences = context.getSharedPreferences(_settingName, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Сохраняем данные
        editor.putFloat(_dScaleName, Display_Scale);
        editor.putFloat(_rScaleName, Resources_Scale);

        editor.apply();

        Log.i(TAG, "Получены новые настройки: Display_Scale=" + Display_Scale + ", Resources_Scale=" + Resources_Scale);

        // Перезапускаем главную активность для применения новых коэффициентов масштабирования
        restartMainActivity(context);
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

    /**
     * Перезапускает приложение штатными средствами Android для применения новых коэффициентов масштабирования
     */
    private void restartMainActivity(Context context) {
        Log.i(TAG, "Начинаем перезапуск приложения из Application контекста");
        
        try {
            // Поскольку контекст - это Application, используем методы для Application контекста
            
            // Способ 1: Перезапуск через TaskStackBuilder (работает с любым контекстом)
            if (restartWithTaskStackBuilder(context)) {
                return;
            }
            
            // Способ 2: Перезапуск через явный запуск главной активности
            if (restartWithMainActivityLaunch(context)) {
                return;
            }
            
            // Способ 3: Перезапуск через очистку задач и новый запуск
            if (restartWithTaskClear(context)) {
                return;
            }
            
            Log.w(TAG, "Штатные методы перезапуска не сработали, используем резервный");
            restartWithFallbackMethod(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при перезапуске приложения: " + e.getMessage(), e);
        }
    }
    
    /**
     * Способ 2: Перезапуск через явный запуск главной активности из Application контекста
     */
    private boolean restartWithMainActivityLaunch(Context context) {
        try {
            Log.d(TAG, "Перезапуск через явный запуск главной активности");
            
            // Получаем главную активность из манифеста
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntent != null) {
                // Устанавливаем флаги для полной очистки стека и нового запуска
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
                // Параметры для отладки (не обрабатываются MainActivity)
                // launchIntent.putExtra("scale_restart", true);
                // launchIntent.putExtra("restart_time", System.currentTimeMillis());
                
                // Запускаем из Application контекста
                context.startActivity(launchIntent);
                
                Log.i(TAG, "Главная активность запущена из Application контекста");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске главной активности: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Способ 3: Перезапуск через очистку задач и принудительный новый запуск
     */
    private boolean restartWithTaskClear(Context context) {
        try {
            Log.d(TAG, "Перезапуск через очистку задач");
            
            // Получаем ActivityManager для управления задачами
            android.app.ActivityManager activityManager = 
                (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager != null) {
                Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                if (restartIntent != null) {
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                         Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    
                    // Используем Handler для отложенного запуска после очистки
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            context.startActivity(restartIntent);
                            Log.i(TAG, "Приложение перезапущено после очистки задач");
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при отложенном запуске: " + e.getMessage());
                        }
                    }, 300);
                    
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке задач: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Способ 1: Штатный перезапуск через TaskStackBuilder из Application контекста
     */
    private boolean restartWithTaskStackBuilder(Context context) {
        try {
            Log.d(TAG, "Перезапуск через TaskStackBuilder из Application контекста");
            
            Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (restartIntent != null) {
                // Устанавливаем флаги для запуска из Application контекста
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                
                try {
                    // Используем TaskStackBuilder для правильного построения стека задач
                    android.app.TaskStackBuilder stackBuilder = android.app.TaskStackBuilder.create(context);
                    stackBuilder.addNextIntentWithParentStack(restartIntent);
                    stackBuilder.startActivities();
                    
                    Log.i(TAG, "Приложение перезапущено через TaskStackBuilder из Application контекста");
                    return true;
                } catch (Exception stackBuilderException) {
                    Log.w(TAG, "TaskStackBuilder не сработал, пробуем простой startActivity: " + stackBuilderException.getMessage());
                    
                    // Если TaskStackBuilder не работает, используем простой startActivity
                    context.startActivity(restartIntent);
                    Log.i(TAG, "Приложение перезапущено через простой startActivity");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при перезапуске через TaskStackBuilder: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Резервный способ перезапуска из Application контекста
     */
    private void restartWithFallbackMethod(Context context) {
        try {
            Log.d(TAG, "Использование резервного метода перезапуска из Application контекста");
            
            Intent restartIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (restartIntent != null) {
                // Устанавливаем флаги для запуска из Application контекста
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                     Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                     Intent.FLAG_ACTIVITY_CLEAR_TASK);
                
                // Параметр для отладки (не обрабатывается MainActivity)
                // restartIntent.putExtra("fallback_restart", true);
                
                // Запускаем главную активность из Application контекста
                context.startActivity(restartIntent);
                
                Log.i(TAG, "Резервный перезапуск из Application контекста выполнен");
            } else {
                Log.e(TAG, "Не удалось получить launch intent для резервного перезапуска");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка в резервном методе перезапуска: " + e.getMessage());
        }
    }
}
