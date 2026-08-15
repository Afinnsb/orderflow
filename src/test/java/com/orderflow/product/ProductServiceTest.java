package com.orderflow.product;

import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {

        product = new Product();

        product.setName("Mechanical Keyboard");
        product.setDescription("RGB mechanical keyboard");
        product.setPrice(new BigDecimal("2499.90"));
        product.setStock(25);
    }

    @Test
    void shouldCreateProduct() {

        ProductRequest request = new ProductRequest(
                "Mechanical Keyboard",
                "RGB mechanical keyboard",
                new BigDecimal("2499.90"),
                25
        );

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.createProduct(request);

        assertThat(response.name())
                .isEqualTo("Mechanical Keyboard");

        assertThat(response.price())
                .isEqualByComparingTo("2499.90");

        assertThat(response.stock())
                .isEqualTo(25);

        verify(productRepository)
                .save(any(Product.class));
    }

    @Test
    void shouldGetProductById() {

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response =
                productService.getProductById(1L);

        assertThat(response.name())
                .isEqualTo("Mechanical Keyboard");

        assertThat(response.stock())
                .isEqualTo(25);

        verify(productRepository)
                .findById(1L);
    }

    @Test
void shouldThrowExceptionWhenProductDoesNotExist() {

    when(productRepository.findById(999L))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            productService.getProductById(999L)
    )
            .isInstanceOf(
                    com.orderflow.exception.ResourceNotFoundException.class
            )
            .hasMessage("Product not found: 999");

    verify(productRepository)
            .findById(999L);
}
}