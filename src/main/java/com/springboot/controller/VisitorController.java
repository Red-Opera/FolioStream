package com.springboot.controller;

import com.springboot.data.VisitorService;
import com.springboot.model.VisitorStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.DatatypeConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController 
{
    private final VisitorService visitorService;
    
    @Autowired
    public VisitorController(VisitorService visitorService)
    {
        this.visitorService = visitorService;
    }

    @GetMapping("/stats")
    public VisitorStats getVisitorStats() 
    {
        return visitorService.getVisitorStats();
    }

    @GetMapping("/increment")
    public ResponseEntity<String> incrementVisitor(HttpServletRequest request) 
    {
        // 방문자 식별자 생성 (IP + 유저 에이전트)
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String visitorIdentifier = generateVisitorIdentifier(ipAddress, userAgent);

        // 중복 방문자인지 확인
        boolean isNewVisitor = visitorService.isNewVisitor(visitorIdentifier);
        
        if (isNewVisitor) 
        {
            // 새로운 방문자인 경우 카운트 증가
            visitorService.incrementDailyVisitors(visitorIdentifier);
            
            return ResponseEntity.ok("새 방문자로 카운트되었습니다.");
        } 

        // 이미 오늘 방문한 사용자인 경우
        else
            return ResponseEntity.ok("오늘 이미 카운트된 방문자입니다.");
    }
    
    private String generateVisitorIdentifier(String ip, String userAgent)
    {
        // IP와 UserAgent의 해시 생성
        try 
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((ip + userAgent).getBytes());
            byte[] digest = md.digest();
            
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } 
        
        catch (NoSuchAlgorithmException e) {
            return ip + userAgent;
        }
    }
}
