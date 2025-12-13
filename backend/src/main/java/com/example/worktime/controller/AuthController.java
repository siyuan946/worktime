package com.example.worktime.controller;

import com.example.worktime.model.User;
import com.example.worktime.repository.UserRepository;
import com.example.worktime.service.OperationLogContext;
import com.example.worktime.service.OperationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

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
    public Map<String, String> login(@RequestBody User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入账号和密码");
        }

        User existing = repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!existing.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        String requestedDepartment = user.getDepartment();
        if (!StringUtils.hasText(requestedDepartment)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择登录部门");
        }
        requestedDepartment = requestedDepartment.trim();

        String actualDepartment = existing.getDepartment();
        if (!StringUtils.hasText(actualDepartment)) {
            actualDepartment = "process";
        }
        actualDepartment = actualDepartment.trim();

        if (!actualDepartment.equalsIgnoreCase(requestedDepartment)) {
            String expectedLabel = "production".equalsIgnoreCase(actualDepartment) ? "生产部门" : "工艺部门";
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该账号所属部门为" + expectedLabel + "，请按规定选择");
        }

        logContext.setModule("认证");
        logContext.setEntity("User", existing.getUsername());
        logContext.setSummary("登录成功（" + ("production".equalsIgnoreCase(actualDepartment) ? "生产部门" : "工艺部门") + ")");
        logService.log(existing.getUsername(), "登录", null);

        Map<String, String> response = new HashMap<>();
        response.put("username", existing.getUsername());
        response.put("department", actualDepartment);
        return response;
    }
}
