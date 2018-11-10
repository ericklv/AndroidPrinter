package com.example.peruhop.printer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class PrinterService extends Service {
    private FirestoreRepository firestore;
    private Context context = this;
    private LanConnection lanConnection;
    public PrinterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    public void onCreate(){
        super.onCreate();
    }
//    public void onStart(Intent intent, int startId){
//        Log.d("PrinterService","Te la creiste we");
////        this.stopSelf();
//    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String ip = intent.getStringExtra("ip");
        int port = intent.getIntExtra("port",9100);

        lanConnection = new LanConnection(ip, port, this);
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        Log.d("Service","port:"+port);
        firestoreTest();
        return START_NOT_STICKY;
    }


    public void onDestroy(){
        Log.d("PrinterService","Service is down ");
        super.onDestroy();
    }


    private void firestoreTest() {
        firestore = new FirestoreRepository();
        firestore.getFirestore().collection("printJobs")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("TAG", "listen: connection error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (!Boolean.parseBoolean(dc.getDocument().get("printed").toString())) {
                                String tempFilePath = MainActivity.storage + "/PeruHopPrinter/" + dc.getDocument().getId() + ".pdf";
                                File pdf64 = new File(MainActivity.base64toFile(String.valueOf(dc.getDocument().get("pdf")), tempFilePath));
                                switch (dc.getType()) {
                                    case ADDED:
//                                        Utils.customToast(context,"new print service").show();
                                        recieveFile(pdf64,lanConnection);
                                        DocumentReference documentReference = firestore.getFirestore().collection("printJobs").document(dc.getDocument().getId());
                                        documentReference.update("printed", true);
                                        Log.d("TAG", "New Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                    case MODIFIED:
                                        Utils.customToast(context,"update service" + dc.getDocument().getId(),0).show();
//                                        recieveFile(pdf64,lanConnection);
                                        Log.d("TAG", "Modified Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                    case REMOVED:
                                        Utils.customToast(context,"remove service",0).show();
                                        Log.d("TAG", "Removed Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                }
                            }
                        }

                    }
                });
    }

    private void recieveFile(File file, LanConnection connection) {
        OutputStream outputStream;
        if (connection.getConnection() == null) {
            Log.d("recieveFile","connection null");
        } else {
            try {
                outputStream = connection.getConnection().getOutputStream();
                ArrayList<Bitmap> pages = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    pages = pdfToBitmap(file);
                }
                int pageCounter = 0;
                for (Bitmap page : pages) {
                    /*Este genera la imagen */
                    BitmapConvertor bitmapConvertor = new BitmapConvertor(context, outputStream);
                    bitmapConvertor.convertBitmap(page, "voucher" + pageCounter, "PeruHopPrinter");
//                    cutPrint(outputStream);
//                    outputStream.flush();
                    pageCounter++;
                }
            } catch (IOException e) {
                Log.d("recieveFile","ioException");
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ArrayList<Bitmap> pdfToBitmap(File pdfFile) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));
            Bitmap bitmap;
            final int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
//                int width = getResources().getDisplayMetrics().densityDpi / 120 * page.getWidth();
//                int height = getResources().getDisplayMetrics().densityDpi / 120 * page.getHeight();
                int width = 600;
                int height = width * page.getHeight() / page.getWidth();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(bitmap, 0, 0, null);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                bitmaps.add(bitmap); // close the page
                page.close();
            } // close the renderer
            renderer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitmaps;
    }
}
