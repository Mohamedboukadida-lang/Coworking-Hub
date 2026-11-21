package com.coworkinghub.coworkinghub.dto;

import com.coworkinghub.coworkinghub.model.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        String surname,
        Integer age,
        String profession,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        AccountType typeCompte) {
}
