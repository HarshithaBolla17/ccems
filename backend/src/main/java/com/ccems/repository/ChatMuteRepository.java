package com.ccems.repository;

import com.ccems.model.ChatMute;
import com.ccems.model.ChatScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMuteRepository extends JpaRepository<ChatMute, Long> {

    @Query("select m from ChatMute m where m.scope = :scope and m.scopeId = :sid and m.userId = :uid")
    Optional<ChatMute> find(@Param("scope") ChatScope scope, @Param("sid") Long sid, @Param("uid") Long uid);

    @Query("select m from ChatMute m where m.scope = :scope and m.scopeId = :sid")
    List<ChatMute> findForRoom(@Param("scope") ChatScope scope, @Param("sid") Long sid);
}
