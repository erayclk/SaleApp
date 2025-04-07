import java.io.*;
import java.net.*;

public class PaymentServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("PaymentServer started. Waiting for connections...");

        while (true) {
            System.out.println("Waiting for new connection...");
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read JSON request from the client
                String jsonRequest = in.readLine();
                System.out.println("Raw data received: " + jsonRequest);

                // Initialize the response
                String jsonResponse;

                // Determine the response based on the payment type
                if (jsonRequest.contains("\"PaymentType\":\"Credit\"")) {
                    jsonResponse = "{\"ResponseCode\":\"00\"}"; // Credit payment approval
                } else if (jsonRequest.contains("\"PaymentType\":\"QRCode\"")) {
                    jsonResponse = "{\"ResponseCode\":\"00\"}"; // QR Code payment approval
                } else {
                    jsonResponse = "{\"ResponseCode\":\"99\"}"; // Invalid request
                }

                // Send the response back to the client
                out.println(jsonResponse);
                out.flush();
                System.out.println("Sent response: " + jsonResponse);
                System.out.println("Received JSON: " + jsonRequest);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}