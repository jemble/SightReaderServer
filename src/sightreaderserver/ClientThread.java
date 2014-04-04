/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sightreaderserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author jeremy
 */
public class ClientThread extends Thread {
    public static final int STATUS_OK = 100;
    public static final int STATUS_AUDI_PROB = 101;
    public static final int STATUS_XML_PROB = 102;
    public static final int STATUS_GENERAL_PROB = 103;
    private static int curStatus = STATUS_OK;
    
    private static final String FILE_LOC = "/home/appFiles/";
    
    private static final String LOCAL_DIST_LOC = "/usr/bin/audiveris/AudiverisApp/dist/";
    private static final String REMOTE_DIST_LOC = "/usr/share/audiveris/dist/";
    
    private static final String DIST_LOC = REMOTE_DIST_LOC;
    
    private static ObjectInputStream objInStream = null;
    private static ObjectOutputStream objOutStream = null;
    private static Socket clientConn = null;
    private static String fileName;
    private static String tempo;
    
    public ClientThread(Socket clientConn){
        this.clientConn = clientConn;
    }
    
    public void run(){
        
        //create our streams
        try{
            createObjectStreams();
        }
        catch(IOException ex){
            System.out.println("problem creating streams: "+ex.getLocalizedMessage());
        }
        
        //get the filename from the client
        try{
            fileName = receiveMessage();
        }
        catch(ClassNotFoundException ex){
            System.out.println("No class definition found when getting filename: "+ex.getLocalizedMessage());
        }
        catch(IOException ex){
            System.out.println("Communication error when getting filename: "+ex.getLocalizedMessage());
        }
        
        //get the tempo from the client
        try{
            tempo = receiveMessage();
        }
        catch(ClassNotFoundException ex){
            System.out.println("No class definition found when getting tempo: "+ex.getLocalizedMessage());
        }
        catch(IOException ex){
            System.out.println("Communication error when getting tempo: "+ex.getLocalizedMessage());
        }
        
        //write the xml to file
        try{
            writeXml();
        }
        catch(FileNotFoundException ex){
            System.out.println("Could not file found when writing xml: "+ex.getLocalizedMessage());
        }
        
        //send current status to client
        try{
            sendCurrentStatus();
        }
        catch(IOException ex){
            System.out.println("Problem sending current status: "+ex.getLocalizedMessage());
        }
        
        //get the picture file
        try{
            getFile();
        }
        catch(ClassNotFoundException ex){
            System.out.println("Class not found when getting image file: "+ex.getLocalizedMessage());
        }
        catch(IOException ex){
            System.out.println("Comms error when getting image file: "+ex.getLocalizedMessage());
        }
        
        //send current status to client
        try{
            sendCurrentStatus();
        }
        catch(IOException ex){
            System.out.println("problem sending current status: "+ex.getLocalizedMessage());
        }
        
        //process with audiveris
        try{
            System.out.println("Processing with audiveris");
            runCommand(createCommandString(1), false);
        }
        catch(IOException ex){
            System.out.println("Comms error when running audiveris command: "+ex.getLocalizedMessage());
        }
        catch(InterruptedException ex){
            System.out.println("The process was interrupted when running audiveris command: "+ex.getLocalizedMessage());
            curStatus = STATUS_AUDI_PROB;
        }
        
        //process with musicxml2mid
        if(curStatus == STATUS_OK){
            try{
                System.out.println("Processing with musicxmlmid");
                runCommand(createCommandString(2),true);
            }
            catch(IOException ex){
                System.out.println("Comms error when running musicxml2mid command: "+ex.getLocalizedMessage());
            }
            catch(InterruptedException ex){
                System.out.println("The process was interrupted when running musicxml2mid command: "+ex.getLocalizedMessage());
            }
        }
        
        //send status to client
        try{
            sendCurrentStatus();
        }
        catch(IOException ex){
            System.out.println("Comms error when sending status: "+ex.getLocalizedMessage());
        }
        
        //send the midi file to the client
        try{
            sendFile(FILE_LOC+fileName+".midi");
        }
        catch(IOException ex){
            System.out.println("Comms problem when trying to send midi: "+ex.getLocalizedMessage());
        }
        
        //clean up
        try{
            cleanUp();
        }
        catch(IOException ex){
            System.out.println("Problem closing the streams: "+ex.getLocalizedMessage());
        }
        
    }
        
    
    public static void setCurStatus(int status){
        curStatus = status;
        System.out.println("current status: "+curStatus);
    }
    
    /**
     * Creates the object input and output streams
     * @throws IOException 
     */
    private static void createObjectStreams() throws IOException{
        objInStream = new ObjectInputStream(clientConn.getInputStream());
        objOutStream = new ObjectOutputStream(clientConn.getOutputStream());
        System.out.println("Creating streams");
    }
    
    /**
     * gets a string message from the client
     * @return the string sent from the client
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private static String receiveMessage() throws IOException, ClassNotFoundException{
        String msg = objInStream.readObject().toString();
        System.out.println("received message: "+msg);
        return msg;
    }
    
    /**
     * Sends the current status to the client
     * @throws IOException 
     */
    private static void sendCurrentStatus() throws IOException{
        objOutStream.writeInt(curStatus);
        objOutStream.flush();
        System.out.println("sending status: "+curStatus);
    }
     
    /**
     * Creates an xml file using the fileName class variable
     * @param xml the string to write to the xml file
     * @throws FileNotFoundException 
     */
    private static void writeXml() throws FileNotFoundException{
        PrintWriter outWriter = new PrintWriter(FILE_LOC+"script_"+fileName+".xml");
        String xml = makeXmlString();
        outWriter.println(xml);
        outWriter.flush();
        outWriter.close();
    }
    
    private static String makeXmlString(){
        String xml = "";
        xml += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        xml += "<script file=\""+FILE_LOC+fileName+".jpg\">";
        xml += "<parameters><language>eng</language>";
        xml += "<tempo>"+tempo+"</tempo>";
        xml += "<adaptive-filter mean-coeff=\"0.7\" std-dev-coeff=\"0.9\"/>";
        xml += "</parameters><step name=\"SCORE\"/>";
        xml += "<export path=\""+FILE_LOC+fileName+".xml\"/>";
        xml += "</script>";
        System.out.println("xml generated: "+xml);
        return xml;
    }
    
    /**
     * gets the image file from the client and writes it to disk (as a jpg)
     * using the fileName variable.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private static void getFile() throws FileNotFoundException, IOException, ClassNotFoundException{
        byte[] byteArray = (byte[])objInStream.readObject();
        FileOutputStream mediaStream = new FileOutputStream(FILE_LOC+fileName+".jpg");
        mediaStream.write(byteArray);
        mediaStream.close();
        System.out.println("getting file "+FILE_LOC+fileName+".jpg");
    }
    
    /**
     * sends the midi file to the client
     * @param file the name and path of the file to send
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static void sendFile(String file) throws FileNotFoundException, IOException{
        FileInputStream fileIn = new FileInputStream(file);
        long fileLen = (new File(file)).length();
        int intFileLen = (int)fileLen;
        byte[] byteArray = new byte[intFileLen];
        fileIn.read(byteArray);
        fileIn.close();
        objOutStream.writeObject(byteArray);
        objOutStream.flush();
        System.out.println("Sending file "+file + " of length: "+fileLen);
    }
    
    /**
     * starts a process with the specified commands
     * @param commands the commands to run
     * @param pipeOutput whether the output is piped to another process
     * @throws IOException
     * @throws InterruptedException 
     */
    private static void runCommand(String[] commands, boolean pipeOutput) throws IOException, InterruptedException{
        FileOutputStream fos = null;
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(commands);
        StreamHandler errHandler = new StreamHandler(proc.getErrorStream(),"INFO");
        errHandler.start();
        
        if(pipeOutput){
            fos = new FileOutputStream(FILE_LOC+fileName+".midi");
            StreamHandler outHandler = new StreamHandler(proc.getInputStream(),"OUTPUT",fos);
            outHandler.start();
        }
        int exitVal = proc.waitFor();
        System.out.println("exit code: "+exitVal);
        if (fos != null){
            fos.flush();
            fos.close();
        }
    }
    
    /**
     * creates a String array of commands to pass to a process
     * @param commandType 1 = java audiveris, 2 = perl musicxml2midi
     * @return the array of commands
     */
    private static String[] createCommandString(int commandType){
        switch(commandType){
            case 1:
                String[] javaCommands = {"java","-jar",DIST_LOC+"audiveris.jar",
                    "-batch", 
                    "-script", FILE_LOC+"script_"+fileName+".xml"};
                return javaCommands;
            case 2:
                String[] perlCommands =  {"perl", 
                   DIST_LOC+"musicxml2mid.pl", 
                   FILE_LOC+fileName+".xml" };
                return perlCommands;
            default:
                return null;
        }
    }
    
    /**
     * closes our input and output streams and the socket connection
     * @throws IOException 
     */
    private static void cleanUp() throws IOException{
        objInStream.close();
        objOutStream.close();
        clientConn.close();
        System.out.println("cleaning up");
    }
}
    
   
