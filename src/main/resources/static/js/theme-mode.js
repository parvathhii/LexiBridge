(() => {
    const storageKey = 'lexibridge-color-mode';
    const themeButtons = document.querySelectorAll('[data-theme-toggle]');
    if (!themeButtons.length) return;
    let nightMode = false;

    try {
        nightMode = localStorage.getItem(storageKey) === 'night';
    } catch (_) {
        // Keep the toggle usable if browser storage is unavailable.
    }

    const applyMode = () => {
        document.body.classList.toggle('theme-dark', nightMode);
        themeButtons.forEach((button) => {
            button.setAttribute('aria-pressed', String(nightMode));
            button.setAttribute('aria-label', nightMode ? 'Switch to day mode' : 'Switch to night mode');
            const icon = button.querySelector('[data-theme-icon]');
            const label = button.querySelector('[data-theme-label]');
            if (icon) icon.textContent = nightMode ? '☀' : '☾';
            if (label) label.textContent = nightMode ? 'Day mode' : 'Night mode';
        });
    };

    applyMode();
    themeButtons.forEach((button) => {
        button.addEventListener('click', () => {
            nightMode = !nightMode;
            try {
                localStorage.setItem(storageKey, nightMode ? 'night' : 'day');
            } catch (_) {
                // The current page still changes even if the preference cannot be saved.
            }
            applyMode();
        });
    });
})();
