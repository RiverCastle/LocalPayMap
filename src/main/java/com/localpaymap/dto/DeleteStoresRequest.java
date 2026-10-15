package com.localpaymap.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DeleteStoresRequest(@NotEmpty List<Long> ids) {}
