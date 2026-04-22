package com.example.demoservice.userindex;

import com.example.demorepository.entity.User;

public record IndexedUser(Long id, String username, String email, String password, String role) {

    public static IndexedUser fromUser(User user) {
        return new IndexedUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
    }
}
