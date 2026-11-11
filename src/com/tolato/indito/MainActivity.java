package com.tolato.indito;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        startForegroundService(new Intent(this, WatchService.class).setAction(WatchService.ACTION_RESTART_LOG));
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(40, 40, 40, 40);
        TextView t = new TextView(this);
        t.setTextSize(28);
        t.setGravity(Gravity.CENTER);
        t.setText("Tolatókamera indító fut.\nHátramenetben magától előhozza a gyári felvevő hátsó kamera nézetét,\nutána visszavisz a főképernyőre.\n\nFontos: a felvevőben a hátsó kamera nézet legyen kiválasztva.");
        l.addView(t);
        setContentView(l);
    }
}
