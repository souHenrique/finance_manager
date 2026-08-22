package com.amorim.finance_manager.user.service;

import com.amorim.finance_manager.security.AuthenticatedUserProvider;
import com.amorim.finance_manager.shared.exception.UnauthenticatedUserException;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class CurrentUserService implements AuthenticatedUserProvider {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedUserException();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            throw new UnauthenticatedUserException();
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(UnauthenticatedUserException::new);
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
