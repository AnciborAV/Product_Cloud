package org.example.aav.dto;

import jakarta.validation.constraints.*;

public record ContactForm(
    @NotBlank @Size(max=100) String displayName,
    @NotBlank @Size(max=100) String lastName,
    @NotBlank @Pattern(regexp="[+0-9() -]{7,30}") String phone,
    @NotBlank @Email @Size(max=254) String email) {}
