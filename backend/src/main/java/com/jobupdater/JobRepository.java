package com.jobupdater;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    boolean existsByUrl(String url);

    boolean existsByTitleAndCompany(String title, String company);
}
