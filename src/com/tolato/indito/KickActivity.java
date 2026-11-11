package com.tolato.indito;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Láthatatlan, rövid életű ablak: amíg előtérben van, (újra)indítja a naplóolvasót,
 * mert az Android 13 csak előtérben lévő alkalmazásnak ad naplóhozzáférést.
 */
public class KickActivity extends Activity {
    @Override protected void onResume() {
        super.onResume();
        startForegroundService(new Intent(this, WatchService.class).setAction(WatchService.ACTION_RESTART_LOG));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
            overridePendingTransition(0, 0);
        }, 1500);
    }
}
