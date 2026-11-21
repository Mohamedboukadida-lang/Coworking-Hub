package com.coworkinghub.coworkinghub.dto;

/** Profile fields a user may change. Null fields are left unchanged. */
public record UserUpdateRequest(String name, String surname, Integer age, String profession) {
}
