package org.example.ingresso.ingresso.service.dto;

import java.util.List;

public record PageResult<T>(List<T> content, int pageNumber, int pageSize, long totalElements, List<SortOrder> sort) {
    public PageResult {
        content = List.copyOf(content);
        sort = List.copyOf(sort);
    }
}