package com.coworkinghub.coworkinghub.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

final class SecurityResponses {

    private SecurityResponses() {
    }

    /** Writes a 401 response in the same problem-details shape the controllers use. */
    static void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\""
                        + detail.replace("\"", "'") + "\"}");
    }
}
