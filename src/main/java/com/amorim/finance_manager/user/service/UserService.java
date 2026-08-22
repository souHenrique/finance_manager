package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.shared.exception.DuplicateEmailException;
import com.amorim.finance_manager.user.dto.RegisterRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.mapper.UserMapper;
import com.amorim.finance_manager.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = userMapper.toEntity(request);
        user.setPasswordHash(encodedPassword);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return userMapper.toResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
