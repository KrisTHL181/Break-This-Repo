function renderProgressList(containerId, rows) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    if (!rows || rows.length === 0) {
        container.innerHTML = `<div class="progress-failed">${i18n.lookUp('no_task')}</div>`;
        return;
    }
    rows.forEach(item => {
        const all = Number(item.all || 0);
        const read = Number(item.read || 0);
        const remaining = Number(item.remaining || (all - read));
        const percent = all > 0 ? Math.max(0, Math.min(100, Math.round((read / all) * 100))) : 0;
        const done = remaining <= 0 && all > 0;

        const row = document.createElement('div');
        row.className = 'progress-row';
        row.innerHTML = `
            <div>
                <div class="progress-user-name">${item.allname || item.uid}</div>
                <div class="progress-user-uid">UID: ${item.uid}</div>
            </div>
            <div class="progress-right">
                ${done ? `<div class="progress-done"><i class="fa-solid fa-check"></i> ${i18n.lookUp('completed')}</div>` : `
                    <div class="progress-bar"><div class="progress-bar-inner" style="width:${percent}%"></div></div>
                    <div>${remaining}/${all}</div>
                `}
            </div>
        `;
        container.appendChild(row);
    });
}

async function loadAllProgress(refresh = true) {
    const res = await getApi(url + `/api/proj/get_proj_progress_all?adminUid=${uid}&adminToken=${token}`);
    if (!res || !res.result) {
        const msg = parseInt(res.message);
        document.getElementById('allUserProgressList').innerHTML = `<div class="progress-failed">${i18n.lookUp('load_failed_retry')}</div>`;
        if (msg !== 28) {
            await openModal(
                i18n.lookUp("modal_content_fail")[msg].title,
                i18n.lookUp("modal_content_fail")[msg].message
            );
        } else {
            showBubble(i18n.lookUp("modal_content_fail")[msg].message, 'red', "#fff");
        }
        return;
    }
    if (refresh) {
        showBubble(i18n.lookUp('modal_content_success')[4].message, 'blue', "#fff");
    }
    renderProgressList('allUserProgressList', res.data || []);
}

async function queryProjProgress() {
    const projName = document.getElementById('progressProjName').value.trim();
    if (!projName) {
        showBubble(
            i18n.lookUp("modal_content_fail")[11].message,
            'red',
            "#fff"
        )
        return;
    }
    const res = await getApi(url + `/api/proj/get_proj_progress?proj=${projName}&adminUid=${uid}&adminToken=${token}`);
    if (!res || !res.result) {
        const msg = parseInt(res.message);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        cleanProjProgress();
        return;
    }

    renderProgressList('singleProjProgressList', res.data || []);
}

function cleanProjProgress() {
    document.getElementById('progressProjName').value = '';
    document.getElementById('singleProjProgressList').innerHTML = '';
    showBubble(i18n.lookUp("modal_content_success")[0].message, "blue", "#fff");
}
