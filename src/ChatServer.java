import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {

    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());

    public static final int PORT = 59001;

    private static class  ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Handling client communication in thread: " + Thread.currentThread().getName() + " for client: " + socket.getRemoteSocketAddress());

            BufferedReader reader = null;
            PrintWriter writer = null;
            String username = null;

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                clientWriters.add(writer); // Add the client's writer to the set of client writers for broadcasting messages

                // Ask the client for a username
                writer.println("SERVER: Enter your username: ");
                username = reader.readLine();
                System.out.println("Client " + socket.getRemoteSocketAddress() + " set username to: " + username);

                // broadcast to all clients that a new user joined the chat
                broadcastMessage("Client " + username + " has joined the chat!");

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received message from " + username + ": " + message);
                    broadcastMessage(username + ":  " + message);
                }
            } catch (IOException e) {
                System.out.println("Error handling client communication: " + e.getMessage());
            } finally {
                try {
                    if (writer != null) {
                        clientWriters.remove(writer);   // REMOVE CLIENT FROM LIST
                    }
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                    if (socket != null) socket.close();
                    if (username != null) {
                        broadcastMessage("SERVER: " + username + " has left the chat!");
                    }
                } catch (IOException e) {
                    System.out.println("Error closing client resources: " + e.getMessage());
                }
            }
        }

        private static void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("The chat server is running. Awaiting connections");

        try(ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                //Accept a  new  client connection
                // We are listening/awaiting for clients in the main thread, so this will block until a client connects
                var socket =  listener.accept(); // blocks until a client connects
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());

                // how do we handle client communications
                ClientHandler handler = new ClientHandler(socket); // create a new thread to handle client communication
                handler.start(); // start the thread to handle client communication
            }
        } catch (IOException e) {
            System.out.println("Error stating the server: " + e.getMessage());
        }
    }
}
