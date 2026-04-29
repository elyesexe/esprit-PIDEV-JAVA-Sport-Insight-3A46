package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Product;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAiServiceTest {

    @Test
    void localGeneratorBuildsDescriptionAndTags() throws Exception {
        ProductAiService service = new ProductAiService();
        ProductAiService.ProductDraft draft = new ProductAiService.ProductDraft(
                "Mercurial Boot",
                "Boots",
                "Nike",
                "42",
                new BigDecimal("349.90")
        );

        Method method = ProductAiService.class.getDeclaredMethod("generateLocalContent", ProductAiService.ProductDraft.class);
        method.setAccessible(true);
        ProductAiService.GeneratedProductContent content =
                (ProductAiService.GeneratedProductContent) method.invoke(service, draft);

        assertNotNull(content);
        assertTrue(content.marketingTitle().contains("Nike"));
        assertTrue(content.description().contains("349.90 DT"));
        assertTrue(content.tags().contains("nike"));
        assertTrue(content.tags().contains("boots"));
    }

    @Test
    void localSearchParserExtractsFiltersFromNaturalLanguageQuery() throws Exception {
        ProductAiService service = new ProductAiService();
        List<Product> products = List.of(
                new Product(1, "Mercurial", "Boots", new BigDecimal("320.00"), 7, "42", "Nike", "boot.png", "Speed boot", "boots, nike"),
                new Product(2, "Training Jersey", "Jerseys", new BigDecimal("120.00"), 12, "M", "Adidas", "jersey.png", "Training jersey", "jersey, adidas")
        );

        Method method = ProductAiService.class.getDeclaredMethod("localSearchQuery", String.class, List.class);
        method.setAccessible(true);
        ProductAiService.SmartProductQuery query =
                (ProductAiService.SmartProductQuery) method.invoke(service, "nike boots under 350 size 42 in stock", products);

        assertEquals("Boots", query.category());
        assertEquals("Nike", query.brand());
        assertEquals("42", query.size());
        assertEquals(new BigDecimal("350.00"), query.maxPrice());
        assertTrue(query.inStockOnly());
    }

    @Test
    void localSearchParserRecognizesCategorySynonyms() throws Exception {
        ProductAiService service = new ProductAiService();
        List<Product> products = List.of(
                new Product(1, "Mercurial", "Boots", new BigDecimal("320.00"), 7, "42", "Nike", "boot.png", "Speed boot", "boots, nike"),
                new Product(2, "Training Jersey", "Jerseys", new BigDecimal("120.00"), 12, "M", "Adidas", "jersey.png", "Training jersey", "jersey, adidas")
        );

        Method method = ProductAiService.class.getDeclaredMethod("localSearchQuery", String.class, List.class);
        method.setAccessible(true);
        ProductAiService.SmartProductQuery query =
                (ProductAiService.SmartProductQuery) method.invoke(service, "nike shoes under 350", products);

        assertEquals("Boots", query.category());
        assertEquals("Nike", query.brand());
        assertEquals(new BigDecimal("350.00"), query.maxPrice());
        assertFalse(query.keywords().contains("shoes"));
    }

    @Test
    void localSearchParserDerivesCheapAndExpensivePriceBands() throws Exception {
        ProductAiService service = new ProductAiService();
        List<Product> products = List.of(
                new Product(1, "Budget Boots", "Boots", new BigDecimal("90.00"), 10, "41", "Nike", "boot1.png", "Entry boots", "boots"),
                new Product(2, "Match Boots", "Boots", new BigDecimal("150.00"), 8, "42", "Nike", "boot2.png", "Match boots", "boots"),
                new Product(3, "Elite Boots", "Boots", new BigDecimal("320.00"), 5, "43", "Nike", "boot3.png", "Elite boots", "boots")
        );

        Method method = ProductAiService.class.getDeclaredMethod("localSearchQuery", String.class, List.class);
        method.setAccessible(true);
        ProductAiService.SmartProductQuery cheapQuery =
                (ProductAiService.SmartProductQuery) method.invoke(service, "cheap shoes", products);
        ProductAiService.SmartProductQuery expensiveQuery =
                (ProductAiService.SmartProductQuery) method.invoke(service, "expensive shoes", products);

        assertEquals("Boots", cheapQuery.category());
        assertEquals(new BigDecimal("150.00"), cheapQuery.maxPrice());
        assertTrue(cheapQuery.summary().contains("budget range <= 150.00 DT"));

        assertEquals("Boots", expensiveQuery.category());
        assertEquals(new BigDecimal("320.00"), expensiveQuery.minPrice());
        assertTrue(expensiveQuery.summary().contains("premium range >= 320.00 DT"));
    }
}
