// Create Project
async function createProj() {
    const fileInput = document.getElementById("imgInputCreateThumbnail");
    const projName = document.querySelector('#createProj input[name="input_project_name"]').value;
    const reviewTypeInput = document.querySelector('#createProj input[name="review_type"]:checked');
    const maxScoreInput = document.querySelector('#createProj input[name="input_project_max_score"]');
    // 非空
    if (!projName || projName.trim() === "" || !reviewTypeInput || !maxScoreInput || fileInput.value === '') {
        showBubble(i18n.lookUp("modal_content_fail")[11].message, 'red', '#fff');
        return;
    }

    if (projName.length > 100) {
        showBubble(i18n.lookUp("modal_content_fail")[6].message, 'red', '#fff');
        return;
    }

    const reviewType = parseInt(reviewTypeInput.value, 10);
    const maxScore = parseInt(maxScoreInput.value, 10);

    if (isNaN(maxScore) || maxScore < 3 || maxScore > 10) {
        showBubble(i18n.lookUp("modal_content_fail")[1].message, 'red', '#fff');
        return;
    }

    const confirmed = await openModal(
        i18n.lookUp("modal_content_confirm")[3].title,
        i18n.lookUp("confirm_create_project")
    );
    if (!confirmed) {
        return;
    }
    const param = {
        name: projName,
        type: reviewType,
        max: maxScore,
        adminUid: uid,
        adminToken: token
    }
    const result = await postApiWithFile(url + "/api/proj/create_proj", param, fileInput);
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        showBubble(i18n.lookUp("modal_content_fail")[msg]["message"], 'red', '#fff');
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        cleanCreateProj();
        getProj();
    }
}

// Clean Create Project Form
function cleanCreateProj() {
    document.querySelector("#createProj .text-input").value = "";
    document.querySelector('input[name="review_type"][value="0"]').checked = true;
    document.querySelector('#createProj input[name="input_project_max_score"]').value = "3";
    const fileInput = document.getElementById("imgInputCreateThumbnail");
    fileInput.value = "";
    const display = document.getElementById("imgInputCreateThumbnailDisplay");
    display.innerHTML = `<span>${i18n.lookUp("you_havent_chosen")}</span>`;
}
