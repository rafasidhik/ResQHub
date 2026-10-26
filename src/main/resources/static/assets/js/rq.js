/* ResQHub front-end helpers (no framework). Everything is progressive:
   forms still submit normally if this file fails to load. */
(function () {
    'use strict';

    // --- re-open a modal and refill it after a server-side validation error
    document.addEventListener('DOMContentLoaded', function () {
        var holder = document.getElementById('rq-state');
        if (holder) {
            try {
                var state = JSON.parse(holder.textContent || '{}');
                var modalEl = state.modal ? document.getElementById(state.modal) : null;
                if (modalEl && state.values) {
                    Object.keys(state.values).forEach(function (name) {
                        if (name === 'action') { return; }
                        var field = modalEl.querySelector('[name="' + name + '"]');
                        if (!field) { return; }
                        if (field.type === 'checkbox') { field.checked = state.values[name] === 'on'; }
                        else { field.value = state.values[name]; }
                    });
                    bootstrap.Modal.getOrCreateInstance(modalEl).show();
                }
            } catch (e) { /* ignore malformed state */ }
        }
    });

    // --- "Edit" buttons: copy data-* attributes into the modal's fields
    document.addEventListener('click', function (ev) {
        var btn = ev.target.closest('[data-rq-edit]');
        if (!btn) { return; }
        var modalEl = document.getElementById(btn.getAttribute('data-rq-edit'));
        if (!modalEl) { return; }
        Object.keys(btn.dataset).forEach(function (key) {
            if (key === 'rqEdit' || key === 'bsToggle' || key === 'bsTarget') { return; }
            var field = modalEl.querySelector('[name="' + key + '"]');
            if (!field) { return; }
            if (field.type === 'checkbox') { field.checked = btn.dataset[key] === 'true'; }
            else { field.value = btn.dataset[key]; }
        });
        var title = btn.getAttribute('data-rq-title');
        var t = modalEl.querySelector('.modal-title');
        if (title && t) { t.textContent = title; }
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    });

    // --- confirm dialogs for destructive actions (Bootstrap modal, not alert())
    var pendingForm = null;
    document.addEventListener('submit', function (ev) {
        var form = ev.target;
        var msg = form.getAttribute('data-rq-confirm');
        if (!msg || form.dataset.rqConfirmed === '1') { return; }
        ev.preventDefault();
        var modalEl = document.getElementById('rqConfirmModal');
        if (!modalEl) { return; }
        pendingForm = form;
        modalEl.querySelector('.rq-confirm-text').textContent = msg;
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    });
    document.addEventListener('click', function (ev) {
        if (ev.target.closest('#rqConfirmYes') && pendingForm) {
            pendingForm.dataset.rqConfirmed = '1';
            pendingForm.submit();
        }
    });

    // --- client-side table search
    document.addEventListener('input', function (ev) {
        var box = ev.target.closest('[data-rq-search]');
        if (!box) { return; }
        var table = document.getElementById(box.getAttribute('data-rq-search'));
        if (!table) { return; }
        var q = box.value.trim().toLowerCase();
        var visible = 0;
        table.querySelectorAll('tbody tr[data-row]').forEach(function (tr) {
            var show = !q || tr.textContent.toLowerCase().indexOf(q) !== -1;
            tr.style.display = show ? '' : 'none';
            if (show) { visible++; }
        });
        var none = table.parentElement.querySelector('.rq-no-match');
        if (none) { none.style.display = visible ? 'none' : ''; }
    });

    // --- prevent double submits
    document.addEventListener('submit', function (ev) {
        var form = ev.target;
        if (ev.defaultPrevented) { return; }
        if (form.dataset.rqBusy === '1') { ev.preventDefault(); return; }
        form.dataset.rqBusy = '1';
        form.querySelectorAll('button[type=submit]').forEach(function (b) {
            setTimeout(function () { b.disabled = true; }, 0);
        });
    });
    // restore buttons when returning via the back button
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form').forEach(function (f) { f.dataset.rqBusy = ''; });
        document.querySelectorAll('button[type=submit]').forEach(function (b) { b.disabled = false; });
    });
})();

// auto-submit filter dropdowns
document.addEventListener('change', function (ev) {
    var sel = ev.target.closest('[data-rq-autosubmit]');
    if (sel && sel.form) { sel.form.submit(); }
});
