package com.ccems.controller;

import com.ccems.dto.Dtos.*;
import com.ccems.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return admin.stats();
    }

    @GetMapping("/coordinators")
    public List<UserDto> coordinators() {
        return admin.coordinators();
    }
}
