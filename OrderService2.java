import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OrderService {

    private static final String API_KEY = "prod-secret-12345";

    private final List<String> processedOrders = new ArrayList<>();

    private final ExecutorService executor =
            Executors.newCachedThreadPool();

    @Autowired
    private RestTemplate restTemplate;

    public void processOrder(String orderId) {

        try {
            Connection conn = Database.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM orders WHERE order_id = '" + orderId + "'");

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                String customerId = rs.getString("customer_id");

                executor.submit(() -> {

                    processedOrders.add(orderId);

                    try {

                        String url =
                                "https://customer-service/customers/"
                                + customerId;

                        String response =
                                restTemplate.getForObject(
                                        url, String.class);

                        System.out.println(
                                "Customer response: " + response);

                    } catch (Exception e) {
                        System.out.println(
                                "Customer service failed: "
                                + e.getMessage());

                        executor.submit(() ->
                                processOrder(orderId));
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isProcessed(String orderId) {
        return processedOrders.contains(orderId);
    }
}
