function applyTheme() {
    // 1. Check local storage
    const storedTheme = localStorage.getItem('hgs_theme');
    
    if (storedTheme === 'dark') {
        document.documentElement.classList.add('dark-mode');
        return;
    } else if (storedTheme === 'light') {
        document.documentElement.classList.remove('dark-mode');
        return;
    }
    
    // 2. If 'system', use media query
    if (storedTheme === 'system') {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.documentElement.classList.add('dark-mode');
        } else {
            document.documentElement.classList.remove('dark-mode');
        }
        return;
    }

    // 3. Fallback: time based (default if nothing is set)
    const hour = new Date().getHours();
    const isNight = hour >= 19 || hour < 7;
    
    if (isNight) {
        document.documentElement.classList.add('dark-mode');
    } else {
        document.documentElement.classList.remove('dark-mode');
    }
}

function setTheme(theme) {
    localStorage.setItem('hgs_theme', theme);
    applyTheme();
    updateThemeMenuSelection();
}

function updateThemeMenuSelection() {
    const currentTheme = localStorage.getItem('hgs_theme') || 'auto';
    document.querySelectorAll('.theme-option').forEach(btn => {
        if (btn.dataset.theme === currentTheme) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
}

// Ejecutar al cargar
document.addEventListener('DOMContentLoaded', () => {
    applyTheme();
    updateThemeMenuSelection();
    
    // Listen for system changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (localStorage.getItem('hgs_theme') === 'system') applyTheme();
    });
});

// User menu dropdown toggle
function toggleUserMenu() {
    document.getElementById('userDropdown').classList.toggle('show');
}

// Close dropdown if clicked outside
window.onclick = function(event) {
    if (!event.target.closest('.user-menu-container')) {
        var dropdowns = document.getElementsByClassName("user-dropdown");
        for (var i = 0; i < dropdowns.length; i++) {
            var openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('show')) {
                openDropdown.classList.remove('show');
            }
        }
    }
}
