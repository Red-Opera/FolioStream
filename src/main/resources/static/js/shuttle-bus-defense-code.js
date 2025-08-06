/* ==========================================================================
   shuttle-bus-defense Documentation JavaScript
   ========================================================================== */

// Global Variables
let currentTheme = 'light';
let searchTimeout;
let currentFilter = 'all';

// DOM Content Loaded Event
document.addEventListener('DOMContentLoaded', function() {
    initializeTheme();
    initializeNavigation();
    initializeHeroAnimations();
    initializeFeatures();
    initializeCodebaseSearch();
    initializeAPITabs();
    initializeScrollAnimations();
    initializeModals();
});

// Theme Management
function initializeTheme() {
    const savedTheme = localStorage.getItem('shuttle-bus-defense-docs-theme') || 'light';
    setTheme(savedTheme);
    
    const themeToggle = document.getElementById('themeToggle');
    themeToggle.addEventListener('click', toggleTheme);
}

function setTheme(theme) {
    currentTheme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    
    const themeIcon = document.querySelector('#themeToggle i');
    if (theme === 'dark') {
        themeIcon.className = 'fas fa-sun';
    } else {
        themeIcon.className = 'fas fa-moon';
    }
    
    // Save theme preference
    localStorage.setItem('shuttle-bus-defense-docs-theme', theme);
}

function toggleTheme() {
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
    
    // Add smooth transition effect
    document.body.style.transition = 'background-color 0.3s ease, color 0.3s ease';
    setTimeout(() => {
        document.body.style.transition = '';
    }, 300);
}

// Navigation
function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            const targetElement = document.querySelector(targetId);
            
            if (targetElement) {
                const navHeight = document.querySelector('.navbar').offsetHeight;
                const targetPosition = targetElement.offsetTop - navHeight - 20;
                
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // Active navigation highlighting
    window.addEventListener('scroll', updateActiveNavigation);
}

function updateActiveNavigation() {
    const sections = document.querySelectorAll('.section');
    const navLinks = document.querySelectorAll('.nav-link');
    const navHeight = document.querySelector('.navbar').offsetHeight;
    
    let current = '';
    
    sections.forEach(section => {
        const sectionTop = section.offsetTop - navHeight - 50;
        const sectionHeight = section.offsetHeight;
        
        if (window.scrollY >= sectionTop && window.scrollY < sectionTop + sectionHeight) {
            current = section.getAttribute('id');
        }
    });
    
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${current}`) {
            link.classList.add('active');
        }
    });
}

// Codebase Search and Filter
function initializeCodebaseSearch() {
    const searchInput = document.getElementById('codeSearch');
    const filterTags = document.querySelectorAll('.filter-tag');
    const codeCards = document.querySelectorAll('.code-file-card');
    let currentCategory = 'all';

    function filterCards() {
        const keyword = searchInput.value.trim().toLowerCase();
        codeCards.forEach(card => {
            const name = card.querySelector('.file-name').textContent.toLowerCase();
            const descSpan = card.querySelector('.file-description span');
            const desc = descSpan ? descSpan.textContent.toLowerCase() : '';
            const features = card.getAttribute('data-feature').split(',').map(f => f.trim().toLowerCase());
            const matchKeyword = name.includes(keyword) || desc.includes(keyword);
            const matchFeature = (currentCategory === 'all' || features.includes(currentCategory.toLowerCase()));
            if (matchKeyword && matchFeature) {
                card.classList.remove('hidden');
            } else {
                card.classList.add('hidden');
            }
        });
    }

    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(filterCards, 200);
    });

    filterTags.forEach(tag => {
        tag.addEventListener('click', function() {
            filterTags.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentCategory = this.getAttribute('data-feature');
            filterCards();
        });
    });
}

// .hidden 클래스를 완전히 숨김 처리
(function() {
    const style = document.createElement('style');
    style.innerHTML = '.hidden { display: none !important; }';
    document.head.appendChild(style);
})();

// shuttle-bus-defense 코드 보기 함수 - 서버 엔드포인트 사용
function viewCode(fileName) {
    // fileName으로 code-file-card에서 path를 찾음
    const cards = document.querySelectorAll('.code-file-card');
    let filePath = null;
    cards.forEach(card => {
        const nameElem = card.querySelector('.file-name');
        if (nameElem && nameElem.textContent === fileName) {
            const pathElem = card.querySelector('.file-path');
            if (pathElem) filePath = pathElem.textContent + fileName;
        }
    });
    
    if (filePath) {
        // 서버 엔드포인트를 통해 코드 로드
        const serverUrl = '/shuttle-bus-defense/code/raw?path=' + encodeURIComponent(filePath);
        
        // 코드 뷰어 모달에 코드 로드
        loadCodeInModal(fileName, serverUrl);
    } else {
        alert('파일 경로를 찾을 수 없습니다.');
    }
}
window.viewCode = viewCode;

// 코드를 모달에 로드하는 함수
async function loadCodeInModal(fileName, url) {
    const modal = document.getElementById('codeViewerModal');
    const title = document.getElementById('codeViewerTitle');
    const codeContent = document.getElementById('fullCodeContent');
    
    if (!modal || !title || !codeContent) {
        // 모달이 없으면 새 탭에서 열기
        window.open(url, '_blank');
        return;
    }
    
    title.textContent = fileName;
    codeContent.textContent = '코드를 로딩 중...';
    modal.style.display = 'flex';
    modal.classList.add('active');
    
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const code = await response.text();
        codeContent.innerHTML = highlightCSharpCode(code);
    } catch (error) {
        console.error('코드 로딩 실패:', error);
        codeContent.textContent = '코드를 불러올 수 없습니다. 오류: ' + error.message;
    }
}

function showDependencies(fileName) {
    alert(fileName + '의 의존성 정보를 표시합니다.');
}
window.showDependencies = showDependencies;

function closeCodeViewer() {
    const modal = document.getElementById('codeViewerModal');
    if (modal) {
        modal.style.display = 'none';
        modal.classList.remove('active');
    }
}
window.closeCodeViewer = closeCodeViewer;

// 코드 복사 기능
function copyFullCode() {
    const codeElement = document.getElementById('fullCodeContent');
    if (codeElement) {
        const textToCopy = codeElement.textContent || codeElement.innerText || '';
        
        if (navigator.clipboard) {
            navigator.clipboard.writeText(textToCopy).then(() => {
                showCopyFeedback('복사되었습니다!');
            }).catch(err => {
                console.error('복사 실패:', err);
                fallbackCopyCode(textToCopy);
            });
        } else {
            fallbackCopyCode(textToCopy);
        }
    }
}
window.copyFullCode = copyFullCode;

function downloadCode() {
    const codeElement = document.getElementById('fullCodeContent');
    const title = document.getElementById('codeViewerTitle');
    
    if (codeElement && title) {
        const code = codeElement.textContent || codeElement.innerText || '';
        const fileName = title.textContent;
        
        const blob = new Blob([code], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        
        showCopyFeedback('다운로드 시작!');
    }
}
window.downloadCode = downloadCode;

function fallbackCopyCode(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
        document.execCommand('copy');
        showCopyFeedback('복사되었습니다!');
    } catch (err) {
        console.error('복사 실패:', err);
        showCopyFeedback('복사 실패');
    }
    
    document.body.removeChild(textArea);
}

function showCopyFeedback(message) {
    // 피드백 메시지를 잠시 보여줌
    const feedback = document.createElement('div');
    feedback.textContent = message;
    feedback.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #4CAF50;
        color: white;
        padding: 10px 20px;
        border-radius: 4px;
        z-index: 10000;
        font-size: 14px;
    `;
    document.body.appendChild(feedback);
    
    setTimeout(() => {
        document.body.removeChild(feedback);
    }, 2000);
}

function initializeHeroAnimations() {
    // Hero 섹션 애니메이션이 필요하다면 여기에 구현
    // 현재는 빈 함수로 에러만 방지
}

function initializeFeatures() {
    // 기능 상세 데이터 로드
    loadFeatureDetails();
    
    // 기능 카드 클릭 시 모달 열기
    const featureCards = document.querySelectorAll('.feature-card');
    const modal = document.getElementById('featureModal');
    const modalClose = modal ? modal.querySelector('.modal-close') : null;
    
    featureCards.forEach(card => {
        card.addEventListener('click', function() {
            const featureId = this.getAttribute('data-feature');
            showFeatureModal(featureId);
        });
    });
    
    if (modalClose) {
        modalClose.addEventListener('click', function() {
            modal.classList.remove('active');
        });
    }
    
    // 모달 외부 클릭 시 닫기
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                modal.classList.remove('active');
            }
        });
    }
    
    // 기능 페이지네이션 초기화
    initializeFeaturePagination();
}

let featureDetailsData = {};

async function loadFeatureDetails() {
    try {
        const response = await fetch('/data/shuttle-bus-defense-feature-details.json');
        featureDetailsData = await response.json();
    } catch (error) {
        console.error('Failed to load feature details:', error);
    }
}

function showFeatureModal(featureId) {
    const modal = document.getElementById('featureModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalCode = document.getElementById('modalCode');
    const modalSteps = document.getElementById('modalSteps');
    const codeFilename = document.querySelector('.code-filename');
    
    if (!modal || !featureDetailsData[featureId]) return;
    
    const featureData = featureDetailsData[featureId];
    
    // 모달 제목 설정
    modalTitle.textContent = featureData.title;
    
    // 파일명 설정
    if (codeFilename && featureData.relatedFiles && featureData.relatedFiles.length > 0) {
        const fileName = featureData.relatedFiles[0].split('/').pop();
        codeFilename.textContent = fileName;
    }
    
    // 코드 예제 설정
    if (modalCode) {
        const codeContent = featureData.codeExample || '// 코드 예제를 준비 중입니다...';
        modalCode.innerHTML = highlightCSharpCode(codeContent);
    }
    
    // 구현 단계 설정
    if (modalSteps && featureData.implementationSteps) {
        modalSteps.innerHTML = '';
        featureData.implementationSteps.forEach(step => {
            const li = document.createElement('li');
            li.textContent = step;
            modalSteps.appendChild(li);
        });
    }
    
    // 모달 표시
    modal.classList.add('active');
}

// 코드 복사 기능
function copyCode() {
    const codeElement = document.getElementById('modalCode');
    if (codeElement) {
        // HTML 태그를 제거하고 순수 텍스트만 추출
        const textToCopy = codeElement.textContent || codeElement.innerText || '';
        
        if (navigator.clipboard) {
            navigator.clipboard.writeText(textToCopy).then(() => {
                showCopyFeedback();
            }).catch(err => {
                console.error('Failed to copy code:', err);
                fallbackCopyCode(textToCopy);
            });
        } else {
            fallbackCopyCode(textToCopy);
        }
    }
}

function fallbackCopyCode(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
        document.execCommand('copy');
        showCopyFeedback();
    } catch (err) {
        console.error('Fallback copy failed:', err);
    }
    
    document.body.removeChild(textArea);
}

function showCopyFeedback() {
    const copyBtn = document.querySelector('.copy-btn');
    if (copyBtn) {
        const originalHTML = copyBtn.innerHTML;
        copyBtn.innerHTML = '<i class="fas fa-check"></i>';
        copyBtn.style.color = '#4CAF50';
        
        setTimeout(() => {
            copyBtn.innerHTML = originalHTML;
            copyBtn.style.color = '';
        }, 2000);
    }
}

// 전역으로 등록
window.copyCode = copyCode;

function initializeFeaturePagination() {
    const featureCards = document.querySelectorAll('.feature-card');
    const pagination = document.getElementById('featurePagination');
    const pageSize = 6; // 한 페이지에 6개씩 표시
    let currentPage = 1;
    let filteredIndexes = Array.from(featureCards.keys());
    
    function showFeaturePage(page) {
        currentPage = page;
        featureCards.forEach((card, i) => {
            card.style.display = 'none';
        });
        
        for (let idx = (page-1)*pageSize; idx < Math.min(filteredIndexes.length, page*pageSize); idx++) {
            featureCards[filteredIndexes[idx]].style.display = 'block';
        }
        renderFeaturePagination();
    }
    
    function renderFeaturePagination() {
        if (!pagination) return;
        
        pagination.innerHTML = '';
        const totalPages = Math.ceil(filteredIndexes.length / pageSize);
        
        for (let i = 1; i <= totalPages; i++) {
            const btn = document.createElement('button');
            btn.textContent = i;
            btn.className = 'pagination-btn' + (i === currentPage ? ' active' : '');
            btn.setAttribute('type', 'button');
            if (i === currentPage) btn.setAttribute('aria-current', 'page');
            
            btn.addEventListener('click', function() {
                showFeaturePage(i);
            });
            
            pagination.appendChild(btn);
        }
    }
    
    // 초기 페이지 표시
    if (featureCards.length > 0) {
        showFeaturePage(1);
    }
}

function initializeAPITabs() {
    // API 탭 클릭 시 내용 전환
    const tabs = document.querySelectorAll('.api-tab');
    const tabContents = document.querySelectorAll('.api-tab-content');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            const target = this.getAttribute('data-tab');
            const content = document.getElementById(target);
            if (content) content.classList.add('active');
        });
    });
}

function initializeScrollAnimations() {
    // 섹션이 보일 때 페이드인 애니메이션 적용
    const sections = document.querySelectorAll('.section');
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in');
            }
        });
    }, { threshold: 0.2 });
    sections.forEach(section => {
        observer.observe(section);
    });
    // CSS 추가
    const style = document.createElement('style');
    style.innerHTML = '.fade-in { animation: fadeInUp 1s ease; }';
    document.head.appendChild(style);
}

function initializeModals() {
    // 코드 뷰어 모달 닫기 버튼 이벤트 연결
    const codeModal = document.getElementById('codeViewerModal');
    const closeBtns = codeModal ? codeModal.querySelectorAll('.modal-close') : [];
    closeBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            codeModal.style.display = 'none';
            codeModal.classList.remove('active');
        });
    });
    
    // 모달 외부 클릭 시 닫기
    if (codeModal) {
        codeModal.addEventListener('click', function(e) {
            if (e.target === codeModal) {
                codeModal.style.display = 'none';
                codeModal.classList.remove('active');
            }
        });
    }
}

// C# 코드 하이라이팅 함수
function highlightCSharpCode(code) {
    // C# 키워드
    const keywords = [
        'public', 'private', 'protected', 'internal', 'static', 'virtual', 'override', 'abstract',
        'class', 'interface', 'struct', 'enum', 'namespace', 'using', 'void', 'int', 'float', 
        'double', 'bool', 'string', 'char', 'byte', 'short', 'long', 'decimal', 'object',
        'var', 'const', 'readonly', 'if', 'else', 'for', 'while', 'do', 'foreach', 'in',
        'switch', 'case', 'default', 'break', 'continue', 'return', 'try', 'catch', 'finally',
        'throw', 'new', 'this', 'base', 'null', 'true', 'false', 'typeof', 'sizeof', 'is', 'as',
        'ref', 'out', 'params', 'get', 'set', 'value', 'yield', 'async', 'await', 'lock'
    ];
    
    // Unity 및 C# 타입들
    const types = [
        'Transform', 'GameObject', 'MonoBehaviour', 'Component', 'Vector3', 'Vector2', 'Quaternion',
        'Rigidbody', 'Collider', 'AudioSource', 'AudioClip', 'Camera', 'Light', 'Renderer',
        'Material', 'Texture', 'Sprite', 'Animation', 'Animator', 'Canvas', 'Button', 'Text',
        'Image', 'Slider', 'RectTransform', 'EventSystem', 'CharacterController', 'ParticleSystem',
        'IEnumerator', 'Coroutine', 'List', 'Dictionary', 'Array', 'ScriptableObject'
    ];
    
    let highlightedCode = code;
    
    // HTML 이스케이프
    highlightedCode = highlightedCode
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    
    // 주석 하이라이팅 (가장 높은 우선순위로 먼저 처리)
    highlightedCode = highlightedCode.replace(/\/\/.*$/gm, '<span class="comment">$&</span>');
    highlightedCode = highlightedCode.replace(/\/\*[\s\S]*?\*\//g, '<span class="comment">$&</span>');
    
    // 문자열 하이라이팅 - 주석 제외
    highlightedCode = highlightedCode.replace(/&quot;([^&]*)&quot;(?![^<]*<\/span>)/g, '<span class="string">&quot;$1&quot;</span>');
    highlightedCode = highlightedCode.replace(/'([^'\\]*(\\.[^'\\]*)*)'(?![^<]*<\/span>)/g, '<span class="string">\'$1\'</span>');
    
    // 숫자 하이라이팅 - 주석 제외
    highlightedCode = highlightedCode.replace(/\b(\d+\.?\d*f?)\b(?![^<]*<\/span>)/g, '<span class="number">$1</span>');
    
    // 키워드 하이라이팅 - 주석 제외
    keywords.forEach(keyword => {
        const regex = new RegExp(`\\b${keyword}\\b(?![^<]*>)(?![^<]*<\/span>)`, 'g');
        highlightedCode = highlightedCode.replace(regex, `<span class="keyword">${keyword}</span>`);
    });
    
    // 타입 하이라이팅 - 주석 제외
    types.forEach(type => {
        const regex = new RegExp(`\\b${type}\\b(?![^<]*>)(?![^<]*<\/span>)`, 'g');
        highlightedCode = highlightedCode.replace(regex, `<span class="type">${type}</span>`);
    });
    
    // struct 키워드 뒤에 오는 단어를 클래스와 같은 색깔로 처리 - 주석 제외
    highlightedCode = highlightedCode.replace(/(<span class="keyword">struct<\/span>)\s+([a-zA-Z_][a-zA-Z0-9_]*)(?![^<]*<\/span>)/g, '$1 <span class="type">$2</span>');
    
    // 메서드 호출 하이라이팅 - 주석 제외
    highlightedCode = highlightedCode.replace(/\b([a-zA-Z_][a-zA-Z0-9_]*)\s*(?=\()(?![^<]*<\/span>)/g, '<span class="method">$1</span>');
    
    // 속성 하이라이팅 - 주석 제외
    highlightedCode = highlightedCode.replace(/\.([a-zA-Z_][a-zA-Z0-9_]*)\b(?![^<]*>)(?![^<]*<\/span>)/g, '.<span class="property">$1</span>');
    
    return highlightedCode;
}

// 숫자 애니메이션 (스크립트 파일)
function animateStatNumbers() {
    document.querySelectorAll('.stat-number[data-target]').forEach(function(el) {
        const target = parseInt(el.getAttribute('data-target'), 10);
        const duration = 2400;
        let start = null;
        function animate(ts) {
            if (!start) start = ts;
            const elapsed = ts - start;
            const progress = Math.min(elapsed / duration, 1);
            // sin 곡선 기반 증가
            const eased = Math.sin(progress * (Math.PI / 2));
            const value = Math.floor(eased * target);
            el.textContent = value;
            if (progress < 1) {
                requestAnimationFrame(animate);
            } else {
                el.textContent = target;
            }
        }
        el.textContent = '0';
        requestAnimationFrame(animate);
    });
}

document.addEventListener('DOMContentLoaded', function() {
    animateStatNumbers();
    const cards = document.querySelectorAll('.code-file-card');
    const pagination = document.getElementById('pagination');
    const pageSize = 10;
    let currentPage = 1;
    let filteredIndexes = Array.from(cards.keys());
    
    function showPage(page) {
        currentPage = page;
        cards.forEach((card, i) => {
            card.style.display = 'none';
        });
        for (let idx = (page-1)*pageSize; idx < Math.min(filteredIndexes.length, page*pageSize); idx++) {
            cards[filteredIndexes[idx]].style.display = '';
        }
        renderPagination();
    }
    
    function renderPagination() {
        if (!pagination) return;
        pagination.innerHTML = '';
        const totalPages = Math.ceil(filteredIndexes.length / pageSize);
        for (let i = 1; i <= totalPages; i++) {
            const btn = document.createElement('button');
            btn.textContent = i;
            btn.className = 'pagination-btn' + (i === currentPage ? ' active' : '');
            btn.setAttribute('type', 'button');
            if (i === currentPage) btn.setAttribute('aria-current', 'page');
            btn.addEventListener('click', function() {
                showPage(i);
            });
            pagination.appendChild(btn);
        }
    }
    
    function filterCards() {
        const keyword = document.getElementById('codeSearch').value.trim().toLowerCase();
        const activeTag = document.querySelector('.filter-tag.active');
        const currentCategory = activeTag ? activeTag.getAttribute('data-feature') : 'all';
        filteredIndexes = [];
        cards.forEach((card, i) => {
            const name = card.querySelector('.file-name').textContent.toLowerCase();
            const descSpan = card.querySelector('.file-description span');
            const desc = descSpan ? descSpan.textContent.toLowerCase() : '';
            const features = card.getAttribute('data-feature').split(',').map(f => f.trim().toLowerCase());
            const matchKeyword = name.includes(keyword) || desc.includes(keyword);
            const matchFeature = (currentCategory === 'all' || features.includes(currentCategory.toLowerCase()));
            if (matchKeyword && matchFeature) {
                filteredIndexes.push(i);
            }
        });
        showPage(1);
    }
    
    document.getElementById('codeSearch').addEventListener('input', function() {
        filterCards();
    });
    
    document.querySelectorAll('.filter-tag').forEach(tag => {
        tag.addEventListener('click', function() {
            document.querySelectorAll('.filter-tag').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            filterCards();
        });
    });
    
    filterCards();
});