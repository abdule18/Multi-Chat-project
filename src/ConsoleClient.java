import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 59001;

    public static void main(String[] args) {
        System.out.printf("Connecting to chat server at %s:%d...%n", SERVER_ADDRESS, PORT);

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        Scanner scanner = null;

        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            scanner = new Scanner(System.in);

            System.out.println("Connected to the chat server.");

            PrintWriter finalWriter = writer;
            BufferedReader finalReader = reader;

            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = finalReader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            receiveThread.setDaemon(true);
            receiveThread.start();

            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("QUIT")) {
                    System.out.println("Closing connection...");
                    break;
                }

                finalWriter.println(message);
            }

        } catch (IOException e) {
            System.out.println("Error connecting to the server: " + e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                System.out.println("Error closing reader: " + e.getMessage());
            }

            if (writer != null) writer.close();

            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }

            if (scanner != null) scanner.close();
        }
    }
}