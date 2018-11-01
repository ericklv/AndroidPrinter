package com.example.peruhop.myapplication2;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class PrinterConstants {
    public static int PORT=9100,TOTAL_CHAR=45,DIV1=10,DIV2=5,DIV3=10,DIV4=10,DIV5=10;
    public static String IP="192.168.10.11";
    private OutputStream printer;
    public static final String UUID="00001101-0000-1000-8000-00805f9b34fb";
    public PrinterConstants(OutputStream printer){
        this.printer=printer;
    }

    public void printString(String str) throws IOException {
        Log.i("PRINTER_PRE",str);
        printer.write(str.getBytes());
        printer.write(0xA);
        printer.flush();
    }
    public void storeString(String str) throws IOException {
        printer.write(str.getBytes());

        printer.flush();
    }
    public void printStorage() throws IOException {
        printer.write(0xA);

        printer.flush();
    }
    public void feed(int feed) throws IOException {
        //escInit();
        printer.write(0x1B);
        printer.write("d".getBytes());
        printer.write(feed);printer.flush();

    }
    public void printAndFeed(String str, int feed) throws IOException {
        //escInit();
        printer.write(str.getBytes());
        printer.write(0x1B);
        printer.write("d".getBytes());
        printer.write(feed);printer.flush();

    }
    public void setBold(Boolean bool) throws IOException {
        printer.write(0x1B);
        printer.write("E".getBytes());
        printer.write((int)(bool?1:0));printer.flush();
    }
    /**
     * Sets white on black printing
     * */
    public void setInverse(Boolean bool) throws IOException {
        bool=false;
        printer.write(0x1D);
        printer.write("B".getBytes());
        printer.write( (int)(bool?1:0) );printer.flush();

    }
    public void resetToDefault() throws IOException {
        setInverse(false);
        setBold(false);
        setUnderline(0);
        setJustification(0);printer.flush();
    }
    /**
     * Sets underline and weight
     *
     * @param val
     *      0 = no underline.
     *      1 = single weight underline.
     *      2 = double weight underline.
     * */
    public void setUnderline(int val) throws IOException {
        printer.write(0x1B);
        printer.write("-".getBytes());
        printer.write(val);printer.flush();
    }
    /**
     * Sets left, center, right justification
     *
     * @param val
     *      0 = left justify.
     *      1 = center justify.
     *      2 = right justify.
     * */

    public void setJustification(int val) throws IOException {
        printer.write(0x1B);
        printer.write("a".getBytes());
        printer.write(val);
        printer.flush();
    }
    public void setLeftRight(String left,String right) throws IOException {
        printer.write(0x1B);
        printer.write("a".getBytes());
        printer.write(0);
        printString(left);

        printer.write(0x1B);
        printer.write("a".getBytes());
        printer.write(2);
        printString(right);

        printer.flush();
    }
    public void printBarcode(String code, int type, int h, int w, int font, int pos) throws IOException {

        //need to test for errors in length of code
        //also control for input type=0-6

        //GS H = HRI position
        printer.write(0x1D);
        printer.write("H".getBytes());
        printer.write(pos); //0=no print, 1=above, 2=below, 3=above & below

        //GS f = set barcode characters
        printer.write(0x1D);
        printer.write("f".getBytes());
        printer.write(font);

        //GS h = sets barcode height
        printer.write(0x1D);
        printer.write("h".getBytes());
        printer.write(h);

        //GS w = sets barcode width
        printer.write(0x1D);
        printer.write("w".getBytes());
        printer.write(w);//module = 1-6

        //GS k
        printer.write(0x1D); //GS
        printer.write("k".getBytes()); //k
        printer.write(type);//m = barcode type 0-6
        printer.write(code.length()); //length of encoded string
        printer.write(code.getBytes());//d1-dk
        printer.write(0);//print barcode

        printer.flush();
    }

    public void beep() throws IOException {
        printer.write(0x1B);
        printer.write("(A".getBytes());
        printer.write(4);
        printer.write(0);
        printer.write(48);
        printer.write(55);
        printer.write(3);
        printer.write(15);printer.flush();
    }

    public void setLineSpacing(int spacing) throws IOException {

        //function ESC 3
        printer.write(0x1B);
        printer.write("3".getBytes());
        printer.write(spacing);

    }
    public void cut() throws IOException {
        printer.write(0x1D);
        printer.write("V".getBytes());
        printer.write(48);
        printer.write(0);printer.flush();
    }
}