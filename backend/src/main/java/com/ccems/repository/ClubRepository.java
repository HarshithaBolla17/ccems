package com.ccems.repository;

import com.ccems.model.Club;
import com.ccems.model.ClubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
            select c from Club c
            where (:status is null or c.status = :status)
              and (:q is null or lower(c.name) like lower(concat('%', :q, '%'))
                   or lower(c.description) like lower(concat('%', :q, '%')))
            """)
    Page<Club> search(@Param("status") ClubStatus status, @Param("q") String q, Pageable pageable);

    List<Club> findByCoordinator_UserId(Long coordinatorId);

    boolean existsByNameIgnoreCase(String name);
}
