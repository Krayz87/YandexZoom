package com.zeekr;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.Logo;
import com.yandex.mapkit.logo.Padding;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;

import androidx.constraintlayout.widget.ConstraintLayout;

public class HudPresentation extends Presentation {

    private static String TAG = "YandexHud";
    private int heightPixels;
    private int widthPixels;
    private MapView mapView;
    UserLocationLayer userLocationLayer;
    public HudPresentation(Context outerContext, Display display) {
        super(outerContext, display);

    }

    @Override
    protected void onStop() {
        // При остановке активности нужно останавливать MapKit и MapView
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void show() {
        super.show();

        Log.d(TAG, "HudPresentation show");
        int mapHeight = mapView.getHeight();
        int mapWidth = mapView.getWidth();

        userLocationLayer.setAnchor(new PointF(100, 150), new PointF(100, 150));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем корневой ConstraintLayout
        ConstraintLayout constraintLayout = new ConstraintLayout(getContext());
        constraintLayout.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));
        constraintLayout.setBackgroundColor(0x00000000); // Прозрачный фон (#00000000)

        // Создаем MapView
        mapView = new MapView(getContext());
        mapView.setId(View.generateViewId()); // Генерация уникального ID для MapView

        // Создаем параметры для MapView
        ConstraintLayout.LayoutParams mapViewParams = new ConstraintLayout.LayoutParams(
                dpToPx(200), // 200dp в пикселях
                dpToPx(250)  // 250dp в пикселях
        );

        // Устанавливаем параметры для MapView
        mapView.setLayoutParams(mapViewParams);

        // Добавляем MapView в ConstraintLayout
        constraintLayout.addView(mapView);

        // Настраиваем ограничения с использованием ConstraintSet
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        // Устанавливаем ограничения для MapView
        constraintSet.connect(
                mapView.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                dpToPx(550) // отступ 550dp от левого края
        );

        constraintSet.connect(
                mapView.getId(),
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                dpToPx(220) // отступ 220dp от верхнего края
        );

        // Применяем ограничения
        constraintSet.applyTo(constraintLayout);

        // Устанавливаем созданный View как содержимое Presentation
        setContentView(constraintLayout);

        //setContentView(R.layout.map_view);

        //mapView = (MapView) findViewById(R.id.mapview);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getDisplay().getRealMetrics(displayMetrics);
        this.heightPixels = displayMetrics.heightPixels;
        this.widthPixels = displayMetrics.widthPixels;

//        FrameLayout.LayoutParams layoutParamsMap = (FrameLayout.LayoutParams) mapView.getLayoutParams();
//
//        // Получаем настройки из SettingsManager
//        int width = settingsManager.getDisplayWidth();
//        int height = settingsManager.getDisplayHeight();
//        int leftMargin = settingsManager.getLeftMargin();
//        int topMargin = settingsManager.getTopMargin();
//
//        // Устанавливаем ширину и высоту из настроек (0 означает использование всего экрана)
//        ((ViewGroup.MarginLayoutParams) layoutParamsMap).width = 300;//(width == 0) ? widthPixels : width;
//        ((ViewGroup.MarginLayoutParams) layoutParamsMap).height = 300;//(height == 0) ? heightPixels : height;
//        ((ViewGroup.MarginLayoutParams) layoutParamsMap).leftMargin = leftMargin;
//        ((ViewGroup.MarginLayoutParams) layoutParamsMap).topMargin = topMargin;
//
//        Log.d(TAG, String.format("Применены настройки отображения: ширина=%d, высота=%d, левый отступ=%d, верхний отступ=%d",
//                ((ViewGroup.MarginLayoutParams) layoutParamsMap).width,
//                ((ViewGroup.MarginLayoutParams) layoutParamsMap).height,
//                leftMargin, topMargin));
//
//        mapView.setLayoutParams(layoutParamsMap);

        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = this.widthPixels;
        attributes.height = this.heightPixels;
        attributes.x = 0;
        attributes.y = 0;
        window.setDimAmount(0.0f);
        window.setAttributes(attributes);

        String style = "[" +
                "    {" +
                "        \"tags\": {" +
                "            \"any\": [" +
                "                \"poi\"," +
                "                \"structure\"," +
                "                \"landscape\"," +
                "                \"transit\"," +
                "                \"path\"," +
                "                \"admin\"," +
                "                \"water\"" +
                "            ]," +
                "            \"none\": [" +
                "                \"road_1\"," +
                "                \"road_2\"," +
                "                \"road_3\"," +
                "                \"road_4\"," +
                "                \"road_5\"," +
                "                \"road_6\"," +
                "                \"road_7\"" +
                "            ]" +
                "        }," +
                "        \"stylers\": {" +
                "            \"visibility\": \"off\"" +
                "        }" +
                "    }," +
                "    {" +
                "        \"tags\": {" +
                "            \"any\": [" +
                "                \"path\"," +
                "                \"road_limited\"," +
                "                \"road_unclassified\"," +
                "                \"road_minor\"," +
                "                \"ferry\"," +
                "                \"crosswalk\"" +
                "            ]" +
                "        }," +
                "        \"stylers\": {" +
                "            \"visibility\": \"off\"" +
                "        }" +
                "    }," +
                "    {" +
                "        \"types\": [" +
                "            \"point\"," +
                "            \"polygon\"" +
                "        ]," +
                "        \"stylers\": {" +
                "            \"opacity\": 0" +
                "        }" +
                "    }," +
                "    {" +
                "        \"types\": [" +
                "            \"polyline\"" +
                "        ]," +
                "        \"tags\": {" +
                "            \"none\": [" +
                "                \"road_1\"," +
                "                \"road_2\"," +
                "                \"road_3\"," +
                "                \"road_4\"," +
                "                \"road_5\"," +
                "                \"road_6\"," +
                "                \"road_7\"" +
                "            ]" +
                "        }," +
                "        \"stylers\": {" +
                "            \"opacity\": 0" +
                "        }" +
                "    }," +
                "    {" +
                "        \"tags\": {" +
                "            \"any\": [" +
                "                \"land\"," +
                "                \"landcover\"," +
                "                \"urban_area\"" +
                "            ]" +
                "        }," +
                "        \"stylers\": {" +
                "            \"visibility\": \"off\"" +
                "        }" +
                "    }" +
                "]";
        mapView.getMapWindow().getMap().setMapStyle(style);
        mapView.getMapWindow().getMap().setNightModeEnabled(true);
        mapView.getMapWindow().getMap().set2DMode(false);
//        Logo logo = mapView.getMapWindow().getMap().getLogo();
//        logo.setPadding(new Padding(400, 400));
        MapKit mapKit = MapKitFactory.getInstance();
        userLocationLayer = mapKit.createUserLocationLayer(mapView.getMapWindow());
        userLocationLayer.setAutoZoomEnabled(true);
        userLocationLayer.setVisible(true);
        userLocationLayer.setHeadingEnabled(true);

        Log.d(TAG, "HudPresentation onCreate");
    }

    // Метод для конвертации dp в пиксели
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

}
