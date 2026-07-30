package com.securechat.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
            select u from AppUser u
            where (lower(u.username) like lower(concat(:query, '%'))
                or u.phoneNumber like concat(:query, '%'))
              and u.id <> :currentUserId
            order by u.username asc
            """)
    List<AppUser> search(@Param("query") String query, @Param("currentUserId") UUID currentUserId, Pageable pageable);
}

