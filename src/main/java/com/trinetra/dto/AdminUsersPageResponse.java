package com.trinetra.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUsersPageResponse {

    private List<AdminUserAccessResponse> users;
}
