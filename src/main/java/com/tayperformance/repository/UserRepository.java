package com.tayperformance.repository;

import com.tayperformance.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(String q, Pageable pageable);
}
