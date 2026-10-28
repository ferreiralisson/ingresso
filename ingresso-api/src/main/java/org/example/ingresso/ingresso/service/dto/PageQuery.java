package org.example.ingresso.ingresso.service.dto;

import java.util.List;

public record PageQuery(int pageNumber, int pageSize, List<SortOrder> sort) {
    public PageQuery {
        sort = List.copyOf(sort);
    }
}