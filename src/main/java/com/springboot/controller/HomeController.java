package com.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.springboot.model.VisitorCount;
import com.springboot.model.Project;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;

@Controller
public class HomeController 
{

    private Map<String, List<Project>> createPortfolioData() 
    {
        Map<String, List<Project>> portfolios = new HashMap<>();
        
        // Unity 프로젝트
        List<Project> unityProjects = Arrays.asList(
            new Project("Legacy of Auras", 
                "3D RPG 게임으로, 플레이어의 성장과 깊이 있는 스토리텔링에 중점을 둔 프로젝트입니다.<br><br>" +
                "개발 환경: Unity 3D<br>" +
                "개발 형태: 개인 프로젝트<br>" +
                "담당 파트: Client, Server<br><br>" +
                "캐릭터 성장 시스템과 몰입도 높은 스토리 중심의 탐험을 즐길 수 있습니다."),

            new Project("K Project", 
                "캐릭터 성장 요소가 가미된 슈팅 게임입니다.<br><br>" +
                "개발 환경: Unity 2D<br>" +
                "개발 형태: 팀 프로젝트<br>" +
                "담당 파트: Client, Server<br><br>" +
                "다양한 무기와 스킬을 활용한 전투 시스템, 레벨 디자인, 그리고 서버 연동을 통한 멀티플레이 기능을 구현했습니다."),

            new Project("셔틀버스 디펜스 게임", 
                "특정 거점을 방어하며 적의 공격을 막는 캐주얼 디펜스 게임입니다.<br><br>" +
                "개발 환경: Unity 3D<br>" +
                "개발 형태: 팀 프로젝트<br>" +
                "담당 파트: Client<br><br>" +
                "다양한 방어 시설과 적 웨이브 시스템을 구현했으며, 전략적인 게임플레이를 제공합니다.")
        );
        
        // Unreal 프로젝트
        List<Project> unrealProjects = Arrays.asList(
            new Project("Era of Dreams : 1950s Simulation", 
                "1950년대 미국 배경의 시뮬레이션 게임입니다.<br><br>" +
                "개발 환경: Unreal 5<br>" +
                "개발 형태: 개인 프로젝트<br>" +
                "담당 파트: Client<br><br>" +
                "해당 시대의 문화와 사회상을 세밀하게 재현했으며, 플레이어가 당시의 생활과 주요 사건들을 직접 체험할 수 있도록 구현했습니다. 사실적인 그래픽과 시대 고증을 통해 몰입감 있는 게임 환경을 제공합니다."),

            new Project("Unreal Project 2", 
                "Unreal Engine으로 제작된 프로젝트입니다.<br><br>" +
                "고품질 그래픽과 물리 효과를 구현했습니다."),

            new Project("Unreal Project 3", 
                "Unreal Engine으로 제작된 프로젝트입니다.<br><br>" +
                "고품질 그래픽과 물리 효과를 구현했습니다.")
        );
        
        // Graphic 프로젝트
        List<Project> graphicProjects = Arrays.asList(
            new Project("DirectX GameEngine", 
                "DirectX 11을 활용한 3D 그래픽스 엔진 개발 프로젝트입니다.<br><br>" +
                "개발 환경: DirectX 11 3D<br>" +
                "개발 형태: 개인 프로젝트<br>" +
                "담당 파트: Client<br><br>" +
                "기본적인 렌더링 파이프라인부터 고급 그래픽 기능까지 직접 구현하여 게임 엔진의 핵심 기능을 이해하고 개발하는 것을 목표로 했습니다."),

            new Project("D2DGame", 
                "DirectX 11 2D를 이용해 개발한 슈팅 게임입니다.<br><br>" +
                "개발 환경: DirectX 11 2D<br>" +
                "개발 형태: 개인 프로젝트<br>" +
                "담당 파트: Client<br><br>" +
                "씬 관리 시스템, 다양한 적 캐릭터와의 전투, 충돌 처리 등 게임의 기본적인 요소들을 직접 구현했습니다."),

            new Project("Sokoban", 
                "클래식한 소코반(창고지기) 퍼즐 게임입니다.<br><br>" +
                "개발 환경: Window API C++<br>" +
                "개발 형태: 개인 프로젝트<br>" +
                "담당 파트: Client<br><br>" +
                "Windows API와 C++을 활용하여 개발했으며, 퍼즐 게임의 규칙을 충실히 구현하고 레벨 디자인과 게임 로직을 깔끔하게 설계했습니다.")
        );
        
        portfolios.put("unity", unityProjects);
        portfolios.put("unreal", unrealProjects);
        portfolios.put("graphic", graphicProjects);
        
        return portfolios;
    }

    @GetMapping("/")
    public String home(Model model) 
    {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", null);
        
        return "home";
    }

    @GetMapping("/category/{category}")
    public String category(@PathVariable("category") String category, Model model) 
    {
        model.addAttribute("message", "포트폴리오 갤러리");
        model.addAttribute("portfolios", createPortfolioData());
        model.addAttribute("selectedCategory", category);
        
        return "home";
    }

    @GetMapping("/support")
    public String support(Model model) 
    {
        model.addAttribute("message", "Support");
        model.addAttribute("visitorCount", new VisitorCount(0, 0)); // 임시로 0으로 설정
        return "support";
    }

    @GetMapping("/legacy-of-auras")
    public String legacyOfAuras(Model model) {
        String projectName = "Legacy of Auras";
        String galleryPath = "/images/Gallery/Legacy of Auras/";
        String realPath = "src/main/resources/static/images/Gallery/Legacy of Auras/";

        File folder = new File(realPath);
        String[] files = folder.list((dir, name) -> name.matches("\\d+\\.png"));
        List<String> galleryImages = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                int numA = Integer.parseInt(a.replace(".png", ""));
                int numB = Integer.parseInt(b.replace(".png", ""));
                return Integer.compare(numA, numB);
            });
            for (String file : files) {
                galleryImages.add(galleryPath + file);
            }
        }
        model.addAttribute("galleryImages", galleryImages);
        return "legacy-of-auras";
    }
}