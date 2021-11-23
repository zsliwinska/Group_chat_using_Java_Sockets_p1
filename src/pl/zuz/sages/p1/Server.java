package pl.zuz.sages.p1;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {

    private static int Uniqueid;
    private int port;
    private ArrayList<HandleClient> clientsList = new ArrayList<>();
    private SimpleDateFormat date = new SimpleDateFormat("HH:mm");
    private boolean active;
    Socket socket;


    public Server(int port){
        this.port = port;

        active = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (active){
                display("Waiting for clients: ");

                socket = serverSocket.accept();

                if(!active) break;
                HandleClient client = new HandleClient(socket);

                clientsList.add(client);

                client.start();
            }
            try{
                serverSocket.close();
                for (HandleClient cl : clientsList) {
                    try {
                        cl.in.close();
                        cl.out.close();
                        cl.socket.close();
                    } catch (IOException ioE) {}

                }
            }catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }




        } catch (IOException e) {
            display(date.format(new Date()) + " Exception on new ServerSocket: " + e + "\n");

        }



    }

    private void display(String msg) {
        String time = date.format(new Date()) + " " + msg;
        System.out.println(time);
    }



    private synchronized boolean send(String message){
        String time = date.format(new Date());
        boolean privateMsg = false;
        //if private
        String[] recipient = message.split(" ",3);

        if(recipient[1].charAt(0) == '@') privateMsg = true;

        if(privateMsg){

            String checkUser = recipient[1].substring(1);

            String msg = time + " " + recipient[1] + "\n";

            boolean found = false;
            for(HandleClient user: clientsList){
               if(checkUser.equals(user.getUsername())) {
                    if(!user.writeMsg(msg)) {
                        clientsList.remove(user);
                        display("Client " + user.username + " is disconnected  and removed from the chat.");
                    }
                    // username found and delivered the message
                    found=true;
                    break;
                }
            }

            if(!found)
            {
                return false;
            }


        }else{
            String msg = time + " " + message + "\n";

            System.out.print(msg);

            for(int i = clientsList.size(); --i >= 0;) {
                HandleClient ct = clientsList.get(i);
                // try to write to the Client if it fails remove it from the list
                if(!ct.writeMsg(msg)) {
                    clientsList.remove(i);
                    display("Client " + ct.username + " is disconnected and removed from the chat.");
                }
            }

        }
        return true;

    }

    synchronized void remove(int id) {

        String disconnectedClient = "";

        for(int i = 0; i < clientsList.size(); ++i) {
            HandleClient ct = clientsList.get(i);

            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                clientsList.remove(i);
                break;
            }
        }
        send("~~~~ " + disconnectedClient + " has left the chat room. ~~~~" );
    }



    class HandleClient extends Thread{

        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;
        int id;
        String username;
        MessageType msg;
        String date;

        HandleClient(Socket socket) {
            this.socket = socket;
            id = ++Uniqueid;

            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());

                username = (String) in.readObject();
                send("~~~~ " + username + " has joined the chat room. ~~~~");

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            date = new Date() + "\n";



        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }


        public void run() {
            boolean active = true;

            while(active){
                try {
                    msg = (MessageType) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                String message = msg.getMessage_context();

                switch(msg.getMessage_type()) {

                    case MessageType.message:
                        boolean confirmation =  send(username + ": " + message);
                        if(!confirmation){
                            String msg = "~~~~ User no exists. ~~~~";
                            writeMsg(msg);
                        }
                        break;
                    case MessageType.logout:
                        display(username.toUpperCase() + " LEFT ");
                        active = false;
                        break;
                    case MessageType.users:
                        writeMsg("List of the users connected at " + date.format(new Date().toString()) + "\n");

                        for(int i = 0; i < clientsList.size(); ++i) {
                            HandleClient ct = clientsList.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }

            }

            remove(id);
            close();

        }

        private void close() {
            try {
                if(out != null) out.close();
                if(in != null) in.close();
                if(socket != null) socket.close();
            }
            catch(Exception e) {}

        }

        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                out.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("~~~~ Error sending message to " + username + " ~~~~");
                display(e.toString());
            }
            return true;
        }
    }

    public static void main(String[] args) {
        int portNumber = 6000;

        // create a server object and start it
        Server server = new Server(portNumber);

    }
}
