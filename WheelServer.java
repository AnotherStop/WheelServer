/*
 * Wheel HTTP Server
 * Implementing HTTP protocol version 1.0
 *
 * --Yet another toy HTTP server
 * --Learning by reinventing the wheel
 *
 * Running: $ javac WheelServer.java
 *          $ java WheelServer [port]
 *
 * Author: Bing Lu
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class WheelServer{

    public static void main(String[] args){         

        //port that will be listened
        int port = 80;  //defaul port
        //user specified port
        if(args.length == 1){
            int portArgs = Integer.parseInt(args[0]);
            if(portArgs == 80 || (portArgs > 1024 && portArgs < 65535))
                port = portArgs;
            else{
                System.out.println("You specified port "+portArgs+" is invalid.");
                System.out.println("Valid port range is between 1024 to 65535, exclusive");
                //return false;
            }

        } 

        System.out.println("\n===========Wheel HTTP Server============");
        System.out.println("    It's really reinvent-the-wheel. ");
        System.out.println("=========================================\n");

        System.out.println("Trying to bind on port " + Integer.toString(port) + "...");
        
        ServerSocket serversocket = null;
        //create server socket object on specified port
        try{
            serversocket = new ServerSocket(port);
        }
        catch(Exception e) { 
            System.out.println("Error:" + e.getMessage());
            //return false;
        }

        System.out.println("Ready, Waiting for requests coming...");

        //infinite loop for listening port
        while(true) {
            
            try {
                //accepting a connection from a client
                Socket connectionSocket = serversocket.accept();

                //create a new thread to handle this connection
                ConnectionThread connection = new ConnectionThread(connectionSocket);

            }
            catch(Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }

        } 
    }

}

class ConnectionThread extends Thread {

    Socket connectionSocket = null;

    public ConnectionThread(Socket connectionSocket){
        this.connectionSocket = connectionSocket;
        start();
    }

    public void run(){
        try{
            InetAddress client = connectionSocket.getInetAddress();
            
            //print client's name and IP address
            System.out.println("\n"+client.getHostName() + " connected to the server.");
            System.out.println("--whose ip Address is "+client.getHostAddress());

            System.out.println("Processing request...");
            long tid = Thread.currentThread().getId();
            System.out.println("Current Working Thread's id is " + tid);

            //buffered reader for reading client's request
            BufferedReader input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            //buffered writer for returning the server's response
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connectionSocket.getOutputStream()));

            //parse the request under HTTP 1.0 protocol
            parseHttpRequest(input, output);

            connectionSocket.close();
        } 
        catch(Exception ex){
            System.out.println("Error: " + ex.getMessage());
        }       
    }

    /*
     * Implementation of HTTP 1.0 protocol
     * Arguments: input, the received http request; output, the returned response
     */
    private void parseHttpRequest(BufferedReader input, BufferedWriter output) {
        
        //code for current supported method
        // 0 for unimplement method
        // 1 GET method
        // 2 HEAD method
        int supportedMethod = 0;

        //store the initial line of http request
        String initialLine = null;

        //path of the requested file
        String path = null; 
        
        //read the initial line
        try{
            initialLine = input.readLine();
        }catch(Exception ex){
            System.out.println("Error: " + ex.getMessage());
        }

        //split the initial line to get the three parts of it
        String[] initialLineParts = initialLine.split("\\s+");
        boolean badRequest = false;
        boolean notImplemented = false;

        //if there's not exactly 3 parts, must be a bad request
        if(initialLineParts.length != 3){
            badRequest = true;
        }

        //if it isn't a bad request so far, searching the method's name
        if(badRequest != true){

            //store the method that haven't been implemented by this server
            ArrayList<String> notImplementedMethods = new ArrayList<String>();
            notImplementedMethods.add("POST");
            notImplementedMethods.add("PUT");
            notImplementedMethods.add("DELETE");
            notImplementedMethods.add("OPTIONS");
            notImplementedMethods.add("TRACE");

            //find method
            if (initialLineParts[0].equalsIgnoreCase("GET")) { 
                supportedMethod = 1;
            }      
            else if (initialLineParts[0].equalsIgnoreCase("HEAD")) { 
                supportedMethod = 2;
            }
            else if(notImplementedMethods.contains(initialLineParts[0]) == true){
                notImplemented = true;
            }
            else{
                badRequest = true;
            }
        }

        //cope with the condition that bad request or method not implement
        if(badRequest == true || notImplemented == true){
            try{
                if(badRequest == true)
                    output.write(buildHttpResponseHeader(400, 0));
                
                else if(notImplemented == true)
                    output.write(buildHttpResponseHeader(501, 0));
              
                output.close();
                return;
            }
            catch (Exception ex){
                System.out.println("Error: " + ex.getMessage());
            }
        }

        //path of the requested resource
        path = initialLineParts[1].trim().substring(1);
        System.out.println("Client requested: " + new File(path).getAbsolutePath());
        
        if(path.startsWith("../") == true){
            try{
                output.write(buildHttpResponseHeader(403, 0));
                output.close();
                return;
            }
            catch (Exception ex){
                System.out.println("Error: " + ex.getMessage());
            }
        }

        //reader for reading the requested file
        BufferedReader requestedFile = null;
        try {
            requestedFile = new BufferedReader(new FileReader(path));
        }
        catch (Exception ex) {
            
            try {
              output.write(buildHttpResponseHeader(404, 0));
              output.close();
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println("Error: " + ex.getMessage());
        }

        //code for content type
        int contentType = 0;
        try {

            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = 1;
            }
            else if (path.endsWith(".gif")) {
                contentType = 2;      
            }
            else if (path.endsWith(".ico")) {
                contentType = 3;
            }
            else if (path.endsWith(".zip")) {
                contentType = 4;
            }
            else{
                contentType = 5;
            }

            //so far, so good. It's a 200 OK response
            output.write(buildHttpResponseHeader(200, contentType));

            //provide message body, only if it's GET method
            if (supportedMethod == 1) {
                //read from the requested file and write to buffered writer
                String line = null;
                while ((line = requestedFile.readLine()) != null) {
                    output.write(line);
                    output.newLine();
                }
            }

            output.flush();   //flush buffer
            
            //close to release resource
            requestedFile.close();
    
        }
        catch (Exception ex) {
            try{
                output.write(buildHttpResponseHeader(500, 0));
                output.close();
                return;
            }
            catch (Exception e){
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println("Error: " + ex.getMessage());
        }

    }

    /*
     * Build Http Response Header
     * Arguments: statusCode, http response status code; contentType, content type
     */
    private String buildHttpResponseHeader(int statusCode, int contentType) {
        
        final String httpProtocolVersion = "HTTP/1.0 ";
        //http status code
        final String succeed = "200 OK";
        final String badRequest = "400 Bad Request";
        final String forbidden = "403 Forbidden";
        final String notFound = "404 Not Found";
        final String interalError = "500 Internal Server Error";
        final String notImplemented = "501 Not Implemented";

        String responseStatusLine = new String();
        responseStatusLine += httpProtocolVersion;
        
        switch (statusCode) {
          case 200:
              responseStatusLine += succeed;
              break;
          case 400:
              responseStatusLine += badRequest;
              break;
          case 403:
              responseStatusLine += forbidden;
              break;
          case 404:
              responseStatusLine += notFound;
              break;
          case 500:
              responseStatusLine += interalError;
              break;
          case 501:
              responseStatusLine += notImplemented;
              break;
        }
        responseStatusLine += "\r\n";  //line end with CRLF

        String responseConnectionType = "Connection: close\r\n"; 
        String responseServerName = "Server: Wheel HTTP Server\r\n"; 
        
        String content = null;
        switch (contentType) {
            case 0:
                break;
            case 1:
                content = "Content-Type: image/jpeg\r\n";
                break;
            case 2:
                content = "Content-Type: image/gif\r\n";
              break;
            case 3:
                content = "Content-Type: image/icon\r\n";
                break;
            case 4:
                content = "Content-Type: application/zip-compressed\r\n";
                break;
            case 5:
            default:
                content = "Content-Type: text/html\r\n";
                break;
        }

        //merge the above line to composite a response header
        String responseHeader = responseStatusLine + responseConnectionType + responseServerName + content; 
        responseHeader += "\r\n";

        return responseHeader;
    }

}
