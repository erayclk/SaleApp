import java.io.*;
import java.net.*;

public class TestClient {
    public static void main(String[] args) {
        try {
            System.out.println("Starting test client...");
            
            // Connect to the server
            Socket socket = new Socket("localhost", 12345);
            System.out.println("Connected to server");
            
            // Set up input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // Send a test credit card payment request
            String creditRequest = "{\"PaymentType\":\"Credit\",\"Amount\":\"100.00\"}";
            System.out.println("Sending credit card request: " + creditRequest);
            out.println(creditRequest);
            out.flush();
            
            // Read the response
            String response = in.readLine();
            System.out.println("Received response: " + response);
            
            // Close the connection
            socket.close();
            System.out.println("Test completed successfully");
            
        } catch (Exception e) {
            System.out.println("Test client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
