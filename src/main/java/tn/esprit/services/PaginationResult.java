package tn.esprit.services;

import java.util.List;

public record PaginationResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages
) {
    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }
}
