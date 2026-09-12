package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.shared.exception.DuplicateEmailException;
import com.amorim.finance_manager.shared.exception.InvalidProfileUpdateException;
import com.amorim.finance_manager.user.dto.UpdateProfileRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.mapper.UserMapper;
import com.amorim.finance_manager.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        User user = currentUserService.getCurrentUser();

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
        validate(request);

        User user = currentUserService.getCurrentUser();

        if (request.email() != null
                && !request.email().equals(user.getEmail())
                && userRepository.existsByEmailAndIdNot(request.email(), user.getId())) {
            throw new DuplicateEmailException();
        }

        userMapper.updateEntity(request, user);

        try {
            User updateUser = userRepository.saveAndFlush(user);
            return userMapper.toResponse(updateUser);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    private void validate(UpdateProfileRequest request) {
        if (request.name() == null && request.email() == null) {
            throw new InvalidProfileUpdateException("Informe ao menos um campo para atualização");
        }

        if (request.name() != null && request.name().isBlank()) {
            throw new InvalidProfileUpdateException("Nome não pode ser vazio");
        }

        if (request.email() != null && request.email().isBlank()) {
            throw new InvalidProfileUpdateException("E-mail não pode ser vazio");
        }
    }
}