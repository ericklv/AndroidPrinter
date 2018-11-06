package com.example.peruhop.printer;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitmapConvertor {

    private int mDataWidth;
    private byte mRawBitmapData[];
    private byte[] mDataArray;
    private static final String TAG = "BitmapConvertor";
    private ProgressDialog mPd;
    private Context mContext;
    private int mWidth, mHeight;
    private String mStatus;
    private String mFileName;
    private String mDirFile;
    private String storage = Environment.getExternalStorageDirectory().getAbsolutePath();
    private OutputStream outputStream;


    public BitmapConvertor(Context context, OutputStream outputStream) {
        // TODO Auto-generated constructor stub
        mContext = context;
        this.outputStream = outputStream;
    }

    /**
     * Converts the input image to 1bpp-monochrome bitmap
     *
     * @param inputBitmap : Bitmpa to be converted
     * @param fileName    : Save-As filename
     * @return :  Returns a String. Success when the file is saved on memory card or error.
     */
    public String convertBitmap(Bitmap inputBitmap, String fileName, String dirFile) {

        this.mWidth = inputBitmap.getWidth();
        this.mHeight = inputBitmap.getHeight();
        this.mFileName = fileName;
        this.mDirFile = dirFile;
        this.mDataWidth = ((mWidth + 31) / 32) * 4 * 8;
        this.mDataArray = new byte[(mDataWidth * mHeight)];
        this.mRawBitmapData = new byte[(mDataWidth * mHeight) / 8];
        ConvertInBackground convert = new ConvertInBackground();
        convert.execute(inputBitmap);
        return mStatus;

    }


    private void convertArgbToGrayscale(Bitmap bmpOriginal, int width, int height) {
        int pixel;
        int k = 0;
        int B = 0, G = 0, R = 0;
        try {
            for (int x = 0; x < height; x++) {
                for (int y = 0; y < width; y++, k++) {
                    // get one pixel color
                    pixel = bmpOriginal.getPixel(y, x);

                    // retrieve color of all channels
                    R = Color.red(pixel);
                    G = Color.green(pixel);
                    B = Color.blue(pixel);
                    // take conversion up to one single value by calculating pixel intensity.
                    R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);
                    // set new pixel color to output bitmap
                    if (R < 140) {
                        mDataArray[k] = 0;
                    } else {
                        mDataArray[k] = 1;
                    }
                }
                if (mDataWidth > width) {
                    for (int p = width; p < mDataWidth; p++, k++) {
                        mDataArray[k] = 1;
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, e.toString());
        }
    }

    private void createRawMonochromeData() {
        int length = 0;
        for (int i = 0; i < mDataArray.length; i = i + 8) {
            byte first = mDataArray[i];
            for (int j = 0; j < 7; j++) {
                byte second = (byte) ((first << 1) | mDataArray[i + j]);
                first = second;
            }
            mRawBitmapData[length] = first;
            length++;
        }
    }

    private String saveImage(String fileName, int width, int height) {
        FileOutputStream fileOutputStream;
        BMPFile bmpFile = new BMPFile();
        File myDir = new File(storage + "/" + mDirFile);
        File file = new File(myDir, fileName + ".bmp");
        try {
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return "Memory Access Denied";
        }
        bmpFile.saveBitmap(fileOutputStream, mRawBitmapData, width, height);
//        bmpFile.saveBitmap(fileOutputStream, mRawBitmapData, width, height);
        return "Success";
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

    class ConvertInBackground extends AsyncTask<Bitmap, String, Void> {

        @Override
        protected Void doInBackground(Bitmap... params) {
            // TODO Auto-generated method stub
            convertArgbToGrayscale(params[0], mWidth, mHeight);
            createRawMonochromeData();
            mStatus = saveImage(mFileName, mWidth, mHeight);
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            String path = storage + "/" + mDirFile + "/" + mFileName + ".bmp";
            printBoleta(path);
            try {
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mPd.dismiss();
            Toast.makeText(mContext, "Monochrome bitmap created successfully. Please check in sdcard", Toast.LENGTH_LONG).show();
        }


        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            mPd = ProgressDialog.show(mContext, "Converting Image", "Please Wait", true, false, null);
        }


    }
}