package egel.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

public class ManeuverView {

    private static final String TAG = "ManeuverView";
    private static final String ACTION_MANEUVER_VISIBILITY = "ru.yandex.yandexnavi.MANEUVER_VISIBILITY";
    private static final String ACTION_MANEUVER_UPDATE = "ru.yandex.yandexnavi.MANEUVER_UPDATE";
    private static final String EXTRA_DISTANCE = "distance";
    private static final String EXTRA_IMAGE_ID = "image_id";
    private static final String EXTRA_IS_VISIBLE = "is_visible";
    private static final String EXTRA_MANEUVER_IMAGE = "maneuver_image";
    private static final String EXTRA_METRICS = "metrics";
    private static final String EXTRA_ROAD_NAME = "road_name";
    private static final String EXTRA_UNIT = "unit";

    private static ManeuverView _instance;
    //private Context appContext;
    private boolean initialized = false;

    // Хуки для отключения
    private MethodHook.Unhook setManeuverUnhook;
    private MethodHook.Unhook setContentVisibleUnhook;

    // Переменные для хранения текущего состояния
    private String currentDistance = "";
    private String currentUnit = "";
    private String currentRoadName = "";
    private String currentImageId = "";
    private boolean isCurrentlyVisible = false;

    public static ManeuverView Instance() {
        if (_instance == null) {
            _instance = new ManeuverView();
        }
        return _instance;
    }

    private ManeuverView() {
        // Pine уже инициализирован в ScaleApp
    }

//    public void setAppContext(Context context) {
//        this.appContext = context;
//    }

    public void initHooks() {
        if (initialized) {
            Log.d(TAG, "Хуки уже инициализированы");
            return;
        }

        try {
            Log.i(TAG, "Инициализация хуков для ManeuverView...");

            // Инициализируем хук для метода setManeuver
            initSetManeuverHook();

            // Инициализируем хук для метода setContentVisible
            initSetContentVisibleHook();

            initialized = true;
            Log.i(TAG, "Хуки ManeuverView успешно инициализированы");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации хуков: " + e.getMessage(), e);
        }
    }

    private void initSetManeuverHook() {
        try {
            Class<?> contextManeuverViewClass = Class.forName("ru.yandex.yandexnavi.ui.guidance.maneuver.ContextManeuverView");
            Class<?> resourceIdClass = Class.forName("com.yandex.navikit.resources.ResourceId");

            setManeuverUnhook = Pine.hook(
                contextManeuverViewClass.getDeclaredMethod("setManeuver", 
                    resourceIdClass, 
                    String.class, 
                    String.class, 
                    String.class), 
                new MethodHook() {
                    @Override
                    public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                        try {
                            Object imageId = callFrame.args[0];
                            String distance = (String) callFrame.args[1];
                            String unit = (String) callFrame.args[2];
                            String nextRoadName = (String) callFrame.args[3];

                            // Сохраняем текущие значения
                            currentImageId = imageId != null ? imageId.toString() : "";
                            currentDistance = distance != null ? distance : "";
                            currentUnit = unit != null ? unit : "";
                            currentRoadName = nextRoadName != null ? nextRoadName : "";

                            Log.d(TAG, "setManeuver вызван: imageId=" + currentImageId + 
                                      ", distance=" + currentDistance + 
                                      ", unit=" + currentUnit + 
                                      ", roadName=" + currentRoadName);

                            // Получаем объект ContextManeuverView
                            Object contextManeuverView = callFrame.thisObject;
                            
                            // Отправляем broadcast с новой логикой
                            sendManeuverUpdateBroadcast(contextManeuverView, imageId, distance, unit, nextRoadName);

                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка в хуке setManeuver: " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        Log.d(TAG, "setManeuver выполнен успешно");
                    }
                });

            Log.d(TAG, "Хук на setManeuver установлен");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при установке хука на setManeuver: " + e.getMessage(), e);
        }
    }

    private void initSetContentVisibleHook() {
        try {
            Class<?> contextManeuverViewClass = Class.forName("ru.yandex.yandexnavi.ui.guidance.maneuver.ContextManeuverView");

            setContentVisibleUnhook = Pine.hook(
                contextManeuverViewClass.getDeclaredMethod("setContentVisible", boolean.class),
                new MethodHook() {
                    @Override
                    public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                        try {
                            boolean visible = (Boolean) callFrame.args[0];
                            isCurrentlyVisible = visible;

                            Log.d(TAG, "setContentVisible вызван: visible=" + visible);

                            // Получаем объект ContextManeuverView
                            Object contextManeuverView = callFrame.thisObject;
                            
                            // Отправляем broadcast с использованием контекста и ID объекта
                            sendContentVisibilityBroadcast(contextManeuverView, visible);

                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка в хуке setContentVisible: " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        Log.d(TAG, "setContentVisible выполнен успешно");
                    }
                });

            Log.d(TAG, "Хук на setContentVisible установлен");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при установке хука на setContentVisible: " + e.getMessage(), e);
        }
    }

    private void sendManeuverUpdateBroadcast(Object contextManeuverView, Object imageId, String distance, String unit, String nextRoadName) {
        try {
            // Получаем контекст компонента через рефлексию
            Context context = (Context) contextManeuverView.getClass().getMethod("getContext").invoke(contextManeuverView);
            
            Intent intent = new Intent(ACTION_MANEUVER_UPDATE);
            intent.putExtra(EXTRA_DISTANCE, distance);
            intent.putExtra(EXTRA_METRICS, unit);
            
            String str = EXTRA_ROAD_NAME;
            if (nextRoadName != null) {
                intent.putExtra(str, nextRoadName);
            } else {
                intent.putExtra(str, "");
            }
            
            // Получаем название ресурса изображения
            try {
                // Получаем internalId из ResourceId (может быть String или int)
                Object internalIdObj = imageId.getClass().getMethod("getInternalId").invoke(imageId);
                String resourceName = "unknown";
                
                if (internalIdObj instanceof String) {
                    // Если internalId это строка, используем её напрямую
                    resourceName = (String) internalIdObj;
                    Log.d(TAG, "Image resource (string): " + resourceName);
                } else if (internalIdObj instanceof Integer) {
                    // Если internalId это число, пытаемся получить название ресурса
                    int internalId = (Integer) internalIdObj;
                    try {
                        Class<?> drawableUtilsClass = Class.forName("ru.yandex.yandexnavi.ui.common.DrawableUtils");
                        int drawableId = (Integer) drawableUtilsClass.getMethod("getDrawableId", Context.class, int.class)
                            .invoke(null, context, internalId);
                        resourceName = context.getResources().getResourceName(drawableId);
                        Log.d(TAG, "Image resource: " + resourceName + " (internalId: " + internalId + ", drawableId: " + drawableId + ")");
                    } catch (Exception e) {
                        // Fallback - используем ID напрямую
                        try {
                            resourceName = context.getResources().getResourceName(internalId);
                            Log.d(TAG, "Image resource (direct): " + resourceName + " (id: " + internalId + ")");
                        } catch (Exception e2) {
                            resourceName = "id_" + internalId;
                            Log.d(TAG, "Image resource (fallback): " + resourceName);
                        }
                    }
                } else {
                    resourceName = internalIdObj != null ? internalIdObj.toString() : "null";
                    Log.d(TAG, "Image resource (toString): " + resourceName);
                }
                
                intent.putExtra(EXTRA_IMAGE_ID, resourceName);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении ресурса изображения: " + e.getMessage(), e);
                // Fallback - используем toString объекта ResourceId
                intent.putExtra(EXTRA_IMAGE_ID, imageId != null ? imageId.toString() : "unknown");
            }
            
            // Получаем изображение из binding (гибкий поиск)
            try {
                // Получаем поле binding
                Field bindingField = contextManeuverView.getClass().getDeclaredField("binding");
                bindingField.setAccessible(true);
                Object binding = bindingField.get(contextManeuverView);
                
                Drawable drawable = null;
                
                // Пробуем разные способы получения drawable
                try {
                    // Способ 1: получаем поле b из binding
                    Field bField = binding.getClass().getDeclaredField("b");
                    bField.setAccessible(true);
                    Object b = bField.get(binding);
                    
                    // Проверяем, есть ли метод getDrawable
                    if (android.widget.ImageView.class.isAssignableFrom(b.getClass())) {
                        drawable = ((android.widget.ImageView) b).getDrawable();
                        Log.d(TAG, "Drawable получен из ImageView");
                    } else if (android.view.View.class.isAssignableFrom(b.getClass())) {
                        // Пытаемся найти drawable через background
                        drawable = ((android.view.View) b).getBackground();
                        Log.d(TAG, "Drawable получен как background");
                    }
                } catch (Exception e1) {
                    Log.d(TAG, "Не удалось получить drawable из поля b: " + e1.getMessage());
                }
                
                // Способ 2: ищем ImageView в других полях binding
                if (drawable == null) {
                    Field[] fields = binding.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        try {
                            field.setAccessible(true);
                            Object fieldValue = field.get(binding);
                            if (fieldValue instanceof android.widget.ImageView) {
                                drawable = ((android.widget.ImageView) fieldValue).getDrawable();
                                if (drawable != null) {
                                    Log.d(TAG, "Drawable найден в поле " + field.getName());
                                    break;
                                }
                            }
                        } catch (Exception e2) {
                            // Игнорируем ошибки поиска
                        }
                    }
                }
                
                if (drawable != null) {
                    Bitmap createBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    drawable.draw(new Canvas(createBitmap));
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    createBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    intent.putExtra(EXTRA_MANEUVER_IMAGE, Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0));
                    Log.d(TAG, "Изображение маневра закодировано в Base64");
                } else {
                    Log.w(TAG, "Drawable не найден, изображение не добавлено");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении изображения из binding: " + e.getMessage(), e);
            }
            
            // Получаем значение isViewContentVisible
            try {
                Field isVisibleField = contextManeuverView.getClass().getDeclaredField("isViewContentVisible");
                isVisibleField.setAccessible(true);
                boolean isViewContentVisible = (Boolean) isVisibleField.get(contextManeuverView);
                intent.putExtra(EXTRA_IS_VISIBLE, isViewContentVisible);
                Log.d(TAG, "isViewContentVisible: " + isViewContentVisible);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении isViewContentVisible: " + e.getMessage(), e);
                intent.putExtra(EXTRA_IS_VISIBLE, false);
            }
            
            context.sendBroadcast(intent);
            
            Log.d(TAG, "Отправлен broadcast setManeuver: distance=" + distance + 
                      ", unit=" + unit + ", roadName=" + (nextRoadName != null ? nextRoadName : "null"));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отправке broadcast setManeuver: " + e.getMessage(), e);
        }
    }

    private void sendContentVisibilityBroadcast(Object contextManeuverView, boolean visible) {
        try {
            // Получаем ID компонента через рефлексию
            int componentId = (Integer) contextManeuverView.getClass().getMethod("getId").invoke(contextManeuverView);
            
            // Получаем контекст компонента через рефлексию
            Context context = (Context) contextManeuverView.getClass().getMethod("getContext").invoke(contextManeuverView);
            
            Intent intent = new Intent(ACTION_MANEUVER_VISIBILITY);
            intent.putExtra(EXTRA_IS_VISIBLE, visible);
            intent.putExtra("component_id", componentId);
            intent.putExtra("component_class", "ContextManeuverView");
            
            context.sendBroadcast(intent);
            
            Log.d(TAG, "Отправлен broadcast setContentVisible: visible=" + visible + 
                      ", componentId=" + componentId + ", class=ContextManeuverView");

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отправке broadcast setContentVisible: " + e.getMessage(), e);
        }
    }

    public void cleanup() {
        try {
            if (setManeuverUnhook != null) {
                setManeuverUnhook.unhook();
                setManeuverUnhook = null;
            }
            if (setContentVisibleUnhook != null) {
                setContentVisibleUnhook.unhook();
                setContentVisibleUnhook = null;
            }
            initialized = false;
            Log.d(TAG, "Хуки отключены");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отключении хуков: " + e.getMessage(), e);
        }
    }
}
