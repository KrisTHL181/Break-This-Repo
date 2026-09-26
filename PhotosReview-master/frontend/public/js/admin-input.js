function updateProjectArchiveDisplay() {
    const input = document.getElementById("inputProjectArchive");
    const display = document.getElementById("inputProjectArchiveDisplay");
    const file = input?.files?.[0];

    display.replaceChildren();
    const label = document.createElement("span");
    if (!file) {
        label.dataset.i18n = "you_havent_chosen";
        label.textContent = i18n.lookUp("you_havent_chosen");
    } else {
        label.textContent = file.name;
    }
    display.appendChild(label);
}

async function importProjectData() {
    const input = document.getElementById("inputProjectArchive");
    const file = input?.files?.[0];
    if (!file) {
        showBubble(i18n.lookUp("choose_a_zip"), "red", "#fff");
        return;
    }
    if (!file.name.toLowerCase().endsWith(".zip")) {
        showBubble(i18n.lookUp("invalid_zip_file"), "red", "#fff");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("uid", String(uid));
    formData.append("token", token);

    try {
        const response = await fetch(url + "/api/data_manager/input_proj_data", {
            method: "POST",
            credentials: "include",
            body: formData
        });
        const result = await response.json();
        if (!response.ok || !result.result) {
            showBubble(result.message || i18n.lookUp("import_project_failed"), "red", "#fff");
            return;
        }

        input.value = "";
        updateProjectArchiveDisplay();
        await getProj();
        populateOutputProjectSelect();
        await openModal(
            i18n.lookUp("information"),
            i18n.lookUp("import_project_success") + result.data.projId
        );
    } catch (error) {
        console.error("Project import failed", error);
        showBubble(i18n.lookUp("import_project_failed"), "red", "#fff");
    }
}
