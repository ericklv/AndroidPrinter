package com.example.peruhop.printer;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;


public class IntentPrinterService extends IntentService {

    public IntentPrinterService() {
        super("IntentPrinterService");
    }

    public static final String ACTION_PROGRESO = "net.sgoliver.intent.action.PROGRESO";
    public static final String ACTION_FIN = "net.sgoliver.intent.action.FIN";

    @Override
    protected void onHandleIntent(Intent intent) {

        int iter = intent.getIntExtra("iteraciones", 0);

        for (int i = 1; i <= iter; i++) {
            tareaLarga();
            Log.d("intentservice","progress"+i*10);
            //Comunicamos el progreso
            Intent intentPrinterService = new Intent();
            intentPrinterService.setAction(ACTION_PROGRESO);
            intentPrinterService.putExtra("progreso", i * 10);
            sendBroadcast(intentPrinterService);
        }

        Intent bcIntent = new Intent();
        bcIntent.setAction(ACTION_FIN);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onDestroy(){
        Log.d("intentservice","destroy");
    }

    private void tareaLarga() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }


}
