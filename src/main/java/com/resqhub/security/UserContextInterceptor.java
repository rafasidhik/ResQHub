package com.resqhub.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.resqhub.controller.NotificationController;
import com.resqhub.dao.UserDAO;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;
import com.resqhub.web.Module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserDAO userDao = new UserDAO();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            User user = null;
            if (auth.getPrincipal() instanceof User) {
                user = (User) auth.getPrincipal();
            } else {
                user = userDao.findByUsername(auth.getName());
            }
            if (user != null) {
                SessionManager.getInstance().login(user);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && SessionManager.getInstance().isLoggedIn()) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            modelAndView.addObject("currentUser", currentUser);

            // Group modules for navigation
            Map<String, List<Module>> groups = new LinkedHashMap<>();
            for (Module m : Module.values()) {
                if (m.allows(currentUser.getRole())) {
                    groups.computeIfAbsent(m.getGroup(), k -> new ArrayList<>()).add(m);
                }
            }
            modelAndView.addObject("navGroups", groups);

            try {
                int unread = new NotificationController().countUnread();
                modelAndView.addObject("unreadCount", unread);
            } catch (Exception e) {
                modelAndView.addObject("unreadCount", 0);
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        SessionManager.getInstance().logout();
    }
}
