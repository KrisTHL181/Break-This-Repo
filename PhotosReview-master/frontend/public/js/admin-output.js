function startAdminFileDownload(endpoint, params) {
    const requestUrl = new URL(url + endpoint, window.location.href);
    Object.entries(params).forEach(([key, value]) => {
        requestUrl.searchParams.set(key, String(value));
    });

    const link = document.createElement("a");
    link.href = requestUrl.toString();
    link.download = "";
    link.hidden = true;
    document.body.appendChild(link);
    link.click();
    window.setTimeout(() => link.remove(), 0);
}

function populateOutputProjectSelect() {
    const select = document.getElementById("outputProjectSelect");
    const downloadButton = document.getElementById("downloadProjectData");
    if (!select || !downloadButton) {
        return;
    }

    const previousValue = select.value;
    select.replaceChildren();

    if (!Array.isArray(projList) || projList.length === 0) {
        const emptyOption = document.createElement("option");
        emptyOption.value = "";
        emptyOption.disabled = true;
        emptyOption.selected = true;
        emptyOption.dataset.i18n = "no_project";
        emptyOption.textContent = i18n.lookUp("no_project");
        select.appendChild(emptyOption);
        downloadButton.disabled = true;
        return;
    }

    projList.forEach((project) => {
        const option = document.createElement("option");
        option.value = project.projId;
        option.textContent = project.name;
        select.appendChild(option);
    });

    if (projList.some((project) => project.projId === previousValue)) {
        select.value = previousValue;
    }
    downloadButton.disabled = false;
}

function initOutputPage() {
    populateOutputProjectSelect();
}

async function downloadProjectData() {
    const projectId = document.getElementById("outputProjectSelect")?.value;
    if (!projectId) {
        await openModal(i18n.lookUp("error"), i18n.lookUp("missing_project_id"));
        return;
    }

    startAdminFileDownload("/api/data_manager/save_proj_data", {
        adminUid: uid,
        adminToken: token,
        projId: projectId
    });
    showBubble(i18n.lookUp("archive_download_started"), "blue", "#fff");
}

async function downloadAllProjectData() {
    startAdminFileDownload("/api/data_manager/save_all_proj_data", {
        adminUid: uid,
        adminToken: token
    });
    showBubble(i18n.lookUp("archive_download_started"), "blue", "#fff");
}
