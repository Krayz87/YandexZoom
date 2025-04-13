package sh.naeba.yandexzoom;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "YandexZoom";
    private static final String SETTINGS_NAME = "com.zeekr.ZoomApp";
    private static final String DISPLAY_SCALE_NAME = "displayScale";
    private static final String RESOURCES_SCALE_NAME = "resourcesScale";
    
    private static final int MIN_SCALE_PERCENT = 100;
    private static final int MAX_SCALE_PERCENT = 400;
    private static final int STEP_PERCENT = 10;
    
    private SeekBar displayScaleSeekBar;
    private SeekBar resourcesScaleSeekBar;
    private TextView displayScaleTextView;
    private TextView resourcesScaleTextView;
    private Button applyButton;
    
    private int displayScalePercent = 100;
    private int resourcesScalePercent = 100;
    
    // Коэффициент увеличения UI
    private static final float UI_SCALE_FACTOR = 2.0f;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Принудительно установить горизонтальную ориентацию
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Инициализация UI компонентов
        displayScaleSeekBar = findViewById(R.id.displayScaleSeekBar);
        resourcesScaleSeekBar = findViewById(R.id.resourcesScaleSeekBar);
        displayScaleTextView = findViewById(R.id.displayScaleTextView);
        resourcesScaleTextView = findViewById(R.id.resourcesScaleTextView);
        applyButton = findViewById(R.id.applyButton);

        
        // Настройка SeekBar для масштаба дисплея
        displayScaleSeekBar.setMax((MAX_SCALE_PERCENT - MIN_SCALE_PERCENT) / STEP_PERCENT);
        displayScaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                displayScalePercent = MIN_SCALE_PERCENT + (progress * STEP_PERCENT);
                updateDisplayScaleText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Настройка SeekBar для масштаба ресурсов
        resourcesScaleSeekBar.setMax((MAX_SCALE_PERCENT - MIN_SCALE_PERCENT) / STEP_PERCENT);
        resourcesScaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                resourcesScalePercent = MIN_SCALE_PERCENT + (progress * STEP_PERCENT);
                updateResourcesScaleText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Настройка кнопки применения
        applyButton.setOnClickListener(v -> applySettings());
        
        // Загрузка сохраненных настроек
        loadSettings();
    }
    
    private void updateDisplayScaleText() {
        displayScaleTextView.setText(getString(R.string.display_scale, displayScalePercent));
    }
    
    private void updateResourcesScaleText() {
        resourcesScaleTextView.setText(getString(R.string.resources_scale, resourcesScalePercent));
    }
    
    private void loadSettings() {
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        float displayScale = preferences.getFloat(DISPLAY_SCALE_NAME, 1.0f);
        float resourcesScale = preferences.getFloat(RESOURCES_SCALE_NAME, 1.0f);
        
        // Преобразуем float значения обратно в проценты
        displayScalePercent = (int) (displayScale * 100);
        resourcesScalePercent = (int) (resourcesScale * 100);
        
        // Установим позиции ползунков
        displayScaleSeekBar.setProgress((displayScalePercent - MIN_SCALE_PERCENT) / STEP_PERCENT);
        resourcesScaleSeekBar.setProgress((resourcesScalePercent - MIN_SCALE_PERCENT) / STEP_PERCENT);
        
        // Обновим текстовые отображения
        updateDisplayScaleText();
        updateResourcesScaleText();
    }
    
    private void applySettings() {
        // Преобразуем проценты в float значения
        float displayScale = displayScalePercent / 100.0f;
        float resourcesScale = resourcesScalePercent / 100.0f;
        
        // Сохраняем настройки
        SharedPreferences preferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(DISPLAY_SCALE_NAME, displayScale);
        editor.putFloat(RESOURCES_SCALE_NAME, resourcesScale);
        editor.apply();
        
        // Отправляем broadcast с настройками
        Intent intent = new Intent(SETTINGS_NAME);
        intent.putExtra(DISPLAY_SCALE_NAME, displayScale);
        intent.putExtra(RESOURCES_SCALE_NAME, resourcesScale);
        sendBroadcast(intent);
    }
}