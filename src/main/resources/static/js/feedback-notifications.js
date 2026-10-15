(() => {
    const badges = [...document.querySelectorAll('[data-feedback-unread-badge]')];
    if (!badges.length) return;

    const toast = document.querySelector('[data-feedback-toast]');
    const initialBadge = badges.find((badge) => !badge.classList.contains('is-hidden'));
    let lastCount = initialBadge ? Number(initialBadge.textContent) || 0 : 0;
    let toastTimer;

    const updateCount = (count) => {
        badges.forEach((badge) => {
            badge.textContent = String(count);
            badge.classList.toggle('is-hidden', count < 1);
            badge.setAttribute('aria-label', `${count} unread feedback messages`);
        });
        if (count > lastCount && toast) {
            toast.textContent = count === 1 ? 'You have new feedback.' : `You have ${count} unread feedback messages.`;
            toast.hidden = false;
            window.clearTimeout(toastTimer);
            toastTimer = window.setTimeout(() => { toast.hidden = true; }, 6000);
        }
        lastCount = count;
    };

    const refresh = async () => {
        try {
            const response = await fetch('/feedback/unread-count', {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' },
                cache: 'no-store'
            });
            if (!response.ok) return;
            const result = await response.json();
            if (Number.isFinite(Number(result.count))) updateCount(Number(result.count));
        } catch (_) {
            // Keep the last displayed count if a temporary connection error occurs.
        }
    };

    refresh();
    window.setInterval(refresh, 25000);
})();
