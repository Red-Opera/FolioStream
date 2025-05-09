package com.springboot.data;

import com.springboot.model.UniqueVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface UniqueVisitorRepository extends JpaRepository<UniqueVisitor, Long> 
{
    boolean existsByDateAndVisitorIdentifier(LocalDate date, String visitorIdentifier);
}
