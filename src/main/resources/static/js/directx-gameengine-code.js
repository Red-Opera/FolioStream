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
        let currentId = null;

        // Iterate over targetsMap (only unique ids from nav)
        Object.keys(targetsMap).forEach(id => {
            const el = targetsMap[id];
            const headerOffset = getHeaderOffset();
            const top = el.offsetTop - headerOffset - 8; // small extra gap
            const bottom = top + el.offsetHeight;
            if (scrollPos >= top && scrollPos < bottom) {
                currentId = id;
            }
        });

        // If none found, check feature containers (fallback)
        if (!currentId) {
            const featureContainers = document.querySelectorAll('.feature-container[id]');
            featureContainers.forEach(container => {
                const headerOffset = getHeaderOffset();
                const top = container.offsetTop - headerOffset - 8;
                const bottom = top + container.offsetHeight;
                if (scrollPos >= top && scrollPos < bottom) currentId = container.id;
            });
        }

        // Update active states on all nav links
        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('data-target') === currentId) {
                link.classList.add('active');
            }
        });
    }

    window.addEventListener('scroll', onScroll);
    // 초기 한 번 실행
    onScroll();

    // 초기 로드 시 첫 번째 네비게이션 항목 활성화
    if (navLinks.length > 0) {
        navLinks[0].classList.add('active');
    }
});
