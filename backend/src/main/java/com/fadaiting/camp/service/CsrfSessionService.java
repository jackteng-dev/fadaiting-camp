package com.fadaiting.camp.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;

@Service
public class CsrfSessionService {
    private final CookieCsrfTokenRepository repository;
    public CsrfSessionService(CookieCsrfTokenRepository repository) { this.repository = repository; }
    public void rotate(HttpServletRequest request, HttpServletResponse response) {
        repository.saveToken(repository.generateToken(request),request,response);
    }
}
