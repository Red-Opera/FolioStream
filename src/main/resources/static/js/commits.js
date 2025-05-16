document.addEventListener('DOMContentLoaded', function() 
{
    // 모든 타임라인 카드에 클릭 이벤트 추가
    const cards = document.querySelectorAll('.timeline-card');
    
    // 레포지토리별 색상 맵 생성
    const repoColorMap = {};
    
    // 더 뚜렷하고 다양한 색상 팔레트
    const colorPalette = [
        '#e41a1c', // 빨강
        '#377eb8', // 파랑
        '#4daf4a', // 녹색
        '#984ea3', // 보라
        '#ff7f00', // 주황
        '#a65628', // 갈색
        '#f781bf', // 분홍
        '#00796b', // 청록
        '#ffd600', // 노랑
        '#7e57c2', // 연보라
        '#d32f2f', // 진한 빨강
        '#0288d1', // 하늘색
        '#388e3c', // 진한 녹색
        '#5d4037', // 초콜릿
        '#00acc1', // 밝은 청록
        '#ec407a'  // 연분홍
    ];
    
    // 레포지토리 이름에서 색상 생성 함수
    function generateColorFromRepo(repoName) 
    {
        if (repoColorMap[repoName])
            return repoColorMap[repoName];
        
        // 이미 사용된 모든 색상을 다 사용했다면 재사용
        const colorIndex = Object.keys(repoColorMap).length % colorPalette.length;
        repoColorMap[repoName] = colorPalette[colorIndex];

        return repoColorMap[repoName];
    }
    
    // 카드에 색상 적용
    cards.forEach(card => 
    {
        const repo = card.getAttribute('data-repo');
        if (repo) 
        {
            // 레포지토리 이름 부분만 색상 적용
            const repoInfo = card.querySelector('.repo-info');

            if (repoInfo) 
            {
                const textColor = generateColorFromRepo(repo);
                
                // 텍스트 색상만 적용 (배경색은 변경하지 않음)
                const repoText = repoInfo.querySelector('span');
                if (repoText) 
                {
                    repoText.style.color = textColor;
                    repoText.style.fontWeight = 'bold';
                }
                
                // 아이콘 색상도 동일하게 적용
                const icon = repoInfo.querySelector('i');

                if (icon)
                    icon.style.color = textColor;
            }
        }
        
        card.addEventListener('click', function() 
        {
            const repo = this.getAttribute('data-repo');
            const commitSha = this.getAttribute('data-commit-sha');
            
            if (repo) 
            {
                if (commitSha && commitSha.trim() !== '') 
                {
                    // 커밋 SHA가 있으면 해당 커밋의 트리로 이동
                    const url = 'https://github.com/' + repo + '/tree/' + commitSha;

                    console.log('Opening URL:', url);
                    window.open(url, '_blank');1``
                } 
                
                // SHA가 없으면 기존처럼 레포지토리로 이동
                else 
                    window.open('https://github.com/' + repo, '_blank');
            }
        });
    });

    // 커스텀 툴팁 초기화
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));

    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) 
    {
        // 데이터 속성에서 추가/삭제 값을 가져와 천 단위 구분자 적용
        var additions = tooltipTriggerEl.getAttribute('data-additions');
        var deletions = tooltipTriggerEl.getAttribute('data-deletions');
        
        if (additions && deletions) {
            // 숫자 포맷팅 (천 단위 구분자 추가)
            var formattedAdditions = parseInt(additions).toLocaleString('ko-KR');
            var formattedDeletions = parseInt(deletions).toLocaleString('ko-KR');
            
            // 이미 포맷된 값을 data 속성에 저장 (나중에 사용할 수 있도록)
            tooltipTriggerEl.setAttribute('data-formatted-additions', formattedAdditions);
            tooltipTriggerEl.setAttribute('data-formatted-deletions', formattedDeletions);
        }
        
        // 툴팁 옵션 확장 - 더 큰 지연 시간 설정 (사용자가 읽을 시간 확보)
        return new bootstrap.Tooltip(tooltipTriggerEl, { 
            html: true, 
            placement: 'top', 
            trigger: 'hover focus',
            delay: { show: 200, hide: 300 } // 툴팁이 닫히는 시간을 약간 늘림
        });
    });

    // 레포지토리 범례 추가
    createRepoLegend();

    // 레포지토리 색상 범례 생성 함수
    function createRepoLegend() 
    {
        const container = document.querySelector('.container');

        if (!container) 
            return;

        const legend = document.createElement('div');
        legend.className = 'repo-legend';
        legend.innerHTML = `
            <div class="legend-header">
                <i class="fas fa-palette"></i>
                <h6 class="legend-title">레포지토리별 색상</h6>
            </div>
            <div class="legend-items"></div>
        `;

        const legendItems = legend.querySelector('.legend-items');

        // 현재 페이지에 표시된 모든 레포지토리에 대한 범례 아이템 생성
        Object.keys(repoColorMap).forEach(repo => 
        {
            const color = repoColorMap[repo];

            // 레포 이름에서 사용자/레포 부분 분리
            const [owner, repoName] = repo.split('/');

            const item = document.createElement('div');
            item.className = 'legend-item';
            item.setAttribute('data-repo', repo);
            item.setAttribute('title', repo); // 전체 이름을 툴팁으로 표시

            const legendText = document.createElement('div');
            legendText.className = 'legend-text';
            legendText.setAttribute('data-full-repo', repo);

            item.innerHTML = `
                <div class="legend-icon">
                    <i class="fas fa-code-branch" style="color: ${color};"></i>
                </div>
            `;

            legendText.innerHTML = `
                <span class="repo-owner">${owner} /</span>
                <span class="repo-name" style="color: ${color};">${repoName}</span>
            `;

            item.appendChild(legendText);

            // 클릭 이벤트 추가 - 해당 레포지토리로 이동
            item.addEventListener('click', () => { window.open('https://github.com/' + repo, '_blank'); });

            legendItems.appendChild(item);
        });

        // 범례가 비어있지 않은 경우에만 추가
        if (Object.keys(repoColorMap).length > 0) 
        {
            container.appendChild(legend);

            // 범례 아이템 너비 자동 조정
            const repoCount = Object.keys(repoColorMap).length;

            // 레포지토리가 적은 경우 더 넓게 표시
            if (repoCount <= 3)
                legendItems.style.gridTemplateColumns = `repeat(${repoCount}, 1fr)`;
        }
    }
});