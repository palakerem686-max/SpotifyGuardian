package com.castrum.spotifyguardian;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Spotify Guardian");
        title.setTextSize(26);
        title.setPadding(0, 0, 0, pad / 2);
        root.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText("Spotify'ın medya oturumunu izler. Spotify beklenmedik şekilde kaybolursa bildirim verir ve dokunarak yeniden açmanı sağlar. OPPO'nun sistem düzeyindeki kapatmasını kesin olarak engelleyemez.");
        explanation.setTextSize(16);
        explanation.setPadding(0, 0, 0, pad);
        root.addView(explanation);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 0, 0, pad);
        root.addView(status);

        Button access = new Button(this);
        access.setText("1. Medya/bildirim erişimini aç");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access);

        Button start = new Button(this);
        start.setText("2. Koruyucuyu başlat");
        start.setOnClickListener(v -> {
            if (!hasNotificationAccess()) {
                status.setText("Önce medya/bildirim erişimini açman gerekiyor.");
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                return;
            }
            Intent service = new Intent(this, GuardianService.class);
            service.setAction(GuardianService.ACTION_START);
            startForegroundService(service);
            status.setText("Koruyucu çalışıyor. Üst bildirim çubuğunda kalıcı bildirim görünmeli.");
        });
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("Koruyucuyu durdur");
        stop.setOnClickListener(v -> stopService(new Intent(this, GuardianService.class)));
        root.addView(stop);

        setContentView(root);
    }

    private boolean hasNotificationAccess() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        ComponentName expected = new ComponentName(this, SpotifyNotificationListener.class);
        for (String item : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(item);
            if (expected.equals(component)) return true;
        }
        return false;
    }

    private void refreshStatus() {
        if (status == null) return;
        status.setText(hasNotificationAccess()
                ? "Erişim açık. Koruyucuyu başlatabilirsin."
                : "Medya/bildirim erişimi henüz verilmedi.");
    }
}
