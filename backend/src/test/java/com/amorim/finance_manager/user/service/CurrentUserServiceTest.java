package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.shared.exception.UnauthenticatedUserException;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserId() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("user-a@example.com");

        authenticateAs("user-a@example.com");

        when(userRepository.findByEmail("user-a@example.com"))
                .thenReturn(Optional.of(user));

        UUID result = currentUserService.getCurrentUserId();

        assertThat(result).isEqualTo(userId);

        verify(userRepository)
                .findByEmail("user-a@example.com");
    }

    @Test
    void shouldReturnCurrentUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user-a@example.com");

        authenticateAs("user-a@example.com");

        when(userRepository.findByEmail("user-a@example.com"))
                .thenReturn(Optional.of(user));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isSameAs(user);
    }

    @Test
    void shouldRejectUnauthenticatedUser() {
        assertThatThrownBy(() ->
                currentUserService.getCurrentUser()
        )
                .isInstanceOf(UnauthenticatedUserException.class);

        verifyNoInteractions(userRepository);
    }

    private void authenticateAs(String email) {
        UserDetails principal =
                org.springframework.security.core.userdetails.User
                        .withUsername(email)
                        .password("password-hash")
                        .authorities(
                                Collections.<GrantedAuthority>emptyList()
                        )
                        .build();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}
