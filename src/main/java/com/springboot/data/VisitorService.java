package com.springboot.data;

import com.springboot.model.UniqueVisitor;
import com.springboot.model.VisitorStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;

@Service
public class VisitorService 
{
    private static final Logger logger = LoggerFactory.getLogger(VisitorService.class);
    private final VisitorRepository visitorRepository;
    private final UniqueVisitorRepository uniqueVisitorRepository;

    @Autowired
    public VisitorService(VisitorRepository visitorRepository, UniqueVisitorRepository uniqueVisitorRepository) 
    {
    	this.visitorRepository = visitorRepository;
        this.uniqueVisitorRepository = uniqueVisitorRepository;
    }
    
    @Transactional(readOnly = true)
    public boolean isNewVisitor(String visitorIdentifier)
    {
        LocalDate today = LocalDate.now();
        // 같은 날짜, 같은 식별자를 가진 방문자가 있는지 확인
        return !uniqueVisitorRepository.existsByDateAndVisitorIdentifier(today, visitorIdentifier);
    }


    @Transactional(readOnly = true)
    public VisitorStats getVisitorStats() 
    {
    	logger.info("Getting visitor stats");
    	
        LocalDate today = LocalDate.now();
        VisitorStats todayStats = visitorRepository.findByDate(today)
                .orElse(new VisitorStats(today, 0, 0));

        // 전체 방문자 수는 이미 데이터베이스에 저장되어 있으므로 계산할 필요 없음
        logger.info("Returning visitor stats: {}", todayStats);
        
        return todayStats;
    }

    @Transactional
    public void incrementDailyVisitors(String visitorIdentifier) 
    {
    	 logger.info("Processing visitor with identifier: {}", visitorIdentifier);
         LocalDate today = LocalDate.now();

         // 같은 사용자가 오늘 이미 방문했는지 확인
         boolean isNewVisitor = !uniqueVisitorRepository.existsByDateAndVisitorIdentifier(today, visitorIdentifier);

         if (isNewVisitor) 
         {
             // 새로운 방문자는 UniqueVisitor 테이블에 저장
             UniqueVisitor uniqueVisitor = new UniqueVisitor(today, visitorIdentifier);
             uniqueVisitorRepository.save(uniqueVisitor);

             // 방문자 통계 업데이트
             VisitorStats todayStats = visitorRepository.findByDate(today)
                     .orElse(new VisitorStats(today, 0, 0));

             todayStats.setDailyVisitors(todayStats.getDailyVisitors() + 1);

             // 전체 방문자 수 계산
             List<VisitorStats> allStats = visitorRepository.findAll();
             int totalVisitors = allStats.stream()
                     .mapToInt(VisitorStats::getDailyVisitors)
                     .sum();

             todayStats.setTotalVisitors(totalVisitors + 1);
             visitorRepository.save(todayStats);
             logger.info("Incremented visitor count for new visitor: {}", todayStats);
         } 
         
         else
             logger.info("Visitor already counted today, skipping increment");
     }
}