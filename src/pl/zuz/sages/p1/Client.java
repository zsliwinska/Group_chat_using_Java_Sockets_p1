package pl.zuz.sages.p1;

import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;


    private String server, username, path;
    private int port;


    Scanner scan = new Scanner(System.in);

    Client(String server, int port, String username, String path) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.path = path;

        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = "Connection accepted ";
        display(msg);

        try {
            in  = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new ListenFromServer().start();


        try {
            out.writeObject(username);
            out.writeObject(path);
        } catch (IOException e) {
            display("Exception doing login : " + e);
            disconnect();
        }



    }
    private void display(String msg) {
        System.out.println(msg);
    }


    private void sendMsgToServer(MessageType msg) {
        try {
            out.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    private void sendFile(){
        try{
            System.out.println("Enter receivers usernames: |Example: @username @username2");
            String users = scan.nextLine();
            System.out.println("Enter path of file to send: |Example: D:/folder/");
            String filePath = scan.nextLine();
            System.out.println("Enter file name: |Example: file.txt");
            String fileName = scan.nextLine();

            out.writeObject("FILE");
            out.writeObject(users);
            out.writeObject(filePath);
            out.writeObject(fileName);


        }catch(Exception e){}
    }

    void sendImg(){
        try{
            System.out.println("Enter receivers usernames: |Example: @username @username2");
            String users = scan.nextLine();
            System.out.println("Enter path of image U want to send: |Example: D:/folder/");
            String filePath = scan.nextLine();
            System.out.println("Enter image name: |Example: image.jpg");
            String fileName = scan.nextLine();
            System.out.println("Enter format name : |Example: jpg");
            String formatName = scan.nextLine();

            out.writeObject("IMG");
            out.writeObject(users);
            out.writeObject(filePath);
            out.writeObject(fileName);
            out.writeObject(formatName);
        }catch(Exception e){}

    }



    private void disconnect() {
        try {
            if(in != null) in.close();
            if(out != null) out.close();
            if(socket != null) socket.close();
        }
        catch(Exception e) {}


    }

    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {

                    String msg = (String) in.readObject();
                    System.out.println(msg);
                }
                catch(IOException e) {
                    display("~~~~ Server has closed the connection: " + e + " ~~~~");
                    break;
                }
                catch(ClassNotFoundException e2) {}
            }
        }
    }



    public static void main(String[] args) {
        int portNumber = 6000;
        String serverAddress = "localhost";
        String username,path;
        Scanner scan = new Scanner(System.in);


        System.out.println("Enter the username: ");
        username = scan.nextLine();

        System.out.println("Enter the path where to download files: |Example: D:/folder/");
        path = scan.nextLine();

        Client client = new Client(serverAddress, portNumber, username, path);

        System.out.println("\nWelcome to the chatroom!");

        System.out.println("Simply type the message on the group chat \nType @username to send private message " +
                "\nType 'USERS' to check who is online \nType 'LOGOUT' to logout \nType 'FILE' to send image or file");


        while(true) {


            String msg = scan.nextLine();

            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMsgToServer(new MessageType(MessageType.logout, ""));
                break;
            }
            else if(msg.equalsIgnoreCase("USERS")) {
                client.sendMsgToServer(new MessageType(MessageType.users, ""));
            }
            else if(msg.equalsIgnoreCase("FILE")) {
                client.sendMsgToServer(new MessageType(MessageType.file, ""));
                System.out.println("Type IMG to send an image or FILE to send something else");
                String choice = scan.nextLine();
                switch (choice.toUpperCase()){
                    case "IMG" :
                        client.sendImg();
                        break;
                    case "FILE":
                        client.sendFile();
                        break;
                    default:
                        System.out.println("Wrong input. Try to type again from start");
                        break;
                }


            }
            else {
                client.sendMsgToServer(new MessageType(MessageType.message, msg));
            }
        }

        scan.close();
        client.disconnect();
    }
}
