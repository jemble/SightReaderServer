/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sightreaderserver;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 *
 * @author jeremy
 */
public class StreamHandler extends Thread {

    InputStream is;
    String type;
    OutputStream os;
    Logger logger;

    StreamHandler(InputStream is, String type) {
        this(is, type, null);
    }

    StreamHandler(InputStream is, String type, OutputStream redirect) {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }

    public void run() {
        try {
            if (os == null) {
                InputStreamReader isr = new InputStreamReader(is);

                BufferedReader br = new BufferedReader(isr);
                String line = null;
                
                while ((line = br.readLine()) != null) { 
                    System.out.println(line);
                    if(line.contains("Processing stopped")){     
                        ClientThread.setCurStatus(ClientThread.STATUS_AUDI_PROB);
                    }
                    else if(line.contains("Illegal")){
                        ClientThread.setCurStatus(ClientThread.STATUS_XML_PROB);
                    }
                }
            }
            else {
                
                byte[] data = new byte[3072];
                BufferedInputStream bis = new BufferedInputStream(is);
                bis.read(data);
                os.write(data);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

