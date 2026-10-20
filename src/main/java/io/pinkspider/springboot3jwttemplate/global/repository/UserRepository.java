package io.pinkspider.springboot3jwttemplate.global.repository;

import io.pinkspider.springboot3jwttemplate.global.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

