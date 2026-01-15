package com.jobupdater;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String company;

    @Column(length = 500)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String url;

    private String source; // e.g., "LinkedIn", "Indeed"

    private String status = "NEW"; // NEW, APPLIED, DRAFT

    private LocalDateTime postedAt;
    private LocalDateTime discoveredAt;

    @PrePersist
    protected void onCreate() {
        discoveredAt = LocalDateTime.now();
        if (postedAt == null) {
            postedAt = LocalDateTime.now();
        }
    }
}
