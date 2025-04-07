import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class PaymentServer {
    public static void main(String[] args) {
        int port = 5000;
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for client on port " + port);

            while(true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client on port " + port);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;

                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                socket.close();

            }

        } catch (IOException e) {
            throw new RuntimeException(e);

        }
    }
}
