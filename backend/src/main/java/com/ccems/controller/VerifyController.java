package com.ccems.controller;

import com.ccems.dto.Dtos.VerifyDto;
import com.ccems.service.CertificateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint: anyone can check a certificate code without signing in. */
@RestController
@RequestMapping("/api/verify")
public class VerifyController {

    private final CertificateService certificates;

    public VerifyController(CertificateService certificates) {
        this.certificates = certificates;
    }

    @GetMapping("/{code}")
    public VerifyDto verify(@PathVariable String code) {
        return certificates.verify(code);
    }
}
