package io.pinkspider.springbootjwttemplate.global.service;

import io.pinkspider.springbootjwttemplate.global.domain.User;
import io.pinkspider.springbootjwttemplate.global.dto.request.AddUserRequest;
import io.pinkspider.springbootjwttemplate.global.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public Long save(AddUserRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Duplicated email");
        }

        return userRepository.save(User.builder()
                .email(dto.getEmail())
                .auth(dto.getAuth())
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .build()).getId();
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }
}
