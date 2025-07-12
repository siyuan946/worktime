package com.example.worktime.controller;

import com.example.worktime.model.User;
import com.example.worktime.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    private final UserRepository repository;

    public AuthController(UserRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/login")
    public void login(@RequestBody User user) {
        if (repository.findByUsernameAndPassword(user.getUsername(), user.getPassword()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
    }
}
