package com.tayperformance.controller.internalapi.admin;

import com.tayperformance.dto.user.CreateUserRequest;
import com.tayperformance.dto.user.UserDto;
import com.tayperformance.entity.Role;
import com.tayperformance.mapper.UserMapper;
import com.tayperformance.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    // ✅ list users + optional search
    @GetMapping
    public Page<UserDto> list(@RequestParam(required = false) String q, Pageable pageable) {
        return userService.list(q, pageable).map(UserMapper::toDto);
    }

    // ✅ create user (internal admin only)
    @PostMapping
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return UserMapper.toDto(
                userService.createUser(req.getUsername(), req.getPassword(), req.getRole())
        );
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(id, req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id,
                                           @Valid @RequestBody ChangeRoleRequest req) {
        userService.changeRole(id, req.getRole());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @Data
    public static class ChangeRoleRequest {
        @NotNull
        private Role role; // ✅ enum i.p.v. string
    }
}
