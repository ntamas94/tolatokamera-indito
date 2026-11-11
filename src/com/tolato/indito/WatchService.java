package com.tolato.indito;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import java.io.*;

/** Figyeli a rendszernaplót: a tolatókamera videójelének megjelenésekor megnyitja a kameranézetet. */
public class WatchService extends Service {
    static final String TAG = "TolatoIndito";
    static final String ACTION_CLOSE = "com.tolato.indito.CLOSE";
    static volatile boolean signalOn = false;
    private volatile boolean openedRecorder = false;
    private volatile boolean running = true;
    private java.lang.Process proc;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("watch", "Tolatókamera figyelés", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "watch")
                .setContentTitle("Tolatókamera indító").setContentText("Figyeli a hátramenetet")
                .setSmallIcon(android.R.drawable.ic_menu_camera).setOngoing(true).build();
        startForeground(1, n);
        new Thread(this::loop, "logwatch").start();
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        running = false;
        if (proc != null) proc.destroy();
        super.onDestroy();
    }

    private void loop() {
        while (running) {
            try {
                // Csak az új sorok (-T 1), csak a "zl" és "Chip6854" címkék
                proc = Runtime.getRuntime().exec(new String[]{"logcat", "-v", "brief", "-T", "1", "-s", "zl:I", "Chip6854:E"});
                BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while (running && (line = r.readLine()) != null) {
                    if (line.contains("==Back==U_SIGNAL_ON===ints[0]=1")) onSignal(true);
                    else if (line.contains("==Back==U_SIGNAL_ON===ints[0]=0")) onSignal(false);
                    else if (line.contains("index : 3") && line.contains("hasSignal:true")) onSignal(true);
                    else if (line.contains("index : 3") && line.contains("hasSignal:false")) onSignal(false);
                }
            } catch (Exception e) {
                Log.w(TAG, "logcat hiba: " + e);
            }
            SystemClock.sleep(2000);
        }
    }

    private void onSignal(boolean on) {
        if (on == signalOn) return;
        signalOn = on;
        Log.i(TAG, "tolatojel: " + on);
        try {
            if (on) {
                // A gyári felvevő (com.syu.doublecamera) a legutóbbi nézetet mutatja: hátsó kamera
                Intent i = new Intent("com.syu.doublecamera").setPackage("com.syu.doublecamera");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                openedRecorder = true;
            } else if (openedRecorder) {
                openedRecorder = false;
                Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(h);
            }
        } catch (Exception e) {
            Log.w(TAG, "inditas hiba: " + e);
        }
    }
}
