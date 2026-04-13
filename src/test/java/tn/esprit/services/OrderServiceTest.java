package tn.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tn.esprit.entities.Product;
import tn.esprit.entities.User;
import tn.esprit.security.UserRoles;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderServiceTest extends AbstractServiceTestSupport {

    private static final String TEST_PREFIX = "JUNIT_ORDER_" + System.currentTimeMillis() + "_";
    private static final String PRODUCT_PREFIX = TEST_PREFIX + "PRODUCT_";
    private static final String USER_PREFIX = TEST_PREFIX + "USER_";

    private static OrderService orderService;
    private static ProductService productService;
    private static UserService userService;

    @BeforeAll
    static void setup() throws SQLException {
        orderService = new OrderService();
        productService = new ProductService();
        userService = new UserService();
    }

    @BeforeEach
    void initServices() throws SQLException {
        orderService = new OrderService();
        productService = new ProductService();
        userService = new UserService();
    }

    @AfterEach
    void cleanUp() throws SQLException {
        for (tn.esprit.entities.Order order : orderService.getAll()) {
            if (order.getContactEmail() != null && order.getContactEmail().startsWith(TEST_PREFIX.toLowerCase(Locale.ROOT))) {
                orderService.delete(order.getId());
            }
        }

        for (Product product : productService.getAll()) {
            if (product.getName() != null && product.getName().startsWith(PRODUCT_PREFIX)) {
                productService.delete(product.getId());
            }
        }

        for (User user : userService.getAll()) {
            if (user.getEmail() != null && user.getEmail().startsWith(USER_PREFIX.toLowerCase(Locale.ROOT))) {
                userService.delete(user.getId());
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterOrder() throws SQLException {
        Product product = createProduct(productService, PRODUCT_PREFIX, "AJOUT");
        User coach = createUser(userService, USER_PREFIX, "AJOUT", UserRoles.ROLE_ENTRAINEUR);
        assertNotNull(product);
        assertNotNull(coach);

        tn.esprit.entities.Order order = new tn.esprit.entities.Order(
                2,
                java.time.LocalDate.of(2026, 8, 5),
                "PENDING",
                "CARD",
                "UNPAID",
                "L",
                emailFor(TEST_PREFIX, "AJOUT"),
                phoneFor(TEST_PREFIX + "AJOUT", 41000000),
                "Shipping Ajout",
                "Billing Ajout",
                new BigDecimal("399.80"),
                product.getId(),
                coach.getId()
        );

        orderService.add(order);

        tn.esprit.entities.Order orderAjoute = findOrderByContactEmail(orderService, emailFor(TEST_PREFIX, "AJOUT"));
        assertNotNull(orderAjoute);
        assertEquals(2, orderAjoute.getQuantity());
        assertEquals(java.time.LocalDate.of(2026, 8, 5), orderAjoute.getOrderDate());
        assertEquals("PENDING", orderAjoute.getStatus());
        assertEquals("CARD", orderAjoute.getPaymentMethod());
        assertEquals("UNPAID", orderAjoute.getPaymentStatus());
        assertEquals("L", orderAjoute.getSize());
        assertEquals("Shipping Ajout", orderAjoute.getShippingAddress());
        assertEquals("Billing Ajout", orderAjoute.getBillingAddress());
        assertEquals(new BigDecimal("399.80"), orderAjoute.getTotalAmount());
        assertEquals(product.getId(), orderAjoute.getProductId());
        assertEquals(coach.getId(), orderAjoute.getEntraineurId());
    }

    @Test
    @Order(2)
    void testModifierOrder() throws SQLException {
        Product productInitial = createProduct(productService, PRODUCT_PREFIX, "MODIFIER_INIT");
        Product productModifie = createProduct(productService, PRODUCT_PREFIX, "MODIFIER_NEW");
        User coachInitial = createUser(userService, USER_PREFIX, "MODIFIER_INIT", UserRoles.ROLE_ENTRAINEUR);
        User coachModifie = createUser(userService, USER_PREFIX, "MODIFIER_NEW", UserRoles.ROLE_ENTRAINEUR);
        tn.esprit.entities.Order order = createOrder(orderService, TEST_PREFIX, "MODIFIER", productInitial.getId(), coachInitial.getId());
        assertNotNull(order);

        order.setQuantity(5);
        order.setOrderDate(java.time.LocalDate.of(2026, 8, 6));
        order.setStatus("CONFIRMED");
        order.setPaymentMethod("CASH");
        order.setPaymentStatus("PAID");
        order.setSize("XL");
        order.setContactEmail(emailFor(TEST_PREFIX, "MODIFIE"));
        order.setContactPhone(phoneFor(TEST_PREFIX + "MODIFIE", 42000000));
        order.setShippingAddress("Shipping Modifie");
        order.setBillingAddress("Billing Modifie");
        order.setTotalAmount(new BigDecimal("520.00"));
        order.setProductId(productModifie.getId());
        order.setEntraineurId(coachModifie.getId());

        orderService.update(order);

        tn.esprit.entities.Order orderModifie = orderService.getById(order.getId());
        assertNotNull(orderModifie);
        assertEquals(5, orderModifie.getQuantity());
        assertEquals(java.time.LocalDate.of(2026, 8, 6), orderModifie.getOrderDate());
        assertEquals("CONFIRMED", orderModifie.getStatus());
        assertEquals("CASH", orderModifie.getPaymentMethod());
        assertEquals("PAID", orderModifie.getPaymentStatus());
        assertEquals("XL", orderModifie.getSize());
        assertEquals(emailFor(TEST_PREFIX, "MODIFIE"), orderModifie.getContactEmail());
        assertEquals(phoneFor(TEST_PREFIX + "MODIFIE", 42000000), orderModifie.getContactPhone());
        assertEquals("Shipping Modifie", orderModifie.getShippingAddress());
        assertEquals("Billing Modifie", orderModifie.getBillingAddress());
        assertEquals(new BigDecimal("520.00"), orderModifie.getTotalAmount());
        assertEquals(productModifie.getId(), orderModifie.getProductId());
        assertEquals(coachModifie.getId(), orderModifie.getEntraineurId());
    }

    @Test
    @Order(3)
    void testSupprimerOrder() throws SQLException {
        Product product = createProduct(productService, PRODUCT_PREFIX, "SUPPRIMER");
        User coach = createUser(userService, USER_PREFIX, "SUPPRIMER", UserRoles.ROLE_ENTRAINEUR);
        tn.esprit.entities.Order order = createOrder(orderService, TEST_PREFIX, "SUPPRIMER", product.getId(), coach.getId());
        assertNotNull(order);

        orderService.delete(order.getId());

        tn.esprit.entities.Order orderSupprime = orderService.getById(order.getId());
        boolean existeEncore = orderService.getAll().stream()
                .anyMatch(item -> Objects.equals(item.getId(), order.getId()));

        assertNull(orderSupprime);
        assertFalse(existeEncore);
    }
}
