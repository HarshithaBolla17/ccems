package com.ccems.controller;

import com.ccems.dto.HubDtos.HomeDto;
import com.ccems.security.AuthUser;
import com.ccems.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService home;

    public HomeController(HomeService home) {
        this.home = home;
    }

    @GetMapping
    public HomeDto home() {
        return home.home(AuthUser.current());
    }
}
