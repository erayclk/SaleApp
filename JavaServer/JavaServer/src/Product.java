public class Product {
    public int productId;
    public String productName;
    public double price;
    public int vatRate;

    @Override
    public String toString() {
        return "Product [productId=" + productId + ", productName=" + productName + ", price=" + price;
    }
}
