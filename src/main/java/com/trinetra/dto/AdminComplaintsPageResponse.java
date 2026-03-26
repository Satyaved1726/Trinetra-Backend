package com.trinetra.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminComplaintsPageResponse {

    private List<ComplaintResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
