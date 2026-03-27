package com.trinetra.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAssignRequest {

    @JsonAlias({"assigned_to", "assignedTo"})
    private String assignedTo;

    @JsonAlias({"officer_id", "officerId", "admin_id", "adminId"})
    private UUID adminId;

    @JsonAlias({"admin"})
    private String admin;
}
