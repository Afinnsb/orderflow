package com.orderflow.product;

import com.orderflow.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateProduct() throws Exception {

        String request = """
                {
                    "name": "Test Keyboard",
                    "description": "Integration test product",
                    "price": 2499.90,
                    "stock": 25
                }
                """;

        mockMvc.perform(
                post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Test Keyboard"))
        .andExpect(jsonPath("$.price").value(2499.90))
        .andExpect(jsonPath("$.stock").value(25));
    }

    @Test
    void shouldGetProductById() throws Exception {

        Product product = new Product();

        product.setName("Test Mouse");
        product.setDescription("Integration test mouse");
        product.setPrice(new java.math.BigDecimal("1499.90"));
        product.setStock(10);

        Product savedProduct = productRepository.save(product);

        mockMvc.perform(
                get("/api/products/" + savedProduct.getId())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedProduct.getId().intValue()))
        .andExpect(jsonPath("$.name").value("Test Mouse"))
        .andExpect(jsonPath("$.price").value(1499.90))
        .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void shouldReturnBadRequestWhenProductIsInvalid() throws Exception {

        String request = """
                {
                    "name": "",
                    "description": "Invalid product",
                    "price": -100,
                    "stock": -5
                }
                """;

        mockMvc.perform(
                post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.price").exists())
        .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {

        mockMvc.perform(
                get("/api/products/999999")
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message")
                .value("Product not found: 999999"));
    }

    @Test
    void shouldGetAllProducts() throws Exception {

        Product first = new Product();
        first.setName("Keyboard");
        first.setDescription("Test keyboard");
        first.setPrice(new java.math.BigDecimal("2500.00"));
        first.setStock(10);

        Product second = new Product();
        second.setName("Mouse");
        second.setDescription("Test mouse");
        second.setPrice(new java.math.BigDecimal("1500.00"));
        second.setStock(20);

        productRepository.save(first);
        productRepository.save(second);

        mockMvc.perform(
                get("/api/products")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    }
}