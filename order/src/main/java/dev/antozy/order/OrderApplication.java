package dev.antozy.order;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableFeignClients
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}

}

@RestController
@RequiredArgsConstructor
class OrderController {

	private final OrderService orderService;

	@PostMapping("/orders")
	@ResponseStatus(HttpStatus.CREATED)
	public void placeOrder(@RequestBody PlaceOrderRequest request) {
		this.orderService.placeOrder(request);
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PlaceOrderRequest {
	private String product;
	private double price;
}

@Service
@RequiredArgsConstructor
class OrderService {

	private final KafkaTemplate kafkaTemplate;
	private final OrderRepository orderRepository;
	private final InventoryClient inventoryClient;

	public void placeOrder(PlaceOrderRequest request) {
//		System.out.println("Order placed for : " + request.getProduct() + " - " + request.getPrice());
	InventoryStatus status = inventoryClient.exists(request.getProduct());
		if (!status.isExists()) {
			throw new EntityNotFoundException("Product does not exists");
		}

		Order order = new Order();
		order.setProduct(request.getProduct());
		order.setPrice(request.getPrice());
		order.setStatus("PLACED");
		Order orderK = this.orderRepository.save(order);
		this.kafkaTemplate.send("prod.orders.placed", String.valueOf(orderK.getId()), OrderPlacedEvent.builder()
				.product(request.getProduct())
				.price(request.getPrice())
				.orderId(order.getId().intValue())
				.build());
	}

	@KafkaListener(topics = "prod.orders.shipped", groupId = "order-group")
	public void handleOrderShippingEvent(String orderId) {
		this.orderRepository.findById(Long.valueOf(orderId)).ifPresent(order -> {
			order.setStatus("SHIPPED");
			this.orderRepository.save(order);
		});
	}
}

@Data
@Builder
class OrderPlacedEvent {
	private int orderId;
	private String product;
	private double price;
}

interface OrderRepository extends CrudRepository<Order, Long> {

}

@Entity(name = "orders")
@Data
class Order {

	@Id
	@GeneratedValue
	private Long id;

	private String product;
	private double price;

	private String status;
}

@FeignClient(url = "http://localhost:8092", name = "inventories")
interface InventoryClient {
	@GetMapping("/inventories")
	InventoryStatus exists(@RequestParam("productId") String productId);
}

@Data
class InventoryStatus {
	private boolean exists;
}