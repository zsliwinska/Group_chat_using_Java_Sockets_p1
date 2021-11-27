package pl.zuz.sages.p1;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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



    private synchronized boolean send(String message, String username){
        String time = date.format(new Date());
        boolean privateMsg = false;
        List<String> receivers = new ArrayList<>();

        String[] recipient = message.split(" ");

        if(recipient[1].charAt(0) == '@') privateMsg = true;




        if(privateMsg){
            String msg = "";
            for(int i=0;i< recipient.length;i++){
                if(recipient[i].charAt(0) == '@'){
                    receivers.add(recipient[i].substring(1));
                }else {
                    msg = time + " "+username+" : " + recipient[i] + "\n";
                }
            }


            boolean found = false;
            for(String checkUser: receivers){
                for(HandleClient user: clientsList){
                    if(checkUser.contains(user.getUsername())) {
                        if(!user.writeMsg(msg)) {
                            clientsList.remove(user);
                            display("Client " + user.username + " is disconnected  and removed from the chat.");
                        }

                        found=true;
                        break;
                    }
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
        send("~~~~ " + disconnectedClient + " has left the chat room. ~~~~", "");
    }



    class HandleClient extends Thread{

        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out, fileStr;
        int id;
        String username, path;
        MessageType msg;
        String date;


        HandleClient(Socket socket) {
            this.socket = socket;
            id = ++Uniqueid;

            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());

                username = (String) in.readObject();
                path = (String) in.readObject();
                send("~~~~ " + username + " has joined the chat room. ~~~~",username);

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
                        boolean confirmation =  send(username + ": " + message, username);
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

                    case MessageType.file:
//                        System.out.println("DONE");
//                        getAndSendFile();
                        String choice = "";
                        try {
                            choice = (String) in.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        if(choice.equals("FILE")){
                            SendFile();
                        }else{
                            SendImg();
                        }
                        break;

                }

            }

            remove(id);
            close();

        }

        private void SendImg(){

            try{
                String filePath = "", users = "", fileName ="",formatName ="";
                try {
                    users = (String) in.readObject();
                    filePath = (String) in.readObject();
                    fileName = (String) in.readObject();
                    formatName = (String) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                List<String> receivers = splitUsers(users);

                System.out.println("filePath + formantName "+filePath+fileName);
                String sourcePath = filePath+fileName;
                Path source = Paths.get(sourcePath);
                String path="";
                for(HandleClient client : clientsList) {
                    if (receivers.contains(client.username)) {
                        path = client.path+fileName;
                        Path target = Paths.get(path);

                        BufferedImage bi = ImageIO.read(source.toFile());

                        // convert BufferedImage to byte[]
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, formatName, baos);
                        byte[] bytes = baos.toByteArray();

                        // convert byte[] back to a BufferedImage
                        InputStream is = new ByteArrayInputStream(bytes);
                        BufferedImage newBi = ImageIO.read(is);
                        // save it
                        ImageIO.write(newBi, formatName, target.toFile());

                        client.writeMsg("Received image from "+username);
                    }
                }

            }catch (IOException e){
                e.printStackTrace();
            }



        }

        private List<String> splitUsers(String users){
            List<String> receivers = new ArrayList<>();
            String[] recipient = users.split(" ");

            for(int i=0;i< recipient.length;i++){
                if(recipient[i].charAt(0) == '@'){
                    receivers.add(recipient[i].substring(1));
                }
            }
            return receivers;
        }

        private void SendFile(){

            try {
                String filePath = "", users = "", fileName="";
                try {
                    users = (String) in.readObject();
                    filePath = (String) in.readObject();
                    fileName = (String) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                List<String> receivers = splitUsers(users);
                String source = filePath+fileName;
                FileInputStream fileStream = new FileInputStream(source);
//                byte[] b = fileStream.readAllBytes() ;
//                FileMessage f = new FileMessage(b);

//                byte[] buffer = new byte[fileStream.available()];
//                fileStream.read(buffer);



                byte[] content = Files.readAllBytes(Paths.get(source));
                String path = "";
                for(HandleClient client : clientsList) {
                    if (receivers.contains(client.username)) {
                        path = client.path+fileName;
                        FileOutputStream fr = new FileOutputStream(path);
                        fileStr = new ObjectOutputStream(fr);
                        fileStr.writeObject(content);
                        client.writeMsg("Received file from "+username);
                    }
                }

                display("File send succesfully");



            } catch (IOException e) {
                e.printStackTrace();
            }
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

            if(!socket.isConnected()) {
                close();
                return false;
            }

            try {
                out.writeObject(msg);
            } catch(IOException e) {
                display("~~~~ Error sending message to " + username + " ~~~~");
                display(e.toString());
            }
            return true;
        }
    }

    public static void main(String[] args) {
        int portNumber = 6000;
        new Server(portNumber);

    }
}
