package com.walloop.engine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerDto {
    Long id;

    @NotBlank
    String name;

    @Email
    @NotBlank
    String email;
}
