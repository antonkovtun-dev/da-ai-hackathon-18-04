package com.example.chat.users;

import com.example.chat.auth.AuthUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        userService.deleteAccount(currentUserId());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(currentUserId(), req.currentPassword(), req.newPassword());
    }

    private UUID currentUserId() {
        return ((AuthUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getUserId();
    }

    record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword
    ) {}
}
