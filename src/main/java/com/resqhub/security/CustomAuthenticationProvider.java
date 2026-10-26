package com.resqhub.security;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.resqhub.dao.UserDAO;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.User;
import com.resqhub.util.PasswordUtil;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserDAO userDao = new UserDAO();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        try {
            User user = userDao.findByUsername(username);
            if (user == null) {
                throw new BadCredentialsException("Invalid username or password");
            }

            if (user.getAccountStatus() == AccountStatus.LOCKED) {
                throw new LockedException("Account is locked. Please contact an administrator.");
            }
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new LockedException("Account is inactive.");
            }

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setAccountStatus(AccountStatus.LOCKED);
                }
                userDao.save(user);
                throw new BadCredentialsException("Invalid username or password");
            }

            // Successful authentication
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
            }
            user.setLastLogin(LocalDateTime.now());
            if (PasswordUtil.needsUpgrade(user.getPasswordHash())) {
                user.setPasswordHash(PasswordUtil.hash(password));
            }
            userDao.save(user);

            String roleName = user.getRole() == null ? "CITIZEN" : user.getRole().name();
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + roleName));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, password, authorities);
            return authToken;

        } catch (AuthenticationException ae) {
            throw ae;
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
