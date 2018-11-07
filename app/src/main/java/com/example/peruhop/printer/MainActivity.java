package com.example.peruhop.printer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText address;
    private EditText port;
    private TextView connectMessage;
    private Socket socket;
    private LanConnection lanConnection;
    private static OutputStream outputStream;
    private JSONObject ticket = new JSONObject();
    private JSONObject items = new JSONObject();
    private JSONObject item = new JSONObject();
    private JSONObject item1 = new JSONObject();
    private JSONObject item2 = new JSONObject();
    private FirestoreRepository firestore;
    private File file;
    private String encodedString;
    String storage = Environment.getExternalStorageDirectory().getAbsolutePath();
    String pdf = "ticket.pdf";
    String path = storage + "/" + pdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        address = (EditText) findViewById(R.id.currentIP);
        port = (EditText) findViewById(R.id.port);
        connectMessage = (TextView) findViewById(R.id.connectMessage);

        createJSON();

        file = new File(path);
        encodedString = new String(encodeFileToBase64Binary(path), StandardCharsets.US_ASCII);
    }

    public Toast customToast(String text) {
        Toast toast = new Toast(this);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

        TextView textView = new TextView(MainActivity.this);
        textView.setBackgroundColor(Color.argb(200, 99, 110, 114));
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(20);
        textView.setPadding(15, 10, 15, 10);
        textView.setText(text);
        toast.setView(textView);

        return toast;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void connectPrinter(View view) {
        String _address = address.getText().toString();
        String _port = port.getText().toString();
        this.connectMessage.setText(_address + ":" + _port);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        lanConnection = new LanConnection(_address, Integer.parseInt(_port), this);
        if (lanConnection.getConnection() == null) {
            Utils.customToast(MainActivity.this, "Cant connect to IP");
        } else {

            firestoreTest();
        }
    }

    private void createJSON() {
        try {
            item.put("name", "Tequenos");
            item.put("price", "10.00");
            item1.put("name", "Whisky on the rocks");
            item1.put("price", "18.00");
            item2.put("name", "Sex on the beach");
            item2.put("price", "15.00");
            items.put("1", item);
            items.put("2", item1);
            items.put("3", item2);
            ticket.put("company", "Wild Rover Huacachina");
            ticket.put("service", "Restaurant");
            ticket.put("collaborator", "Raul Montesinos");
            ticket.put("collaboratorRol", "Bartender");
            ticket.put("client", "Alberto Smith");
            ticket.put("items", items);
            ticket.put("Total", "78.00");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void printBill() {
        if (lanConnection.getConnection() == null) {
            this.connectMessage.setText("Address doesnt exist");
        } else {
            OutputStream opstream = null;

            try {
                opstream = lanConnection.getConnection().getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                this.connectMessage.setText("Cant connect to printer ");
            }
            outputStream = opstream;

            try {
                outputStream = lanConnection.getConnection().getOutputStream();
                byte[] printformat = new byte[]{0x1B, 0x21, 0x03};
                outputStream.write(printformat);
                printNewLine();
                printCustom(ticket.getString("company"), 2, 1);
                printCustom(ticket.getString("service"), 1, 1);
                printCustom(leftRightAlign(ticket.getString("collaboratorRol"), ticket.getString("collaborator")), 0, 1);
                printNewLine();
                printCustom(leftRightAlign("Client:", ticket.getString("client")), 0, 1);
                printNewLine();
                printCustom("Products", 1, 1);
                try {
                    JSONObject jsonChildObject = (JSONObject) ticket.get("items");
                    Iterator iterator = jsonChildObject.keys();
                    String key = null;
                    Double total = 0.0;
                    while (iterator.hasNext()) {
                        key = (String) iterator.next();
                        String name = ((JSONObject) jsonChildObject.get(key)).getString("name");
                        String price = ((JSONObject) jsonChildObject.get(key))
                                .getString("price")
                                .replace(" ", "")
                                .replace(",", ".");
                        printCustom(leftRightAlign(name, price), 0, 1);
                        total += Double.parseDouble(price);
                    }
                    printCustom(leftRightAlign("Total:", String.format("%.2f", total)), 1, 1);
                    customToast(String.format("%.2f", total)).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                printCustom("Thank you for coming :D", 3, 1);
                printNewLine();

                outputStream.flush();
            } catch (IOException e) {
                customToast("ioexception").show();
                e.printStackTrace();
            } catch (JSONException e) {
                customToast("JSON").show();
                e.printStackTrace();
            }
        }
    }


    private void recieveFile(File file, LanConnection connection) {
        if (connection.getConnection() == null) {
            this.connectMessage.setText("Lost Connection :'(");
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
                    BitmapConvertor bitmapConvertor = new BitmapConvertor(this, outputStream);
                    bitmapConvertor.convertBitmap(page, "voucher" + pageCounter, "PeruHopPrinter");
                    cutPrint(outputStream);
//                    outputStream.flush();
                    pageCounter++;
                }
            } catch (IOException e) {
                customToast("ioexception").show();
                e.printStackTrace();
            }
        }
    }

    private static byte[] encodeFileToBase64Binary(String fileName) {

        File file = new File(fileName);
        byte[] bytes = new byte[0];
        try {
            bytes = loadFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] encoded = Base64.encode(bytes, Base64.DEFAULT);

        return encoded;
    }

    private static byte[] loadFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            Log.d("TAG", "Too large");

        }
        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
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
                                String tempFilePath = storage + "/" + dc.getDocument().getId() + ".pdf";
                                File pdf64 = new File(base64toFile(String.valueOf(dc.getDocument().get("pdf")), tempFilePath));
                                switch (dc.getType()) {
                                    case ADDED:
                                        Utils.customToast(MainActivity.this,"new print").show();
//                                        recieveFile(pdf64,lanConnection);
                                        Log.d("TAG", "New Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                    case MODIFIED:
                                        Utils.customToast(MainActivity.this,"update" + dc.getDocument().getId()).show();
//                                        recieveFile(pdf64,lanConnection);
                                        Log.d("TAG", "Modified Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                    case REMOVED:
                                        Utils.customToast(MainActivity.this,"remove").show();
                                        Log.d("TAG", "Removed Msg: " + dc.getDocument().toObject(Message.class));
                                        break;
                                }
                            }
                        }

                    }
                });
    }


    private void cutPrint(OutputStream outputStream) throws IOException {
        printNewLine();
        printNewLine();
        printNewLine();
        printNewLine();
        outputStream.write(0x1D);
        outputStream.write("V".getBytes());
        outputStream.write(48);
        outputStream.write(0);
        outputStream.flush();
    }

    private void printBoleta(String path) {
        byte[] sendData = null;
        PrintPic pg = new PrintPic();
        pg.initCanvas(800);
        pg.initPaint();
        pg.drawImage(-20, 0, path);
        sendData = pg.printDraw();
        try {
            outputStream.write(sendData);
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File generateImage(Bitmap finalBitmap) {

        String root = storage;
        File myDir = new File(root + "/PeruHopPrinter");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
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

    private Bitmap base64toBitmap(String file) {
        Log.d("TAG", "base64 to Bitmap: " + file.length());
        byte[] decodedStringFile = Base64.decode(file, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedStringFile, 0, decodedStringFile.length);
        return decodedByte;
    }

    private String base64toFile(String encodedString, String pathFile) {
        try {
            File file = new File(pathFile);
            file.createNewFile();
            byte[] byteArray = Base64.decode(encodedString, 0);
            FileOutputStream fileOuputStream = new FileOutputStream(pathFile);
            fileOuputStream.write(byteArray);
            fileOuputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pathFile;
    }

    private Bitmap convertMonochrome(Bitmap bmpSrc, int width, int height) {
        Bitmap bmpMonochrome = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpMonochrome);
        ColorMatrix ma = new ColorMatrix();
        ma.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(ma));
        canvas.drawBitmap(bmpSrc, 0, 0, paint);
        return bmpMonochrome;
    }


    public void printPhoto(int img) {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    img);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    private void printText(String msg) {
        try {
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void printText(byte[] msg) {
        try {
            outputStream.write(msg);
            printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 48) {
            int n = 48 - ans.length();
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
        }
        return ans;
    }

    private void printNewLine() {
        try {
            outputStream.write(PrinterCommands.FEED_LINE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[2];
        dateTime[0] = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.YEAR);
        dateTime[1] = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        return dateTime;
    }

    public static void resetPrint() {
        try {
            outputStream.write(PrinterCommands.ESC_FONT_COLOR_DEFAULT);
            outputStream.write(PrinterCommands.FS_FONT_ALIGN);
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
            outputStream.write(PrinterCommands.ESC_CANCEL_BOLD);
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printCustom(String msg, int size, int align) {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        try {
            switch (size) {
                case 0:
                    outputStream.write(cc);
                    break;
                case 1:
                    outputStream.write(bb);
                    break;
                case 2:
                    outputStream.write(bb2);
                    break;
                case 3:
                    outputStream.write(bb3);
                    break;
            }

            switch (align) {
                case 0:
                    //left align
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            outputStream.write(msg.getBytes());
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void printerTest(View view) {
        File pdf64 = new File(base64toFile(encodedString, storage + "/pdf.pdf"));
        recieveFile(pdf64, lanConnection);
    }
}
