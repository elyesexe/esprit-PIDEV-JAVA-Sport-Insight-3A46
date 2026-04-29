package tn.esprit.repositories;

public final class ProductRepository {
    private ProductRepository() {
    }

    public enum ProductSortField {
        NAME("Name"),
        CATEGORY("Category"),
        PRICE("Price"),
        STOCK("Stock"),
        BRAND("Brand");

        private final String label;

        ProductSortField(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum SortDirection {
        ASC("Asc"),
        DESC("Desc");

        private final String label;

        SortDirection(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
