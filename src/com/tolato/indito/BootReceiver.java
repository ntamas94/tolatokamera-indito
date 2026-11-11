package com.tolato.indito;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        c.startForegroundService(new Intent(c, WatchService.class));
    }
}
