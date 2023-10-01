package dev.antozy.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class InventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryApplication.class, args);
	}

}

@RestController
@RequiredArgsConstructor
class InventoryController {

	private final ProductRepository productRepository;

//	private Map<String, InventoryStatus> statuses = Map.of("1", new InventoryStatus(true), "2", new InventoryStatus(false));
	@GetMapping("inventories")
	public InventoryStatus getInventory(@RequestParam String productId){
		InventoryStatus inventoryStatus = new InventoryStatus();
		Optional<Product> order = productRepository.findById(Long.parseLong(productId));
		if(order.isPresent()) {
			inventoryStatus.setExists(true);
		} else {
			inventoryStatus.setExists(false);
		}
		return inventoryStatus;
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class InventoryStatus {
	private boolean exists;
}

@Entity(name = "product")
@Data
class Product {

	@Id
	@GeneratedValue
	private Long id;

	private String product;
	private double price;

}

interface ProductRepository extends CrudRepository<Product, Long> {

}
