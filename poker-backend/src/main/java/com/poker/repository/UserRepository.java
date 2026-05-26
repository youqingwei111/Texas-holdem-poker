package com.poker.repository;

import com.poker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.chips = u.chips + :amount WHERE u.id = :userId")
    void addChips(@Param("userId") Long userId, @Param("amount") Long amount);

    @Modifying
    @Query("UPDATE User u SET u.chips = u.chips - :amount WHERE u.id = :userId AND u.chips >= :amount")
    int deductChips(@Param("userId") Long userId, @Param("amount") Long amount);
}