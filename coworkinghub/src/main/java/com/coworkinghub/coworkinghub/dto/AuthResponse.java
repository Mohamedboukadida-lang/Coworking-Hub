package com.coworkinghub.coworkinghub.dto;

import com.coworkinghub.coworkinghub.model.Compte;

public record AuthResponse(String token, Compte user) {
}
