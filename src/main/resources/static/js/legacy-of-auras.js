// Back to Top Button
const backToTop = document.querySelector('.back-to-top');

window.addEventListener('scroll', () => 
{
    if (window.pageYOffset > 100) {
        backToTop.classList.add('visible');
    } else {
        backToTop.classList.remove('visible');
    }
});

// Smooth Scrolling
document.querySelectorAll('a[href^="#"]').forEach(anchor => 
{
    anchor.addEventListener('click', function (e) 
	{
        e.preventDefault();
		
        document.querySelector(this.getAttribute('href')).scrollIntoView(
		{
            behavior: 'smooth'
        });
    });
}); 