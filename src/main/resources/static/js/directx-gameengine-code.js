// DirectX GameEngine Code 페이지 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const topBar = document.querySelector('.top-bar');
    function getHeaderOffset() {
        // topBar height plus a small gap
        return (topBar ? topBar.offsetHeight : 70) + 12;
    }
    // 네비게이션 항목(서브 포함) 클릭 시 해당 섹션으로 스크롤
    const navLinks = Array.from(document.querySelectorAll('[data-target]'));

    // Build a map of targetId -> element for efficiency
    const targetsMap = {};
    navLinks.forEach(link => {
        const tid = link.getAttribute('data-target');
        if (tid && !targetsMap[tid]) {
            const el = document.getElementById(tid);
            if (el) targetsMap[tid] = el;
        }
    });

    // Smooth scroll and active toggle for links
    navLinks.forEach((link) => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            // 모든 네비게이션 항목에서 active 클래스 제거
            navLinks.forEach(nav => nav.classList.remove('active'));

            // 클릭된 항목에 active 클래스 추가
            this.classList.add('active');

            // 해당 타겟으로 스크롤 (상단바 높이 고려)
            const targetId = this.getAttribute('data-target');
            const targetElement = targetsMap[targetId] || document.getElementById(targetId);

            if (targetElement) {
                // Compute current header offset dynamically so mobile/desktop are handled
                const headerOffset = getHeaderOffset();
                const offsetTop = targetElement.getBoundingClientRect().top + window.pageYOffset - headerOffset;
                window.scrollTo({ top: offsetTop, behavior: 'smooth' });
            }
        });
    });

    // 모든 그룹은 항상 열려 있으므로 토글 동작은 제거

    // 스크롤 시 현재 섹션에 해당하는 네비게이션 항목 활성화
    function onScroll() {
        const scrollPos = window.pageYOffset || document.documentElement.scrollTop;
        const headerOffset = getHeaderOffset();
        let currentId = null;

        // 모든 섹션 요소를 수집 (anchor-section, feature-container, 그리고 id를 가진 내부 div들)
        const allSections = [];
        document.querySelectorAll('.anchor-section[id], .feature-container[id], .feature-container [id], .feature-text[id]').forEach(section => {
            allSections.push({
                id: section.id,
                top: section.offsetTop - headerOffset,
                bottom: section.offsetTop + section.offsetHeight - headerOffset
            });
        });

        // 현재 스크롤 위치가 속한 섹션 찾기 (화면 중앙 기준)
        const viewportCenter = scrollPos + headerOffset + 100;
        
        for (let i = allSections.length - 1; i >= 0; i--) {
            const section = allSections[i];
            if (scrollPos + headerOffset >= section.top - 50) {
                currentId = section.id;
                break;
            }
        }

        // 모든 네비게이션 항목의 active 클래스 제거
        navLinks.forEach(link => {
            link.classList.remove('active');
        });

        // 현재 섹션에 해당하는 네비게이션 항목 활성화
        if (currentId) {
            // 현재 ID에 해당하는 모든 네비게이션 항목 찾기
            const matchingLinks = navLinks.filter(link => 
                link.getAttribute('data-target') === currentId
            );

            matchingLinks.forEach(link => {
                link.classList.add('active');
                
                // 부모 항목도 활성화 (서브아이템인 경우)
                if (link.classList.contains('nav-subitem') || link.classList.contains('nav-subsubitem')) {
                    const navGroup = link.closest('.nav-group');
                    if (navGroup) {
                        const parentNav = navGroup.querySelector('.nav-parent');
                        if (parentNav && !parentNav.classList.contains('active')) {
                            // 부모가 같은 섹션을 가리키지 않는 경우에만 활성화
                            const parentTarget = parentNav.getAttribute('data-target');
                            if (parentTarget !== currentId) {
                                parentNav.classList.add('active');
                            }
                        }
                    }
                }
            });
        }
    }

    window.addEventListener('scroll', onScroll);
    // 초기 한 번 실행
    onScroll();

    // 초기 로드 시 첫 번째 네비게이션 항목 활성화 제거 (onScroll이 자동으로 처리)
});
