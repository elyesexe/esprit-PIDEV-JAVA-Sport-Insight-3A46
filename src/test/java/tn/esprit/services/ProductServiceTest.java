package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Product;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_PRODUCT_" + System.currentTimeMillis() + "_";

    private static ProductService productService;

    @BeforeAll
    static void setup() throws SQLException {
        productService = new ProductService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        productService = new ProductService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (Product product : productService.getAll()) {
            if (product.getName() != null && product.getName().startsWith(TEST_PREFIX)) {
                productService.delete(product.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterProduct() throws SQLException {
        String name = TEST_PREFIX + "AJOUT";
        Product product = new Product(
                name,
                "Maillot",
                new BigDecimal("199.90"),
                15,
                "L",
                "Nike",
                "product-ajout.png"
        );

        productService.add(product);

        Product productAjoute = findProductByName(productService, name);
        assertNotNull(productAjoute);
        assertEquals("Maillot", productAjoute.getCategory());
        assertEquals(new BigDecimal("199.90"), productAjoute.getPrice());
        assertEquals(15, productAjoute.getStock());
        assertEquals("L", productAjoute.getSize());
        assertEquals("Nike", productAjoute.getBrand());
        assertEquals("product-ajout.png", productAjoute.getImage());
    }

    @Test
    @Order(2)
    void testModifierProduct() throws SQLException {
        Product product = createProduct(productService, TEST_PREFIX, "MODIFIER");
        assertNotNull(product);

        product.setName(TEST_PREFIX + "MODIFIE");
        product.setCategory("Chaussure");
        product.setPrice(new BigDecimal("249.50"));
        product.setStock(20);
        product.setSize("42");
        product.setBrand("Adidas");
        product.setImage("product-modifie.png");

        productService.update(product);

        Product productModifie = productService.getById(product.getId());
        assertNotNull(productModifie);
        assertEquals(TEST_PREFIX + "MODIFIE", productModifie.getName());
        assertEquals("Chaussure", productModifie.getCategory());
        assertEquals(new BigDecimal("249.50"), productModifie.getPrice());
        assertEquals(20, productModifie.getStock());
        assertEquals("42", productModifie.getSize());
        assertEquals("Adidas", productModifie.getBrand());
        assertEquals("product-modifie.png", productModifie.getImage());
    }

    @Test
    @Order(3)
    void testSupprimerProduct() throws SQLException {
        Product product = createProduct(productService, TEST_PREFIX, "SUPPRIMER");
        assertNotNull(product);

        productService.delete(product.getId());

        Product productSupprime = productService.getById(product.getId());
        boolean existeEncore = productService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), product.getId()));

        assertNull(productSupprime);
        assertFalse(existeEncore);
    }

    @Test
    @Order(4)
    void testAdvancedSearchProduct() throws SQLException {
        Product matchingProduct = new Product(
                TEST_PREFIX + "ADV_ALPHA",
                "Boots",
                new BigDecimal("120.00"),
                7,
                "42",
                "Nike",
                "advanced-alpha.png"
        );
        Product otherProduct = new Product(
                TEST_PREFIX + "ADV_BETA",
                "Jersey",
                new BigDecimal("250.00"),
                0,
                "L",
                "Adidas",
                "advanced-beta.png"
        );
        productService.add(matchingProduct);
        productService.add(otherProduct);

        var results = productService.advancedSearch(
                "adv",
                "Boots",
                "Nike",
                new BigDecimal("100.00"),
                new BigDecimal("130.00"),
                5,
                "42",
                true
        );

        assertEquals(1, results.size());
        assertEquals(TEST_PREFIX + "ADV_ALPHA", results.get(0).getName());
    }
}
