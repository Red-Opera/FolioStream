package com.springboot.data;

import com.springboot.model.VisitorStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<VisitorStats, Long> {
    Optional<VisitorStats> findByDate(LocalDate date);
} 