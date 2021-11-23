package pl.zuz.sages.p1;
import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;

    private String server, username;
    private int port;

    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;


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
        } catch (IOException e) {
            display("Exception doing login : " + e);
            disconnect();
        }



    }
    private void display(String msg) {
        System.out.println(msg);
    }


    void sendMsgToServer(MessageType msg) {
        try {
            out.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if(in != null) in.close();
            if(out != null) out.close();
            if(socket != null) socket.close();
        }
        catch(Exception e) {}


    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
        String username;
        Scanner scan = new Scanner(System.in);


        System.out.println("Enter the username: ");
        username = scan.nextLine();

        Client client = new Client(serverAddress, portNumber, username);

        System.out.println("\nWelcome to the chatroom!");

        System.out.println("Simply type the message on the group chat or begin with @username to send private message " +
                "\nType 'USERS' to check who is online \nType 'LOGOUT' to logout ");


        while(true) {


            String msg = scan.nextLine();

            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMsgToServer(new MessageType(MessageType.logout, ""));
                break;
            }
            else if(msg.equalsIgnoreCase("USERS")) {
                client.sendMsgToServer(new MessageType(MessageType.users, ""));
            }
            else {
                client.sendMsgToServer(new MessageType(MessageType.message, msg));
            }
        }

        scan.close();
        client.disconnect();
    }
}
