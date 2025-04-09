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
                    
                    System.out.println("Received payment details:");
                    if (jsonRequest.contains("\"ProductId\"")) {
                        System.out.println("  - Product ID: " + extractJsonValue(jsonRequest, "ProductId"));
                    }
                    if (jsonRequest.contains("\"ProductName\"")) {
                        System.out.println("  - Product Name: " + extractJsonValue(jsonRequest, "ProductName"));
                    }
                    if (jsonRequest.contains("\"Amount\"")) {
                        System.out.println("  - Amount: " + extractJsonValue(jsonRequest, "Amount"));
                    }
                    if (jsonRequest.contains("\"VatRate\"")) {
                        System.out.println("  - VAT Rate: " + extractJsonValue(jsonRequest, "VatRate"));
                    }

                    // Yanıt oluştur
                    String jsonResponse;
                    
                    System.out.println("Analyzing payment type in request: " + jsonRequest);
                    
                    // Extract the payment type directly using our method
                    String paymentType = extractJsonValue(jsonRequest, "PaymentType");
                    System.out.println("Extracted PaymentType: " + paymentType);
                    
                    if (paymentType.equals("Credit")) {
                        System.out.println("Detected Credit payment - Responding with code 02");
                        jsonResponse = "{\"ResponseCode\":\"02\"}\n";
                    } else if (paymentType.equals("QR")) {
                        System.out.println("Detected QR payment");
                        // Get the ProductId to determine which response to send
                        String productId = extractJsonValue(jsonRequest, "ProductId");
                        if (productId.equals("1")) {
                            System.out.println("QR payment with ProductId 1 - Responding with code 01");
                            jsonResponse = "{\"ResponseCode\":\"01\"}\n";
                        } else {
                            System.out.println("QR payment with other ProductId - Responding with code 03");
                            jsonResponse = "{\"ResponseCode\":\"03\"}\n";
                        }
                    } else if (paymentType.equals("Cash")) {
                        System.out.println("Detected Cash payment - Responding with code 01");
                        jsonResponse = "{\"ResponseCode\":\"01\"}\n";
                    } else {
                        System.out.println("Unknown payment type: '" + paymentType + "' - Responding with code 99");
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

    // Basit bir JSON değer çıkarma metodu
    private static String extractJsonValue(String json, String key) {
        // "key": value veya "key":"value" şeklindeki desenleri arar
        String pattern = "\"" + key + "\"\\s*:\\s*([^,}\\s]+|\"[^\"]*\")";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        
        if (m.find()) {
            String value = m.group(1);
            // Eğer değer tırnak içindeyse tırnakları kaldır
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        return "Not found";
    }
}