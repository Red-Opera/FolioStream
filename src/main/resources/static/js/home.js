document.addEventListener('DOMContentLoaded', function() {
    // URL 해시가 있으면 해당 요소로 스크롤
    const hash = window.location.hash;
    if (hash) {
        const element = document.querySelector(hash);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth' });
        }
    }
    
    // 방문자 수 증가
    fetch('/api/visitors/increment', {
        method: 'GET'
    })
    .then(() => {
        // 방문자 통계 가져오기
        return fetch('/api/visitors/stats');
    })
    .then(response => response.json())
    .then(data => {
        // 방문자 수 업데이트
        document.querySelector('.today-visitors').textContent = `오늘 방문자: ${data.dailyVisitors}`;
        document.querySelector('.total-visitors').textContent = `전체 방문자: ${data.totalVisitors}`;
    })
    .catch(error => console.error('Error:', error));
});