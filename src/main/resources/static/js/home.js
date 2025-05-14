/**
 * FolioStream - 메인 자바스크립트
 * 
 * 이 파일은 FolioStream 웹사이트의 주요 상호작용 기능을 담당합니다.
 * 테마 전환, 방문자 통계, 모바일 메뉴 등의 기능이 포함되어 있습니다.
 * 
 * @author FolioStream Team
 * @version 1.0
 * @since 2025-05-14
 */

/**
 * DOM이 완전히 로드된 후 초기화 함수 실행
 */
document.addEventListener('DOMContentLoaded', function() {
    // 테마 스위치 초기화
    initThemeSwitch();
    
    // URL 해시가 있으면 해당 요소로 스크롤
    initHashNavigation();
    
    // 방문자 통계 로드
    loadVisitorStats();
});

/**
 * 테마 스위치 초기화 및 이벤트 핸들링
 * 사용자의 테마 설정을 로컬 스토리지에서 불러오고 테마 전환 이벤트를 설정
 */
function initThemeSwitch() {
    const toggleSwitch = document.getElementById('checkbox');
    
    if (!toggleSwitch) {
        console.error('테마 스위치 요소를 찾을 수 없습니다.');
        return;
    }
    
    // 로컬 스토리지에서 테마 설정 가져오기
    const currentTheme = localStorage.getItem('theme');
    
    // 저장된 테마 적용 (체크 상태 반대로 설정)
    if (currentTheme === 'dark-mode') {
        document.body.classList.add('dark-mode');
        toggleSwitch.checked = false; // 다크 모드일 때 체크 해제 (반대로)
    } else if (currentTheme === 'light-mode' || !currentTheme) {
        document.body.classList.remove('dark-mode');
        toggleSwitch.checked = true; // 라이트 모드일 때 체크 (반대로)
    }
    
    // 테마 변경 이벤트 설정 (체크 상태와 테마 적용을 반대로)
    toggleSwitch.addEventListener('change', function(e) {
        if (!e.target.checked) { // 체크 해제되면 다크 모드로
            document.body.classList.add('dark-mode');
            localStorage.setItem('theme', 'dark-mode');
            console.log('다크 모드로 전환됨');
        } else { // 체크되면 라이트 모드로
            document.body.classList.remove('dark-mode');
            localStorage.setItem('theme', 'light-mode');
            console.log('라이트 모드로 전환됨');
        }
    });
    
    console.log('테마 스위치 초기화 완료');
}

/**
 * URL 해시 기반 페이지 내 네비게이션
 * URL에 #id가 있으면 해당 요소로 스크롤
 */
function initHashNavigation() {
    const hash = window.location.hash;
    if (hash) {
        const element = document.querySelector(hash);
        if (element) {
            // 부드러운 스크롤로 요소로 이동
            element.scrollIntoView({ behavior: 'smooth' });
        }
    }
}

/**
 * 방문자 통계 데이터 로드 및 표시
 * API를 호출하여 방문자 수를 가져오고 UI에 표시
 */
function loadVisitorStats() {
    // 방문자 수 증가 API 호출
    fetch('/api/visitors/increment', {
        method: 'GET'
    })
    .then(() => {
        // 방문자 통계 가져오기
        return fetch('/api/visitors/stats');
    })
    .then(response => response.json())
    .then(data => {
        // 방문자 수 UI 업데이트
        const todayElement = document.querySelector('.today-visitors span');
        const totalElement = document.querySelector('.total-visitors span');
        
        if (todayElement && totalElement) {
            todayElement.textContent = `오늘 ${data.dailyVisitors}`;
            totalElement.textContent = `전체 ${data.totalVisitors}`;
        } else {
            console.warn('방문자 통계 표시 요소를 찾을 수 없습니다.');
        }
    })
    .catch(error => console.error('방문자 통계 로드 오류:', error));
}

/**
 * 모바일 메뉴 토글 기능 초기화
 * 모바일 화면에서 메뉴 버튼 클릭 시 사이드바 표시/숨김
 */
const menuToggle = document.querySelector('.menu-toggle');
if (menuToggle) {
    menuToggle.addEventListener('click', function() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.toggle('show');
            
            // 토글 버튼 아이콘 상태 변경
            const spans = this.querySelectorAll('span');
            if (spans.length > 0) {
                if (sidebar.classList.contains('show')) {
                    // X 모양으로 변경
                    spans[0].style.transform = 'rotate(45deg) translate(5px, 5px)';
                    spans[1].style.opacity = '0';
                    spans[2].style.transform = 'rotate(-45deg) translate(5px, -5px)';
                } else {
                    // 원래 모양으로 복원
                    spans[0].style.transform = 'none';
                    spans[1].style.opacity = '1';
                    spans[2].style.transform = 'none';
                }
            }
        }
    });
}