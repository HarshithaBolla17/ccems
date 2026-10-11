package com.ccems.repository;

import com.ccems.model.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    Optional<ClubMember> findByClub_ClubIdAndStudent_UserId(Long clubId, Long studentId);

    boolean existsByClub_ClubIdAndStudent_UserId(Long clubId, Long studentId);

    long countByClub_ClubId(Long clubId);

    List<ClubMember> findByClub_ClubId(Long clubId);

    List<ClubMember> findByStudent_UserIdOrderByJoinedAtDesc(Long studentId);

    @Query("select m.club.clubId from ClubMember m where m.student.userId = :studentId")
    List<Long> findClubIdsByStudent(@Param("studentId") Long studentId);

    @Query("select m.club.clubId, count(m) from ClubMember m group by m.club.clubId")
    List<Object[]> countMembersPerClub();
}
