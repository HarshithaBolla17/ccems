package com.ccems.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long clubId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Free-text grouping used by the UI (Technology, Arts, Sports...). */
    private String category;

    // one-to-many from users: a coordinator can run one or more clubs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubStatus status = ClubStatus.ACTIVE;
}
