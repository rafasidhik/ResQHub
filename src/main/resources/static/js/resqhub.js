/**
 * ResQHub — Client Interactive Utilities
 */
document.addEventListener("DOMContentLoaded", function () {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Client-side quick filter for tables with data-table-filter
    const filterInputs = document.querySelectorAll('[data-table-filter]');
    filterInputs.forEach(function (input) {
        input.addEventListener('keyup', function () {
            const targetSelector = this.getAttribute('data-table-filter');
            const table = document.querySelector(targetSelector);
            if (!table) return;
            const query = this.value.toLowerCase();
            const rows = table.querySelectorAll('tbody tr');
            rows.forEach(function (row) {
                const text = row.textContent.toLowerCase();
                row.style.display = text.indexOf(query) > -1 ? '' : 'none';
            });
        });
    });

    // Confirmation on destructive actions
    const confirmButtons = document.querySelectorAll('[data-confirm]');
    confirmButtons.forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            const message = this.getAttribute('data-confirm') || 'Are you sure you want to proceed?';
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Collapsible sidebar
    const body = document.body;
    const sidebarToggle = document.getElementById('sidebarToggle');
    const backdrop = document.querySelector('.sidebar-backdrop');
    const sidebarClose = document.querySelector('.sidebar-close');
    const sidebarOpen = document.getElementById('sidebarOpen');
    const isDesktop = () => window.matchMedia('(min-width: 992px)').matches;

    if (sidebarToggle) {
        // Restore collapsed state on desktop only
        if (isDesktop() && localStorage.getItem('resqhub.sidebar') === 'collapsed') {
            body.classList.add('sidebar-toggled');
        }

        sidebarToggle.addEventListener('click', function () {
            body.classList.toggle('sidebar-toggled');
            if (isDesktop()) {
                localStorage.setItem(
                    'resqhub.sidebar',
                    body.classList.contains('sidebar-toggled') ? 'collapsed' : 'open'
                );
            }
        });
    }

    function closeSidebar() {
        body.classList.remove('sidebar-toggled');
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeSidebar);
    }

    if (sidebarOpen) {
        sidebarOpen.addEventListener('click', function () {
            body.classList.add('sidebar-toggled');
        });
    }
    if (sidebarClose) {
        sidebarClose.addEventListener('click', closeSidebar);
    }
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeSidebar();
        }
    });

    // Floating label tooltip when the sidebar is collapsed to icons
    const sidebar = document.querySelector('.sidebar-resq');
    if (sidebar) {
        const tip = document.createElement('div');
        tip.className = 'sidebar-tip';
        document.body.appendChild(tip);

        sidebar.querySelectorAll('.sidebar-menu .nav-link').forEach(function (link) {
            const label = link.querySelector('.nav-label');
            if (!label) return;

            link.addEventListener('mouseenter', function () {
                if (!isDesktop() || !body.classList.contains('sidebar-toggled')) return;
                const rect = link.getBoundingClientRect();
                tip.textContent = label.textContent.trim();
                tip.style.top = (rect.top + rect.height / 2) + 'px';
                tip.style.left = (rect.right + 10) + 'px';
                tip.classList.add('show');
            });

            link.addEventListener('mouseleave', function () {
                tip.classList.remove('show');
            });
        });

        sidebar.addEventListener('scroll', function () {
            tip.classList.remove('show');
        });
    }
});
