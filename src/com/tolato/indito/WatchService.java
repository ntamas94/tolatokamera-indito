package com.tolato.indito;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import java.io.*;

/**
 * Figyeli a rendszernaplót: a tolatókamera videójelének megjelenésekor előhozza a gyári
 * felvevő hátsó nézetét, a jel megszűnésekor visszavisz a főképernyőre.
 */
public class WatchService extends Service {
    static final String TAG = "TolatoIndito";
    static final String ACTION_CLOSE = "com.tolato.indito.CLOSE";
    static final String ACTION_RESTART_LOG = "com.tolato.indito.RESTART_LOG";
    static volatile boolean signalOn = false;
    private volatile boolean openedRecorder = false;
    private volatile java.lang.Process proc;
    private volatile Thread reader;
    private volatile long lastForeign = 0;   // utolsó más folyamattól jött naplósor ideje
    private volatile long lastKick = 0;
    private Handler main;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("watch", "Tolatókamera figyelés", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "watch")
                .setContentTitle("Tolatókamera indító").setContentText("Figyeli a hátramenetet")
                .setSmallIcon(android.R.drawable.ic_menu_camera).setOngoing(true).build();
        startForeground(1, n);
        main = new Handler(Looper.getMainLooper());
        main.postDelayed(watchdog, 10000);
    }

    @Override public int onStartCommand(Intent i, int f, int id) {
        // Az olvasót csak előtérből (KickActivity / MainActivity) indítjuk, mert csak akkor kap hozzáférést
        if (i != null && ACTION_RESTART_LOG.equals(i.getAction())) restartReader();
        else if (reader == null) {
            // root esetén nem kell előtérbe hozni semmit, közvetlenül indulhat az olvasó
            new Thread(() -> { if (hasRoot()) restartReader(); else main.post(this::kick); }).start();
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        main.removeCallbacks(watchdog);
        stopReader();
        super.onDestroy();
    }

    /** Ha 10 mp-ig nem jön más folyamattól sor, az olvasó "vak": újraindítjuk előtérből. */
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            long now = SystemClock.elapsedRealtime();
            if (now - lastForeign > 10000) {
                if (Boolean.TRUE.equals(root)) restartReader(); else kick();
            }
            main.postDelayed(this, 10000);
        }
    };

    private void kick() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastKick < 30000) return;   // legfeljebb 30 mp-enként
        lastKick = now;
        Log.i(TAG, "naplohozzaferes kerese (KickActivity)");
        try {
            Intent k = new Intent(this, KickActivity.class);
            k.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(k);
        } catch (Exception e) { Log.w(TAG, "kick hiba: " + e); }
    }

    private synchronized void stopReader() {
        Thread t = reader;
        reader = null;
        if (proc != null) { proc.destroy(); proc = null; }
        if (t != null) t.interrupt();
    }

    private synchronized void restartReader() {
        stopReader();
        Thread t = new Thread(this::loop, "logwatch");
        reader = t;
        t.start();
    }

    private void loop() {
        Thread me = Thread.currentThread();
        try {
            // Csak az új sorok (-T 1), csak a "zl" és "Chip6854" címkék.
            // Rootként olvasva nincs naplóhozzáférési ablak, és háttérből is működik.
            String lc = "logcat -v brief -T 1 -s zl:I Chip6854:E";
            String[] cmd = hasRoot()
                    ? new String[]{"su", "-c", lc}
                    : new String[]{"logcat", "-v", "brief", "-T", "1", "-s", "zl:I", "Chip6854:E"};
            java.lang.Process p = Runtime.getRuntime().exec(cmd);
            proc = p;
            Log.i(TAG, "naplofigyelo indult");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (reader == me && (line = r.readLine()) != null) {
                lastForeign = SystemClock.elapsedRealtime();
                if (line.contains("==Back==U_SIGNAL_ON===ints[0]=1")) onSignal(true);
                else if (line.contains("==Back==U_SIGNAL_ON===ints[0]=0")) onSignal(false);
                else if (line.contains("index : 3") && line.contains("hasSignal:true")) onSignal(true);
                else if (line.contains("index : 3") && line.contains("hasSignal:false")) onSignal(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "logcat hiba: " + e);
        }
    }

    private static volatile Boolean root;

    /** Van-e root (Magisk su)? Egyszer ellenőrzi, utána megjegyzi. */
    static boolean hasRoot() {
        if (root != null) return root;
        boolean ok = false;
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l = r.readLine();
            p.waitFor();
            ok = l != null && l.contains("uid=0");
        } catch (Exception ignored) {}
        root = ok;
        Log.i(TAG, "root: " + ok);
        return ok;
    }

    private synchronized void onSignal(boolean on) {
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
                // ha a felvevő az első kamerával jönne fel, a kisegítő szolgáltatás átváltja
                main.postDelayed(() -> {
                    AutoAllowService s = AutoAllowService.instance;
                    if (s != null) s.ensureRear(1);
                    else Log.w(TAG, "kisegito szolgaltatas nem fut");
                }, 800);
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
