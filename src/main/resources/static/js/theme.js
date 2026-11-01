/**
 * PeoplePulse HR Dark / Light Mode Theme Controller
 * Provides immediate theme application (zero-flicker),
 * localStorage persistence, system preference auto-detection,
 * and synchronized toggle icon state across all templates.
 */

// 1. Immediately apply saved or system theme before DOM renders (prevents white flash)
(function() {
    const savedTheme = localStorage.getItem('PeoplePulse HR-theme');
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = savedTheme ? savedTheme : (prefersDark ? 'dark' : 'light');
    document.documentElement.setAttribute('data-bs-theme', initialTheme);
})();

// 2. Toggle between Light and Dark mode
function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    
    document.documentElement.setAttribute('data-bs-theme', newTheme);
    localStorage.setItem('PeoplePulse HR-theme', newTheme);
    updateThemeIcons(newTheme);
}

// 3. Update theme icon states (Sun vs Moon)
function updateThemeIcons(theme) {
    const isDark = theme === 'dark';
    
    document.querySelectorAll('.theme-icon-dark').forEach(el => {
        if (isDark) {
            el.classList.add('d-none');
        } else {
            el.classList.remove('d-none');
        }
    });
    
    document.querySelectorAll('.theme-icon-light').forEach(el => {
        if (isDark) {
            el.classList.remove('d-none');
        } else {
            el.classList.add('d-none');
        }
    });

    document.querySelectorAll('.theme-toggle-btn').forEach(btn => {
        btn.setAttribute('title', isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode');
        btn.setAttribute('aria-label', isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode');
    });
}

// 4. Initialize icons after DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
    updateThemeIcons(currentTheme);
});
