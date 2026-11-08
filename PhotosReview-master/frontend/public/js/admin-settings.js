async function loadSystemSettingsForm() {
    const info = await getApi(url + "/api/system/get_website_info");
    if (!info || !info.result) {
        return;
    }
    document.getElementById("systemWebsiteName").value = info.data.website_name || "";
}

async function saveSystemSettings() {
    const websiteName = document.getElementById("systemWebsiteName").value.trim();
    const fileInput = document.getElementById("systemWebsiteIcon");

    if (!websiteName) {
        openModal(
            i18n.lookUp("modal_content_fail")[11].title,
            i18n.lookUp("modal_content_fail")[11].message
        );
    }

    const body = {
        adminUid: uid,
        adminToken: token,
        websiteName
    };
    const result = await postApiWithFile(url + "/api/system/update_website_info", body, fileInput);
    if (!result || !result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return;
    }
    showBubble(i18n.lookUp("modal_content_success")[0].message, 'blue', '#fff');
    await loadWebsiteInfo();
}