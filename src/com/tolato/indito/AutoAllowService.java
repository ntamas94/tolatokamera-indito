package com.tolato.indito;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * Kisegítő szolgáltatás két feladattal:
 * 1. Az Android "Hozzáférés az összes eszköznaplóhoz?" ablakában, ha a Tolatókamera indítónak szól,
 *    megnyomja az egyszeri engedélyezés gombját.
 * 2. Hátramenetkor, ha a gyári felvevő az első kamerával jött fel, átváltja a hátsóra.
 */
public class AutoAllowService extends AccessibilityService {
    static final String TAG = WatchService.TAG;
    static volatile AutoAllowService instance;
    private static final String[] ALLOW = {
            "Allow one-time access", "Egyszeri hozzáférés engedélyezése", "Egyszeri hozzáférés"
    };
    // A felvevő képernyőelemei (2000x1200-as kijelzőn)
    private static final int ICON_X = 1950, FRONT_ARC_Y = 48, REAR_ARC_Y = 155;
    private static final int FLIP_X = 1270, FLIP_Y = 1115;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onServiceConnected() { instance = this; }
    @Override public boolean onUnbind(android.content.Intent i) { instance = null; return super.onUnbind(i); }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() == null || !"android".contentEquals(e.getPackageName())) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        if (root.findAccessibilityNodeInfosByText(getString(R.string.app_name)).isEmpty()) return;
        for (String label : ALLOW) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(label);
            for (AccessibilityNodeInfo n : nodes) {
                AccessibilityNodeInfo c = n;
                while (c != null && !c.isClickable()) c = c.getParent();
                if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "naplohozzaferes automatikusan engedelyezve");
                    return;
                }
            }
        }
    }

    @Override public void onInterrupt() {}

    /** A felvevő megnyitása után hívandó: ha az első kamera aktív, átvált a hátsóra (legfeljebb 3 próba). */
    void ensureRear(int attempt) {
        if (attempt > 3 || !WatchService.signalOn) return;
        takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
            @Override public void onSuccess(ScreenshotResult r) {
                Bitmap hw = Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(), r.getColorSpace());
                if (hw == null) return;
                Bitmap b = hw.copy(Bitmap.Config.ARGB_8888, false);
                r.getHardwareBuffer().close();
                boolean front = isBlue(b, ICON_X, FRONT_ARC_Y);
                boolean rear = isBlue(b, ICON_X, REAR_ARC_Y);
                Log.i(TAG, "felvevo nezet: elso=" + front + " hatso=" + rear + " (proba " + attempt + ")");
                if (front && !rear) {
                    tap(FLIP_X, FLIP_Y);
                    h.postDelayed(() -> ensureRear(attempt + 1), 1200);
                } else if (!front && !rear) {
                    // még nem rajzolódott ki: újra megnézzük
                    h.postDelayed(() -> ensureRear(attempt + 1), 700);
                }
            }
            @Override public void onFailure(int code) { Log.w(TAG, "kepernyokep hiba " + code); }
        });
    }

    /** Kék ív keresése a pont körül (±12 px). */
    private static boolean isBlue(Bitmap b, int cx, int cy) {
        for (int dx = -12; dx <= 12; dx += 4) for (int dy = -12; dy <= 12; dy += 4) {
            int x = cx + dx, y = cy + dy;
            if (x < 0 || y < 0 || x >= b.getWidth() || y >= b.getHeight()) continue;
            int p = b.getPixel(x, y);
            if (Color.blue(p) > 180 && Color.red(p) < 120 && Color.blue(p) - Color.red(p) > 100) return true;
        }
        return false;
    }

    private void tap(int x, int y) {
        Path p = new Path();
        p.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null);
        Log.i(TAG, "atvaltas hatso kamerara (koppintas)");
    }
}
