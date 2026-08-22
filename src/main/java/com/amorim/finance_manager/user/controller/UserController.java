package com.amorim.finance_manager.user.controller;

import com.amorim.finance_manager.user.dto.UpdateProfileRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userProfileService.getCurrentProfile());
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateCurrentProfile(request));
    }
}
