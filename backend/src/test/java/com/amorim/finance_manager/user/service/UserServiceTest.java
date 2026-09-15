package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.shared.exception.DuplicateEmailException;
import com.amorim.finance_manager.user.dto.RegisterRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.mapper.UserMapper;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest(
                "Walter White",
                "walter.white@example.com",
                "SenhaSegura123"
        );

        User user = new User();
        UserResponse response = new UserResponse(
                UUID.randomUUID(),
                "Walter White",
                "walter.white@example.com",
                null,
                null
        );

        when(userRepository.existsByEmail("walter.white@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SenhaSegura123"))
                .thenReturn("$2a$10$hash");

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userMapper.toResponse(user))
                .thenReturn(response);

        userService.register(request);

        assertThat(user.getPasswordHash())
                .isEqualTo("$2a$10$hash");

        verify(passwordEncoder)
                .encode("SenhaSegura123");

        verify(userMapper)
                .toEntity(request);

        verify(userRepository)
                .saveAndFlush(user);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Walter White",
                "walter.white@example.com",
                "SenhaSegura123"
        );

        when(userRepository.existsByEmail("walter.white@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }
}
