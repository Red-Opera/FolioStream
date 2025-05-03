package com.springboot.data;

import com.springboot.model.VisitorStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;

@Service
public class VisitorService {
    private static final Logger logger = LoggerFactory.getLogger(VisitorService.class);
    private final VisitorRepository visitorRepository;

    @Autowired
    public VisitorService(VisitorRepository visitorRepository) {
        this.visitorRepository = visitorRepository;
    }

    @Transactional(readOnly = true)
    public VisitorStats getVisitorStats() {
        logger.info("Getting visitor stats");
        LocalDate today = LocalDate.now();
        VisitorStats todayStats = visitorRepository.findByDate(today)
                .orElse(new VisitorStats(today, 0, 0));

        // 전체 방문자 수는 이미 데이터베이스에 저장되어 있으므로 계산할 필요 없음
        logger.info("Returning visitor stats: {}", todayStats);
        return todayStats;
    }

    @Transactional
    public void incrementDailyVisitors() {
        logger.info("Incrementing daily visitors");
        LocalDate today = LocalDate.now();
        VisitorStats todayStats = visitorRepository.findByDate(today)
                .orElse(new VisitorStats(today, 0, 0));

        // 전체 방문자 수 계산
        List<VisitorStats> allStats = visitorRepository.findAll();
        int totalVisitors = allStats.stream()
                .mapToInt(VisitorStats::getDailyVisitors)
                .sum();

        todayStats.setDailyVisitors(todayStats.getDailyVisitors() + 1);
        todayStats.setTotalVisitors(totalVisitors + 1); // 전체 방문자 수 업데이트
        visitorRepository.save(todayStats);
        logger.info("Updated visitor stats: {}", todayStats);
    }
} 