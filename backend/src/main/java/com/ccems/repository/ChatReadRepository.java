package com.ccems.repository;

import com.ccems.model.ChatRead;
import com.ccems.model.ChatScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {

    @Query("select r from ChatRead r where r.userId = :uid and r.scope = :scope and r.scopeId = :sid")
    Optional<ChatRead> find(@Param("uid") Long uid, @Param("scope") ChatScope scope, @Param("sid") Long sid);
}
