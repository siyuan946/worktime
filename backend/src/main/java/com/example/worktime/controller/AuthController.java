package com.example.worktime.controller;

import com.example.worktime.model.User;
import com.example.worktime.repository.UserRepository;
import com.example.worktime.service.OperationLogContext;
import com.example.worktime.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    private final UserRepository repository;
    private final OperationLogService logService;
    private final OperationLogContext logContext;

    public AuthController(UserRepository repository, OperationLogService logService, OperationLogContext logContext) {
        this.repository = repository;
        this.logService = logService;
        this.logContext = logContext;
    }

    @PostMapping("/login")
    public void login(@RequestBody User user) {
        if (!repository.findByUsernameAndPassword(user.getUsername(), user.getPassword()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        logContext.setModule("认证");
        logContext.setEntity("User", user.getUsername());
        logContext.setSummary("登录成功");
        logService.log(user.getUsername(), "登录", null);
    }
}
