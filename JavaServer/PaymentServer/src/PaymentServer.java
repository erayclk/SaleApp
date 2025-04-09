import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PaymentServer {
    private static JFrame frame;
    private static JLabel statusLabel;
    private static JLabel imageLabel;
    private static JTextArea logArea;
    
    public static void main(String[] args) {
        // Set up the GUI
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        
        int port = 5000;
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            logMessage("Payment Server started on port " + port);
            logMessage("Waiting for connections...");

            while(true) {
                Socket clientSocket = serverSocket.accept();
                logMessage("New client connected: " + clientSocket.getInetAddress());

                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                
                try {
                    String requestLine = reader.readLine();
                    if (requestLine != null) {
                        logMessage("Received request: " + requestLine);
                        
                        // Process payment request
                        JSONObject response = processPaymentRequest(requestLine);
                        
                        // Send response back to client
                        writer.write(response.toString());
                        writer.newLine();
                        writer.flush();
                        logMessage("Sent response: " + response.toString());
                    }
                } catch (Exception e) {
                    logMessage("Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Send error response
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", e.getMessage());
                    writer.write(errorResponse.toString());
                    writer.newLine();
                    writer.flush();
                } finally {
                    clientSocket.close();
                    logMessage("Client disconnected");
                    
                    // Reset UI after some time
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            SwingUtilities.invokeLater(() -> {
                                updateStatus("Waiting for connection...");
                                hideCardImage();
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        } catch (IOException e) {
            logMessage("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static JSONObject processPaymentRequest(String requestJson) throws Exception {
        JSONObject request = new JSONObject(requestJson);
        String paymentType = request.getString("PAYMENT_TYPE");
        String amount = request.getString("AMOUNT");
        
        JSONObject response = new JSONObject();
        response.put("timestamp", new Date().getTime());
        
        switch (paymentType) {
            case "Credit":
                // Process credit card payment
                logMessage("Processing credit card payment of " + amount + " TL");
                updateStatus("Processing Credit Card Payment: " + amount + " TL");
                
                // Show credit card image
                showCreditCardImage();
                
                // Simulate credit card processing
                Thread.sleep(2000); // Simulate processing time
                response.put("status", "success");
                response.put("responseCode", "2");
                response.put("message", "Credit card payment successful");
                response.put("transactionId", "CC-" + System.currentTimeMillis());
                
                updateStatus("Credit Card Payment Successful!");
                break;
                
            case "QR":
            case "QRCode":
                // Process QR payment
                logMessage("Processing QR payment of " + amount + " TL");
                updateStatus("Processing QR Payment: " + amount + " TL");
                hideCardImage();
                
                // Simulate QR processing
                Thread.sleep(1500); // Simulate processing time
                response.put("status", "success");
                response.put("responseCode", "3");
                response.put("message", "QR payment successful");
                response.put("transactionId", "QR-" + System.currentTimeMillis());
                response.put("qrContent", request.optString("QR_CONTENT", ""));
                
                updateStatus("QR Payment Successful!");
                break;
                
            default:
                logMessage("Unsupported payment type: " + paymentType);
                updateStatus("Error: Unsupported payment type");
                hideCardImage();
                
                response.put("status", "error");
                response.put("responseCode", "99");
                response.put("message", "Unsupported payment type: " + paymentType);
        }
        
        // Save payment to database (simulated)
        logMessage("Payment saved: Type=" + paymentType + ", Amount=" + amount);
        
        return response;
    }
    
    private static void createAndShowGUI() {
        // Create and set up the window
        frame = new JFrame("Payment Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());
        
        // Status panel at top
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel = new JLabel("Starting server...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusPanel.add(statusLabel);
        frame.add(statusPanel, BorderLayout.NORTH);
        
        // Center panel for card image
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        centerPanel.add(imageLabel, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);
        
        // Log panel at bottom
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Log"));
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        logPanel.add(scrollPane);
        frame.add(logPanel, BorderLayout.SOUTH);
        
        // Display the window
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Add shutdown hook to close resources
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
    
    private static void logMessage(String message) {
        System.out.println(message);
        
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(message + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
    
    private static void showCreditCardImage() {
        SwingUtilities.invokeLater(() -> {
            if (imageLabel != null) {
                // Create a credit card image
                ImageIcon cardIcon = createCreditCardImage(300, 200);
                imageLabel.setIcon(cardIcon);
                imageLabel.setText("");
                frame.repaint();
            }
        });
    }
    
    private static void hideCardImage() {
        SwingUtilities.invokeLater(() -> {
            if (imageLabel != null) {
                imageLabel.setIcon(null);
                imageLabel.setText("No active payment");
                frame.repaint();
            }
        });
    }
    
    private static ImageIcon createCreditCardImage(int width, int height) {
        // Create a buffered image for the credit card
        BufferedImage cardImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = cardImage.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw card background
        g2d.setColor(new Color(41, 128, 185)); // Blue color
        g2d.fillRoundRect(0, 0, width, height, 20, 20);
        
        // Draw chip
        g2d.setColor(new Color(212, 172, 13)); // Gold color
        g2d.fillRoundRect(30, 50, 50, 40, 5, 5);
        
        // Draw card number area
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString("**** **** **** 1234", 30, 120);
        
        // Draw expiry date
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("VALID THRU", 30, 145);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("12/25", 30, 160);
        
        // Draw card holder name
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("CARD HOLDER", 30, 180);
        
        g2d.dispose();
        return new ImageIcon(cardImage);
    }
}
