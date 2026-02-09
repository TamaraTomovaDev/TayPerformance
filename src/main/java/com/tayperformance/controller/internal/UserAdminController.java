package com.tayperformance.controller.internal;

import com.tayperformance.dto.user.UserDto;
import com.tayperformance.mapper.UserMapper;
import com.tayperformance.service.user.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public Page<UserDto> list(Pageable pageable) {
        return userService.list(pageable).map(UserMapper::toDto);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody ChangePasswordRequest req) {
        userService.changePassword(id, req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody ChangeRoleRequest req) {
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
        @NotBlank
        private String role; // ADMIN / STAFF
    }
}
