/**
 * PeoplePulse HR Mobile Responsive Sidebar Drawer & Backdrop Controller
 * Handles smooth drawer slide-in, background dimming, escape-to-close,
 * and automatic closure on link selection on mobile devices.
 */
function toggleSidebar() {
    const sidebar = document.querySelector('.peoplepulse-sidebar');
    if (!sidebar) return;

    let backdrop = document.querySelector('.sidebar-backdrop');
    if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.className = 'sidebar-backdrop';
        backdrop.setAttribute('aria-hidden', 'true');
        backdrop.addEventListener('click', toggleSidebar);
        document.body.appendChild(backdrop);
    }

    const isOpen = sidebar.classList.toggle('show');
    backdrop.classList.toggle('show', isOpen);
    document.body.style.overflow = isOpen ? 'hidden' : '';
}

// Close drawer when pressing Escape
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const sidebar = document.querySelector('.peoplepulse-sidebar.show');
        if (sidebar) toggleSidebar();
    }
});

// Close drawer automatically on navigation item click on small screens & scroll active item into view
document.addEventListener('DOMContentLoaded', () => {
    const navLinks = document.querySelectorAll('.peoplepulse-sidebar .peoplepulse-nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth < 992) {
                const sidebar = document.querySelector('.peoplepulse-sidebar.show');
                if (sidebar) toggleSidebar();
            }
        });
    });

    // Ensure active link is scrolled into view smoothly in sidebar nav
    const activeLink = document.querySelector('.peoplepulse-sidebar .peoplepulse-nav-link.active');
    if (activeLink) {
        activeLink.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
});
