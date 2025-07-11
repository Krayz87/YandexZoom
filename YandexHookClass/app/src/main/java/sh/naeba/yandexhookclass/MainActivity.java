package sh.naeba.yandexhookclass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    // Существующие actions для манёвров
    private static final String ACTION_MANEUVER_VISIBILITY = "ru.yandex.yandexnavi.MANEUVER_VISIBILITY";
    private static final String ACTION_MANEUVER_UPDATE = "ru.yandex.yandexnavi.MANEUVER_UPDATE";
    
    // Новые actions для GuidanceBinding
    private static final String ACTION_GUIDANCE_DATA_RECEIVED = "egel.app.GUIDANCE_DATA_RECEIVED";
    
    private BroadcastReceiver maneuverReceiver;
    private BroadcastReceiver guidanceDataReceiver;
    private BroadcastReceiver guidanceEventReceiver; // Новый receiver для событий навигации

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

    }
}