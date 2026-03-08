import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatApp extends Application {

    private TextArea chatArea;
    private TextField messageField;
    private TextField hostField;
    private TextField portField;
    private TextField nameField;
    private Button connectButton;
    private Button sendButton;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Thread receiveThread;

    private boolean connected = false;

    @Override
    public void start(Stage primaryStage) {
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        messageField.setDisable(true);

        hostField = new TextField("127.0.0.1");
        portField = new TextField("59001");
        nameField = new TextField();
        nameField.setPromptText("Enter your name");

        connectButton = new Button("Connect");
        sendButton = new Button("Send");
        sendButton.setDisable(true);

        GridPane topPane = new GridPane();
        topPane.setHgap(10);
        topPane.setVgap(10);
        topPane.setPadding(new Insets(10));

        topPane.add(new Label("Host:"), 0, 0);
        topPane.add(hostField, 1, 0);

        topPane.add(new Label("Port:"), 2, 0);
        topPane.add(portField, 3, 0);

        topPane.add(new Label("Name:"), 4, 0);
        topPane.add(nameField, 5, 0);

        topPane.add(connectButton, 6, 0);

        HBox bottomPane = new HBox(10);
        bottomPane.setPadding(new Insets(10));
        bottomPane.getChildren().addAll(messageField, sendButton);

        BorderPane root = new BorderPane();
        root.setTop(topPane);
        root.setCenter(chatArea);
        root.setBottom(bottomPane);

        connectButton.setOnAction(e -> connectToServer());
        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());

        Scene scene = new Scene(root, 750, 500);

        primaryStage.setTitle("Lehman Multi-Platform Chat System");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> closeConnection());
    }

    private void connectToServer() {
        if (connected) {
            appendMessage("Already connected.");
            return;
        }

        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        String username = nameField.getText().trim();

        if (host.isEmpty() || portText.isEmpty() || username.isEmpty()) {
            appendMessage("Please enter host, port, and name.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            appendMessage("Port must be a number.");
            return;
        }

        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connected = true;
            appendMessage("Connected to server at " + host + ":" + port);

            messageField.setDisable(false);
            sendButton.setDisable(false);
            connectButton.setDisable(true);
            hostField.setDisable(true);
            portField.setDisable(true);
            nameField.setDisable(true);

            startReceiverThread(username);

        } catch (IOException e) {
            appendMessage("Connection failed: " + e.getMessage());
        }
    }

    private void startReceiverThread(String username) {
        receiveThread = new Thread(() -> {
            try {
                String line;
                boolean nameSent = false;

                while ((line = reader.readLine()) != null) {

                    if (!nameSent && line.contains("Enter your username")) {
                        writer.println(username);
                        nameSent = true;
                    }

                    String finalLine = line;
                    Platform.runLater(() -> chatArea.appendText(finalLine + "\n"));
                }

            } catch (IOException e) {
                Platform.runLater(() -> appendMessage("Disconnected from server."));
            } finally {
                Platform.runLater(() -> {
                    connected = false;
                    messageField.setDisable(true);
                    sendButton.setDisable(true);
                    connectButton.setDisable(false);
                });
            }
        });

        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void sendMessage() {
        if (!connected || writer == null) {
            appendMessage("You are not connected.");
            return;
        }

        String message = messageField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        if (message.equalsIgnoreCase("QUIT")) {
            closeConnection();
            return;
        }

        writer.println(message);
        messageField.clear();
    }

    private void appendMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    private void closeConnection() {
        try {
            if (writer != null) {
                writer.println("QUIT");
            }
        } catch (Exception ignored) {
        }

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
    }

    public static void main(String[] args) {
        launch(args);
    }
}