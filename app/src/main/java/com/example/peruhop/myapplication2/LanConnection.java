package com.example.peruhop.myapplication2;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class LanConnection {

    private String address;
    private int port;
    private Socket socket;

    public LanConnection(String address, int port, Context context) {
        this.address = address;
        this.port = port;
        try {
            socket = new Socket(this.address, this.port);
        } catch (UnknownHostException e) {
            Toast.makeText(context, "Address doesn't exists", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(context, "Cant connect :(", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public Socket getConnection() {
        return socket;
    }

}
