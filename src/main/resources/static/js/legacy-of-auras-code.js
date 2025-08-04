/* ==========================================================================
   Unity Project Documentation JavaScript
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
    const savedTheme = localStorage.getItem('unity-docs-theme') || 'light';
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
    localStorage.setItem('unity-docs-theme', theme);
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

// 코드 보기/의존성 버튼 동작 함수 추가 및 글로벌 등록
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
    const modal = document.getElementById('codeViewerModal');
    const title = document.getElementById('codeViewerTitle');
    const codeContent = document.getElementById('fullCodeContent');
    title.textContent = fileName + ' 코드 뷰어';
    codeContent.textContent = '// 코드를 불러오는 중...';
    modal.style.display = 'block';
    if (filePath) {
        fetch('/code/raw?path=' + encodeURIComponent(filePath))
            .then(res => res.text())
            .then(code => {
                codeContent.innerHTML = highlightCSharpCode(code);
            })
            .catch(() => {
                codeContent.innerHTML = highlightCSharpCode('// 코드를 불러올 수 없습니다.');
            });
    } else {
        codeContent.textContent = '// 파일 경로를 찾을 수 없습니다.';
    }
}
window.viewCode = viewCode;

function showDependencies(fileName) {
    alert(fileName + '의 의존성 정보를 표시합니다.');
}
window.showDependencies = showDependencies;

function closeCodeViewer() {
    document.getElementById('codeViewerModal').style.display = 'none';
}
window.closeCodeViewer = closeCodeViewer;

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
        const response = await fetch('/data/legacy-of-auras-feature-details.json');
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
    
    // C# 타입들
    const types = [
        'Transform', 'GameObject', 'MonoBehaviour', 'Component', 'Vector3', 'Vector2', 'Quaternion',
        'Rigidbody', 'Collider', 'AudioSource', 'AudioClip', 'Camera', 'Light', 'Renderer',
        'Material', 'Texture', 'Sprite', 'Animation', 'Animator', 'Canvas', 'Button', 'Text',
        'Image', 'Slider', 'RectTransform', 'EventSystem', 'PlayerState', 'MonsterState',
        'WeaponType', 'QuestTitleContent', 'CharacterController', 'ParticleSystem',
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
    
    // 문자열 하이라이팅 (따옴표 안의 내용)
    highlightedCode = highlightedCode.replace(/&quot;([^&]*)&quot;/g, '<span class="string">&quot;$1&quot;</span>');
    highlightedCode = highlightedCode.replace(/'([^'\\]*(\\.[^'\\]*)*)'/g, '<span class="string">\'$1\'</span>');
    
    // 제네릭 타입 하이라이팅 (< > 안의 내용을 클래스로 처리)
    highlightedCode = highlightedCode.replace(/&lt;([^&]+)&gt;/g, '&lt;<span class="type">$1</span>&gt;');
    
    // 주석 하이라이팅
    highlightedCode = highlightedCode.replace(/\/\/.*$/gm, '<span class="comment">$&</span>');
    highlightedCode = highlightedCode.replace(/\/\*[\s\S]*?\*\//g, '<span class="comment">$&</span>');
    
    // 숫자 하이라이팅
    highlightedCode = highlightedCode.replace(/\b(\d+\.?\d*f?)\b/g, '<span class="number">$1</span>');
    
    // 키워드 하이라이팅
    keywords.forEach(keyword => {
        const regex = new RegExp(`\\b${keyword}\\b(?![^<]*>)`, 'g');
        highlightedCode = highlightedCode.replace(regex, `<span class="keyword">${keyword}</span>`);
    });
    
    // 타입 하이라이팅
    types.forEach(type => {
        const regex = new RegExp(`\\b${type}\\b(?![^<]*>)`, 'g');
        highlightedCode = highlightedCode.replace(regex, `<span class="type">${type}</span>`);
    });
    
    // 메서드 호출 하이라이팅 (단어 뒤에 괄호가 오는 경우)
    highlightedCode = highlightedCode.replace(/\b([a-zA-Z_][a-zA-Z0-9_]*)\s*(?=\()/g, '<span class="method">$1</span>');
    
    // 속성 하이라이팅 (점 뒤에 오는 단어)
    highlightedCode = highlightedCode.replace(/\.([a-zA-Z_][a-zA-Z0-9_]*)\b(?![^<]*>)/g, '.<span class="property">$1</span>');
    
    // 어트리뷰트 하이라이팅 ([...] 형태) - 대괄호는 기본 색상, 속성은 타입 색상
    highlightedCode = highlightedCode.replace(/\[([^\]]+)\]/g, '[<span class="type">$1</span>]');
    
    // 클래스명 하이라이팅 (class 키워드 뒤에 오는 단어)
    highlightedCode = highlightedCode.replace(/(<span class="keyword">class<\/span>)\s+([a-zA-Z_][a-zA-Z0-9_]*)/g, '$1 <span class="class-name">$2</span>');

    // 대문자로 시작하는 단어 뒤에 점이 오는 경우 클래스로 판별 (예: Transform.position, GameObject.FindWithTag)
    highlightedCode = highlightedCode.replace(/\b([A-Z][a-zA-Z0-9_]*)\s*(?=\.)/g, '<span class="type">$1</span>');
    
    // private/public/protected 등 뒤에 오는 대문자로 시작하는 단어를 클래스로 판별
    highlightedCode = highlightedCode.replace(/\b(public|private|protected|internal)\s+([A-Z][a-zA-Z0-9_]*)\b/g, '$1 <span class="type">$2</span>');
    
    // 변수 하이라이팅 (Visual Studio 스타일) - 먼저 처리
    highlightedCode = highlightVariables(highlightedCode);
    
    return highlightedCode;
}

// Visual Studio 스타일 변수 하이라이팅
function highlightVariables(code) {
    const lines = code.split('\n');
    let result = [];
    let insideMethod = false;
    let braceCount = 0;
    let classVariables = new Set();
    let parameters = new Set();
    
    // 키워드와 타입 배열 (로컬 복사)
    const keywords = [
        'public', 'private', 'protected', 'internal', 'static', 'virtual', 'override', 'abstract',
        'class', 'interface', 'struct', 'enum', 'namespace', 'using', 'void', 'int', 'float', 
        'double', 'bool', 'string', 'char', 'byte', 'short', 'long', 'decimal', 'object',
        'var', 'const', 'readonly', 'if', 'else', 'for', 'while', 'do', 'foreach', 'in',
        'switch', 'case', 'default', 'break', 'continue', 'return', 'try', 'catch', 'finally',
        'throw', 'new', 'this', 'base', 'null', 'true', 'false', 'typeof', 'sizeof', 'is', 'as',
        'ref', 'out', 'params', 'get', 'set', 'value', 'yield', 'async', 'await', 'lock'
    ];
    
    const types = [
        'Transform', 'GameObject', 'MonoBehaviour', 'Component', 'Vector3', 'Vector2', 'Quaternion',
        'Rigidbody', 'Collider', 'AudioSource', 'AudioClip', 'Camera', 'Light', 'Renderer',
        'Material', 'Texture', 'Sprite', 'Animation', 'Animator', 'Canvas', 'Button', 'Text',
        'Image', 'Slider', 'RectTransform', 'EventSystem', 'PlayerState', 'MonsterState',
        'WeaponType', 'QuestTitleContent', 'CharacterController', 'ParticleSystem',
        'IEnumerator', 'Coroutine', 'List', 'Dictionary', 'Array', 'ScriptableObject'
    ];
    
    // 1단계: 클래스 변수 및 매개변수 식별
    for (let line of lines) {
        // 클래스 레벨에서 변수 선언 찾기 (public, private 등으로 시작)
        const classVarMatch = line.match(/^\s*(?:public|private|protected|internal)?\s*(?:static)?\s*(?:readonly)?\s*(?:const)?\s*(?:<span class="type">)?(\w+)(?:<\/span>)?\s+([a-zA-Z_][a-zA-Z0-9_]*)/);
        if (classVarMatch && !insideMethod) {
            classVariables.add(classVarMatch[2]);
        }
        
        // 메서드 매개변수 찾기
        const methodMatch = line.match(/(?:void|int|bool|string|float|double|IEnumerator|[A-Z]\w*)\s+\w+\s*\(([^)]*)\)/);
        if (methodMatch && methodMatch[1]) {
            const paramString = methodMatch[1];
            // 매개변수 파싱: type paramName, type paramName2, ...
            const paramMatches = paramString.match(/\b([a-zA-Z_][a-zA-Z0-9_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)/g);
            if (paramMatches) {
                paramMatches.forEach(param => {
                    const parts = param.trim().split(/\s+/);
                    if (parts.length >= 2) {
                        parameters.add(parts[parts.length - 1]); // 마지막 부분이 매개변수 이름
                    }
                });
            }
        }
        
        // 메서드 시작/종료 추적
        if (line.includes('void ') || line.includes('int ') || line.includes('bool ') || line.includes('string ') || 
            line.includes('float ') || line.includes('double ') || line.includes('IEnumerator ')) {
            if (line.includes('{')) {
                insideMethod = true;
                braceCount = 1;
            } else if (line.includes('(')) {
                insideMethod = true;
                braceCount = 0;
            }
        }
        
        if (insideMethod) {
            const openBraces = (line.match(/{/g) || []).length;
            const closeBraces = (line.match(/}/g) || []).length;
            braceCount += openBraces - closeBraces;
            
            if (braceCount <= 0) {
                insideMethod = false;
                braceCount = 0;
                parameters.clear(); // 메서드가 끝나면 매개변수 목록 초기화
            }
        }
    }
    
    // 2단계: 변수에 색상 적용
    insideMethod = false;
    braceCount = 0;
    let currentMethodParams = new Set();
    
    for (let line of lines) {
        // 메서드 추적 및 매개변수 재식별
        const methodMatch = line.match(/(?:void|int|bool|string|float|double|IEnumerator|[A-Z]\w*)\s+\w+\s*\(([^)]*)\)/);
        if (methodMatch && methodMatch[1]) {
            currentMethodParams.clear();
            const paramString = methodMatch[1];
            // 매개변수 파싱: type paramName, type paramName2, ...
            const paramMatches = paramString.match(/\b([a-zA-Z_][a-zA-Z0-9_]*)\s+([a-zA-Z_][a-zA-Z0-9_]*)/g);
            if (paramMatches) {
                paramMatches.forEach(param => {
                    const parts = param.trim().split(/\s+/);
                    if (parts.length >= 2) {
                        currentMethodParams.add(parts[parts.length - 1]);
                    }
                });
            }
        }
        
        if (line.includes('void ') || line.includes('int ') || line.includes('bool ') || line.includes('string ') || 
            line.includes('float ') || line.includes('double ') || line.includes('IEnumerator ')) {
            if (line.includes('{')) {
                insideMethod = true;
                braceCount = 1;
            } else if (line.includes('(')) {
                insideMethod = true;
                braceCount = 0;
            }
        }
        
        if (insideMethod) {
            const openBraces = (line.match(/{/g) || []).length;
            const closeBraces = (line.match(/}/g) || []).length;
            braceCount += openBraces - closeBraces;
            
            if (braceCount <= 0) {
                insideMethod = false;
                braceCount = 0;
                currentMethodParams.clear();
            }
        }
        
        // 매개변수 색상 적용 (먼저 처리)
        for (let param of currentMethodParams) {
            const regex = new RegExp(`\\b${param}\\b(?![^<]*>)`, 'g');
            line = line.replace(regex, `<span class="parameter">${param}</span>`);
        }
        
        // 변수 색상 적용
        if (insideMethod) {
            // 메서드 내부 - 로컬 변수는 연파란색
            // 타입 선언된 변수 찾기
            line = line.replace(/(?:<span class="type">)?\b(?:int|float|double|bool|string|var|Vector3|Vector2|Transform|GameObject)\b(?:<\/span>)?\s+([a-zA-Z_][a-zA-Z0-9_]*)/g, function(match, varName) {
                if (!classVariables.has(varName) && !currentMethodParams.has(varName)) {
                    return match.replace(varName, `<span class="local-variable">${varName}</span>`);
                }
                return match;
            });
            
            // 할당문에서 변수 찾기 (이미 선언된 로컬 변수)
            line = line.replace(/\b([a-zA-Z_][a-zA-Z0-9_]*)\s*=(?![^<]*>)/g, function(match, varName) {
                if (!classVariables.has(varName) && !currentMethodParams.has(varName) && 
                    !keywords.includes(varName) && !types.includes(varName) && 
                    !line.includes(`<span class="parameter">${varName}</span>`)) {
                    return `<span class="local-variable">${varName}</span> =`;
                }
                return match;
            });
            
            // 일반적인 변수 사용 (할당이 아닌 경우)
            line = line.replace(/\b([a-zA-Z_][a-zA-Z0-9_]*)\b(?![^<]*>)(?!\s*[=\(])/g, function(match, varName) {
                if (!classVariables.has(varName) && !currentMethodParams.has(varName) && 
                    !keywords.includes(varName) && !types.includes(varName) && 
                    !line.includes(`<span class="parameter">${varName}</span>`) &&
                    !line.includes(`<span class="local-variable">${varName}</span>`) &&
                    !line.includes(`<span class="method">${varName}</span>`) &&
                    !line.includes(`<span class="property">${varName}</span>`)) {
                    // 지역 변수로 추정되는 경우에만 색상 적용
                    const prevChar = line.charAt(line.indexOf(varName) - 1);
                    const nextChar = line.charAt(line.indexOf(varName) + varName.length);
                    if (prevChar !== '.' && nextChar !== '(') {
                        return `<span class="local-variable">${varName}</span>`;
                    }
                }
                return match;
            });
        } else {
            // 클래스 레벨 - 멤버 변수는 기본 색상
            for (let classVar of classVariables) {
                const regex = new RegExp(`(?<!\\.)\\b${classVar}\\b(?![^<]*>)(?!\\s*=)`, 'g');
                line = line.replace(regex, `<span class="class-variable">${classVar}</span>`);
            }
        }
        
        result.push(line);
    }
    
    return result.join('\n');
}

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
        });
    });
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