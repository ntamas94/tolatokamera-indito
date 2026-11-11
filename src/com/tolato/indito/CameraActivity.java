package com.tolato.indito;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import java.util.Arrays;

/** Teljes képernyős tolatókamera-kép a 14-es kameráról, segédvonalakkal. */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {
    static final String TAG = WatchService.TAG;
    static final String CAM_ID = "14";
    private SurfaceView sv;
    private Surface surface;
    private CameraDevice cam;
    private CameraCaptureSession session;
    private Handler bg;
    private int attempts = 0;

    private final BroadcastReceiver closer = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finish(); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        sv = new SurfaceView(this);
        sv.getHolder().setFixedSize(1280, 720);
        sv.getHolder().addCallback(this);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        GuideView gv = new GuideView(this);
        root.addView(gv, new FrameLayout.LayoutParams(-1, -1));
        Log.i(TAG, "CameraActivity onCreate");
        root.setOnClickListener(v -> finish());
        setContentView(root);
        HandlerThread ht = new HandlerThread("cam");
        ht.start();
        bg = new Handler(ht.getLooper());
        registerReceiver(closer, new IntentFilter(WatchService.ACTION_CLOSE), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(closer); } catch (Exception ignored) {}
        closeCam();
        super.onDestroy();
    }

    @Override public void surfaceCreated(SurfaceHolder h) {
        surface = h.getSurface();
        Log.i(TAG, "surface kesz");
        openCam();
    }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    @Override public void surfaceDestroyed(SurfaceHolder h) { closeCam(); surface = null; }

    private void openCam() {
        try {
            CameraManager cm = getSystemService(CameraManager.class);
            Log.i(TAG, "kamerak: " + Arrays.toString(cm.getCameraIdList()) + ", nyitas: " + CAM_ID);
            cm.openCamera(CAM_ID, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice d) { cam = d; startPreview(); }
                @Override public void onDisconnected(CameraDevice d) { d.close(); cam = null; retry(); }
                @Override public void onError(CameraDevice d, int e) { Log.w(TAG, "kamera hiba " + e); d.close(); cam = null; retry(); }
            }, bg);
        } catch (Exception e) {
            Log.w(TAG, "openCamera: " + e);
            retry();
        }
    }

    private void retry() {
        if (isFinishing()) return;
        if (++attempts > 8) { fallback(); return; }
        bg.postDelayed(this::openCam, 500);
    }

    /** Ha a kamera nem nyitható meg, a gyári felvevőt hozza elő. */
    private void fallback() {
        Log.w(TAG, "tartalek: gyari felvevo");
        try {
            Intent i = new Intent("com.syu.doublecamera").setPackage("com.syu.doublecamera");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) { Log.w(TAG, "fallback: " + e); }
        finish();
    }

    private void startPreview() {
        try {
            Log.i(TAG, "kamera megnyitva");
            Surface s = surface;
            if (s == null) { retry(); return; }
            final CaptureRequest.Builder rb = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            rb.addTarget(s);
            cam.createCaptureSession(Arrays.asList(s), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession cs) {
                    session = cs;
                    try { cs.setRepeatingRequest(rb.build(), null, bg); Log.i(TAG, "elonezet fut"); }
                    catch (Exception e) { Log.w(TAG, "repeating: " + e); retry(); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession cs) { Log.w(TAG, "config failed"); retry(); }
            }, bg);
        } catch (Exception e) { Log.w(TAG, "preview: " + e); retry(); }
    }

    private void closeCam() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (cam != null) cam.close(); } catch (Exception ignored) {}
        session = null;
        cam = null;
    }

    /** Egyszerű színes parkolási segédvonalak. */
    static class GuideView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        GuideView(Context c) { super(c); p.setStrokeWidth(8); p.setStyle(Paint.Style.STROKE); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            int[] col = {Color.RED, Color.YELLOW, Color.GREEN};
            float[] y = {h * 0.95f, h * 0.80f, h * 0.67f, h * 0.58f};
            for (int i = 0; i < 3; i++) {
                float t0 = (h - y[i]) / (h * 0.42f), t1 = (h - y[i + 1]) / (h * 0.42f);
                float xl0 = w * (0.22f + 0.16f * t0), xl1 = w * (0.22f + 0.16f * t1);
                float xr0 = w - xl0, xr1 = w - xl1;
                p.setColor(col[i]);
                c.drawLine(xl0, y[i], xl1, y[i + 1], p);
                c.drawLine(xr0, y[i], xr1, y[i + 1], p);
                c.drawLine(xl1, y[i + 1], xl1 + w * 0.04f, y[i + 1], p);
                c.drawLine(xr1, y[i + 1], xr1 - w * 0.04f, y[i + 1], p);
            }
        }
    }
}
