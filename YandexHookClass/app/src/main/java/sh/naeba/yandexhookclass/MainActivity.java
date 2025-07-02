package sh.naeba.yandexhookclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ACTION_MANEUVER_VISIBILITY = "ru.yandex.yandexnavi.MANEUVER_VISIBILITY";
    private static final String ACTION_MANEUVER_UPDATE = "ru.yandex.yandexnavi.MANEUVER_UPDATE";
    
    private BroadcastReceiver maneuverReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Регистрируем приёмник для тестирования хуков
        registerManeuverReceiver();
        
        Log.i(TAG, "MainActivity создана, BroadcastReceiver зарегистрирован");
    }
    
    private void registerManeuverReceiver() {
        maneuverReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (ACTION_MANEUVER_VISIBILITY.equals(action)) {
                    boolean isVisible = intent.getBooleanExtra("is_visible", false);
                    int componentId = intent.getIntExtra("component_id", -1);
                    String componentClass = intent.getStringExtra("component_class");
                    
                    Log.i(TAG, "=== ПОЛУЧЕН BROADCAST setContentVisible ===");
                    Log.i(TAG, "Component ID: " + componentId);
                    Log.i(TAG, "Component Class: " + componentClass);
                    Log.i(TAG, "Is Visible: " + isVisible);
                    Log.i(TAG, "=======================================");
                    
                } else if (ACTION_MANEUVER_UPDATE.equals(action)) {
                    String distance = intent.getStringExtra("distance");
                    String unit = intent.getStringExtra("metrics"); // unit передаётся в metrics
                    String roadName = intent.getStringExtra("road_name");
                    String imageId = intent.getStringExtra("image_id");
                    String maneuverImage = intent.getStringExtra("maneuver_image");
                    boolean isVisible = intent.getBooleanExtra("is_visible", false);
                    
                    Log.i(TAG, "=== ПОЛУЧЕН BROADCAST setManeuver ===");
                    Log.i(TAG, "Distance: " + distance);
                    Log.i(TAG, "Unit: " + unit);
                    Log.i(TAG, "Road Name: " + roadName);
                    Log.i(TAG, "Image ID: " + imageId);
                    Log.i(TAG, "Is Visible: " + isVisible);
                    if (maneuverImage != null && !maneuverImage.isEmpty()) {
                        Log.i(TAG, "Maneuver Image: Base64 данные получены (" + maneuverImage.length() + " символов)");
                    } else {
                        Log.i(TAG, "Maneuver Image: не получено");
                    }
                    Log.i(TAG, "===================================");
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MANEUVER_VISIBILITY);
        filter.addAction(ACTION_MANEUVER_UPDATE);
        registerReceiver(maneuverReceiver, filter);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (maneuverReceiver != null) {
            unregisterReceiver(maneuverReceiver);
            Log.i(TAG, "BroadcastReceiver отключён");
        }
    }
}