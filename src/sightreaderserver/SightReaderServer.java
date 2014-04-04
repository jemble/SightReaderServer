/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sightreaderserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeremy
 */
public class SightReaderServer {

    private static final int PORT_NUM = 1238;
    private static ServerSocket serverSocket;
    private static Socket clientConn;
    private static ArrayList<ClientThread> clientThreads;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
//        clientThreads = new ArrayList<ClientThread>();
        
        //create the server socket
        try{
            serverSocket = new ServerSocket(PORT_NUM);
            System.out.println("Creating socket "+PORT_NUM);
        }
        catch(IOException ex){
            System.out.println("Problem creating serversocket in main: "+ex.getLocalizedMessage());
        }
        
        //main loop
        while(true){
            
            //create client socket connection
            try {
                
                clientConn = serverSocket.accept();
                System.out.println("\n***************************** NEW CLIENT *************************************");
                System.out.println("Accepting connections from "+clientConn.getInetAddress());
            }
            catch(IOException ex){
                System.out.println("Problem creating client socket: "+ex.getLocalizedMessage());
            }
               
            ClientThread cThread = new ClientThread(clientConn);
            System.out.println("Starting processing thread");
            cThread.start();
            
        }
    }
    
}
