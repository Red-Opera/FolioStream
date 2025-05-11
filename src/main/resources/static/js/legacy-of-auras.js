document.addEventListener('DOMContentLoaded', function() 
{
    // 스크롤 시 back-to-top 버튼 표시/숨김
    const backToTopButton = document.querySelector('.back-to-top');
    
    window.addEventListener('scroll', function() 
    {
        if (window.scrollY > 300)
            backToTopButton.style.display = 'flex';

        else
            backToTopButton.style.display = 'none';
    });

    // 내비게이션 링크 스무스 스크롤
    document.querySelectorAll('a[href^="#"]').forEach(anchor => 
    {
        anchor.addEventListener('click', function (e) 
        {
            e.preventDefault();
            
            if (this.getAttribute('href') === '#') 
            {
                window.scrollTo
                ({
                    top: 0,
                    behavior: 'smooth'
                });
                return;
            }
            
            const target = document.querySelector(this.getAttribute('href'));

            if (target)
                target.scrollIntoView({ behavior: 'smooth' });
        });
    });

    // 스크롤 애니메이션 효과
    const animateOnScroll = function() 
    {
        const elements = document.querySelectorAll('.animate__animated:not(.animate__animated--triggered)');
        
        elements.forEach(element => 
        {
            const elementPosition = element.getBoundingClientRect().top;
            const windowHeight = window.innerHeight;
            
            if (elementPosition < windowHeight - 100) 
            {
                const animationClass = element.classList.contains('animate__fadeInUp') ? 'animate__fadeInUp' : 
                                     element.classList.contains('animate__fadeIn') ? 'animate__fadeIn' : '';
                
                if (animationClass)
                    element.classList.add(animationClass, 'animate__animated--triggered');
            }
        });
    };

    // 초기 로드 시 애니메이션 체크
    animateOnScroll();
    
    // 스크롤 시 애니메이션 체크
    window.addEventListener('scroll', animateOnScroll);
});