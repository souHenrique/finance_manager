package com.amorim.finance_manager.security;

import com.amorim.finance_manager.user.entity.User;

import java.util.UUID;

public interface AuthenticatedUserProvider {

    UUID getCurrentUserId();

    User getCurrentUser();
}
