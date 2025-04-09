import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class PaymentServer {
    private static JFrame frame;
    private static JLabel statusLabel;
    private static JLabel imageLabel;
    private static JTextArea logArea;
    private static String currentAmount = "";
    
    public static void main(String[] args) throws IOException {
        // Set up the GUI
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        
        ServerSocket serverSocket = new ServerSocket(5000);
        log("PaymentServer started. Waiting for connections on port 5000...");

        while (true) {
            log("Waiting for new connection...");
            Socket clientSocket = null;

            try {
                clientSocket = serverSocket.accept();
                log("Client connected from: " + clientSocket.getInetAddress());

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
                    log("Raw data received: " + jsonRequest);
                    
                    log("Received payment details:");
                    if (jsonRequest.contains("\"ProductId\"")) {
                        log("  - Product ID: " + extractJsonValue(jsonRequest, "ProductId"));
                    }
                    if (jsonRequest.contains("\"ProductName\"")) {
                        log("  - Product Name: " + extractJsonValue(jsonRequest, "ProductName"));
                    }
                    if (jsonRequest.contains("\"Amount\"")) {
                        log("  - Amount: " + extractJsonValue(jsonRequest, "Amount"));
                    }
                    if (jsonRequest.contains("\"VatRate\"")) {
                        log("  - VAT Rate: " + extractJsonValue(jsonRequest, "VatRate"));
                    }

                    // Yanıt oluştur
                    String responseCode;
                    
                    log("Analyzing payment type in request: " + jsonRequest);
                    
                    // Extract the payment type directly using our method
                    String paymentType = extractJsonValue(jsonRequest, "PaymentType");
                    log("Extracted PaymentType: " + paymentType);
                    
                    String amount = extractJsonValue(jsonRequest, "Amount");
                    
                    if (paymentType.equals("Credit")) {
                        log("Detected Credit payment - Responding with code 02");
                        updateStatus("Processing Credit Card Payment: " + amount + " TL");
                        
                        // Kredi kartı resmini tutarla birlikte göster
                        currentAmount = amount;
                        showCardImage();
                        
                        responseCode = "02";
                    } else if (paymentType.equals("QR")) {
                        log("Detected QR payment");
                        updateStatus("Processing QR Payment: " + amount + " TL");
                        
                        // Hangi yanıtın gönderileceğini belirlemek için Ürün Kimliğini alın
                        String productId = extractJsonValue(jsonRequest, "ProductId");
                        if (productId.equals("1")) {

                            responseCode = "01";
                        } else {

                            responseCode = "03";
                        }
                    } else if (paymentType.equals("Cash")) {
                        log("Detected Cash payment - Responding with code 01");
                        updateStatus("Processing Cash Payment: " + amount + " TL");
                        
                        responseCode = "01";
                    } else {
                        log("Unknown payment type: '" + paymentType + "' - Responding with code 99");
                        updateStatus("Unknown Payment Type: " + paymentType);
                        
                        responseCode = "99";
                    }

                    // İstekteki tüm verileri çıkarın
                    String productId = extractJsonValue(jsonRequest, "ProductId");
                    String productName = extractJsonValue(jsonRequest, "ProductName");
                    String vatRate = extractJsonValue(jsonRequest, "VatRate");

                    // Tüm verilerle tam bir yanıt oluşturun
                    String jsonResponse = String.format(
                        "{\"ResponseCode\":\"%s\",\"ProductId\":%s,\"ProductName\":\"%s\",\"PaymentType\":\"%s\",\"Amount\":\"%s\",\"VatRate\":%s}\n",
                        responseCode, productId, productName, paymentType, amount, vatRate
                    );


                    
                    if (paymentType.equals("Credit")) {
                        updateStatus("Credit Card Payment Successful!");
                    } else if (paymentType.equals("QR")) {
                        updateStatus("QR Payment Successful!");
                    } else if (paymentType.equals("Cash")) {
                        updateStatus("Cash Payment Successful!");
                    }

                    // Yanıtı gönder
                    outputStream.write(jsonResponse.getBytes());
                    outputStream.flush();

                    // Yanıtın gönderilmesini sağlamak için kısa bir gecikme
                    Thread.sleep(200);
                } else {
                    log("No data received from client");
                }

            } catch (Exception e) {
                log("Error in server: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Bağlantıyı kapat
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                        log("Connection closed");
                    } catch (IOException e) {
                        log("Error closing socket: " + e.getMessage());
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
    
    private static void createAndShowGUI() {
        // Pencereyi oluştur ve ayarla
        frame = new JFrame("Payment Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());
        
        // Üst taraftaki durum paneli
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel = new JLabel("Starting server...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusPanel.add(statusLabel);
        frame.add(statusPanel, BorderLayout.NORTH);
        
        // Kart görseli için orta panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        imageLabel = new JLabel("Waiting for payment...");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        centerPanel.add(imageLabel, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);
        
        // Alttaki log paneli
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Log"));
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(scrollPane);
        frame.add(logPanel, BorderLayout.SOUTH);
        
        // Pencereyi görüntüle
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Kaynakları kapat
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Server shutting down...");
                System.exit(0);
            }
        });
    }
    
    private static void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(status);
            }
        });
    }
    
    private static void log(String message) {
        System.out.println(message);
        
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
    
    private static void showCardImage() {
        SwingUtilities.invokeLater(() -> {
            if (imageLabel != null) {
                try {
                    // Belirli dosya yolunu kullan
                    File cardFile = new File("C:/projeler/SaleApp/JavaServer/JavaServer/out/production/JavaServer/res/drawable/card.png");
                    
                    if (!cardFile.exists()) {
                        log("Card image not found at: " + cardFile.getAbsolutePath());


                        
                        imageLabel.setText("Card image not found");
                        return;
                    }

                    
                    // card.png resmini yükleyin
                    BufferedImage cardImage = ImageIO.read(cardFile);
                    
                    // Üzerine çizim yapmak için bir kopya oluşturun
                    BufferedImage copyImage = new BufferedImage(
                            cardImage.getWidth(), 
                            cardImage.getHeight(), 
                            BufferedImage.TYPE_INT_ARGB);
                    
                    Graphics2D g2d = copyImage.createGraphics();
                    
                    // Orijinal resmi çiz
                    g2d.drawImage(cardImage, 0, 0, null);
                    
                    // Tutarı üstüne çiz
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 24));
                    
                    // Sağ ortadaki pozisyondaki çekme miktarı
                    String amountText = currentAmount + " TL";
                    int textWidth = g2d.getFontMetrics().stringWidth(amountText);
                    
                    // Metni kartın sağ orta kısmına yerleştirin
                    int xPosition = cardImage.getWidth() - textWidth - 150; // 150 pixels from the right edge (moved more to the left)
                    int yPosition = cardImage.getHeight() / 2; // vertically centered
                    
                    g2d.drawString(amountText, xPosition, yPosition);
                    
                    g2d.dispose();
                    
                    // Kaynakları silmek için kapatmayı ekleyin
                    imageLabel.setIcon(new ImageIcon(copyImage));
                    imageLabel.setText("");
                    frame.repaint();
                    
                } catch (IOException e) {
                    log("Error loading card image: " + e.getMessage());
                    e.printStackTrace();
                    imageLabel.setText("Error loading card image");
                }
            }
        });
    }
}