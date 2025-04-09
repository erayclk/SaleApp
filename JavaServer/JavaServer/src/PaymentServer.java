import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PaymentServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("PaymentServer started. Waiting for connections on port 5000...");

        while (true) {
            System.out.println("Waiting for new connection...");
            Socket clientSocket = null;

            try {
                clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                // Sunucu tarafında timeout ayarı
                clientSocket.setSoTimeout(20000); // 20 saniye

                // Veri okuma için InputStream kullanımı
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();

                // Veri okuma işlemi - Buffer kullanarak
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);

                if (bytesRead > 0) {
                    // Alınan veriyi String'e çevir
                    String jsonRequest = new String(buffer, 0, bytesRead);
                    System.out.println("Raw data received: " + jsonRequest);

                    // Yanıt oluştur
                    String jsonResponse;
                    
                    System.out.println("Analyzing payment type in request: " + jsonRequest);
                    
                    if (jsonRequest.contains("\"PaymentType\":\"Credit\"")) {
                        System.out.println("Detected Credit payment");
                        jsonResponse = "{\"ResponseCode\":\"02\"}\n";
                    } else if (jsonRequest.contains("\"PaymentType\":\"QR\"")) {
                        System.out.println("Detected QR payment");
                        jsonResponse = "{\"ResponseCode\":\"03\"}\n";
                    } else if (jsonRequest.contains("\"PaymentType\":\"Cash\"")) {
                        System.out.println("Detected Cash payment - Responding with code 01");
                        jsonResponse = "{\"ResponseCode\":\"01\"}\n";
                    } else {
                        System.out.println("Unknown payment type - Responding with code 99");
                        jsonResponse = "{\"ResponseCode\":\"99\"}\n";
                    }

                    System.out.println("Sending response: " + jsonResponse.trim());

                    // Yanıtı gönder
                    outputStream.write(jsonResponse.getBytes());
                    outputStream.flush();

                    // Yanıtın gönderilmesini sağlamak için kısa bir gecikme
                    Thread.sleep(200);
                } else {
                    System.out.println("No data received from client");
                }

            } catch (Exception e) {
                System.out.println("Error in server: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Bağlantıyı kapat
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                        System.out.println("Connection closed");
                    } catch (IOException e) {
                        System.out.println("Error closing socket: " + e.getMessage());
                    }
                }
            }
        }
    }
}