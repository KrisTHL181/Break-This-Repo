const panelBtns = document.querySelectorAll('.panel-btn');
const viewers = document.querySelectorAll('.viewer');
let projList;
let currentManageProjId;
let selectedUploadToProjName;
/**
 *  Example:
 *      {
 *          "10000": [
 *              [0, 219],[220, 438]
 *          ],
 *          "10010": [[0, 438]],
 *          "10011": [[0, 438]]
 *      }
 */
let currentPreliminaryTaskList;
let currentRecheckTaskList;
let photosPool = {};
let currentManagePhotosList = [];

function goPage(p, updateHistory = true) {
    if (!p) {
        return;
    }
    panelBtns.forEach(b => b.classList.remove('selected'));
    panelBtns.forEach(b => {
        if (b.getAttribute('data-page') == p) {
            b.classList.add('selected');
        }
    });

    viewers.forEach(v => {
        v.style.display = v.id === p ? 'block' : 'none';
    });

    if (p === "outputData" && typeof populateOutputProjectSelect === "function") {
        populateOutputProjectSelect();
    }

    const urlParams = new URLSearchParams(window.location.search);
    const newParams = new URLSearchParams();
    newParams.set("p", p);

    if (p === "manageProj" || p === "manageGallery" || p === "viewResults") {
        if (currentManageProjId) {
            newParams.set("id", currentManageProjId);
        } else {
            goPage("projList");
            return;
        }
    }

    const targetUrl =
        `${window.location.pathname}?${newParams.toString()}`;

    if (updateHistory) {
        history.pushState({ page: p }, "", targetUrl);
    } else {
        history.replaceState({ page: p }, "", targetUrl);
    }
}

panelBtns.forEach(btn => {
    btn.addEventListener("click", () => {
        const page = btn.getAttribute('data-page');
        goPage(page);
    });
});

function restoreUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    const p = urlParams.get("p") || "projList";

    if (p === "manageProj" || p === "manageGallery" || p === "viewResults") {
        const id = urlParams.get("id");
        const index = id ? findProjIndexById(id) : -1;

        if (index < 0) {
            goPage("projList");
            return;
        }

        if (p === "manageProj") {
            loadManageProj(index, id, false);
        } else if (p === "manageGallery") {
            loadManageGallery(index, id, false);
        } else {
            loadViewResults(index, id, false);
        }

        return;
    }

    goPage(p, false);
}

// 监测url变化
window.addEventListener("popstate", () => {
    restoreUrl();
});

const uid = parseInt(getCookie('review_uid'), 10);
const token = getCookie('review_token');

// 检查登录情况（2分钟一次）
async function check_login() {
    // 获取结果
    let login_result = await getApi(url + `/api/user/check_token?uid=${uid}&token=${token}`);
    if (!login_result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[12].title,
            i18n.lookUp("modal_content_fail")[12].message
        );
        document.body.innerHTML = '';
        window.location.href = "./loginout.html";
        return false;
    }
    setTimeout(() => {
        check_login();
    }, 2000 * 60);
}
// 选择文件
function fileInputChange(id, allowed = ["jpg", "png", "jpeg", "webp"]) {
    const fileInput = document.getElementById(id);
    const file = fileInput.files[0]
    const fileName = file.name;
    const fileExt = fileName.split(".").pop().toLowerCase();
    if (!allowed.includes(fileExt)) {
        fileInput.value = "";
        return;
    }
    const imgExt = ["jpg", "png", "jpeg", "webp"];
    const display = document.getElementById(id + "Display");
    display.innerHTML = '';
    if (imgExt.includes(fileExt)) {
        const imgEl = document.createElement("img");
        imgEl.classList.add("file-preview");
        display.appendChild(imgEl);
        // 创建图片预览
        const reader = new FileReader();
        reader.onload = (e) => {
            imgEl.src = e.target.result;
        }
        reader.readAsDataURL(file);
    } else {
        const fileNameEl = document.createElement("span");
        fileNameEl.innerText = fileName;
        display.appendChild(fileNameEl);
    }
}

async function loadWebsiteInfo() {
    const info = await getApi(url + "/api/system/get_website_info");
    if (info) {
        const infoObj = info.data;

        const link = document.createElement("link");
        link.rel = "icon";
        link.href = infoObj.website_icon;
        document.head.appendChild(link);

        document.querySelectorAll("[data-config]").forEach(el => {
            const key = el.getAttribute("data-config");
            if (key === "website_name") {
                el.innerText = infoObj.website_name;
            }
            if (key === "website_url") {
                el.addEventListener("click", () => {
                    window.location.href = url + "/admin.html";
                });
            }
        });

        document.getElementsByTagName("title")[0].innerText = infoObj.website_name + " " + i18n.lookUp("nav_title");

    } else {
        setTimeout(() => {
            loadWebsiteInfo()
        }, 500);
    }
}

// 退出登录
async function signout() {
    let a = await openModal(
        i18n.lookUp("modal_content_confirm")[0].title,
        i18n.lookUp("modal_content_confirm")[0].message
    );
    if (a) {
        window.location.href = "./loginout.html"
    } else {
        return;
    }
}



function bindLoadingButtonEl(button, handler) {
    button.addEventListener("click", async (event) => {
        const wasDisabled = button.disabled;
        const spinner = document.createElement("i");
        const spacer = document.createTextNode(" ");

        spinner.className = "fa-solid fa-spinner fa-spin";
        button.prepend(spinner, spacer);
        button.disabled = true;

        try {
            await handler.call(button, event);
        } finally {
            spinner.remove();
            spacer.remove();
            button.disabled = wasDisabled;
        }
    });
}

function bindLoadingButton(id, handler) {
    const button = document.getElementById(id);
    bindLoadingButtonEl(button, handler);
}

(async () => {
    await i18n.init();

    // 检查cookie
    if (!uid || !token) {
        document.body.innerHTML = '';
        window.location.href = "./login.html";
        return false;
    }
    check_login();
    document.getElementById("uidEl").innerHTML = 'UID: ' + uid.toString();

    // Loading website information
    await loadWebsiteInfo();
    await getProj();
    initOutputPage();
    await loadManageUserList();
    await loadSystemSettingsForm();
    await loadAllProgress(false);

    // 读取GET参数p，切换到对应页面
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("p")) {
        restoreUrl();
    }

    // Loading Button Events
    bindLoadingButton("submitCreateProj", createProj);
    bindLoadingButton("cleanCreateProj", cleanCreateProj);
    bindLoadingButton("saveManageProj", saveManageProj);
    bindLoadingButton("cancelManageProj", () => {
        window.history.back();
    });
    bindLoadingButton("saveManageDist", saveManageDist);
    bindLoadingButton("cancelManageDist", () => {
        window.history.back();
    });
    document.getElementById("imgInputCreateThumbnail").addEventListener("change", () => {
        fileInputChange("imgInputCreateThumbnail");
    });
    document.getElementById("imgInputManageThumbnail").addEventListener("change", () => {
        fileInputChange("imgInputManageThumbnail");
    });
    document.getElementById("systemWebsiteIcon").addEventListener("change", () => {
        fileInputChange("systemWebsiteIcon");
    });
    bindLoadingButton("cleanPhotosPool", cleanPhotosPool);
    bindLoadingButton("uploadPhotosPool", uploadImages);
    bindLoadingButton("submitRegisterUser", registerUser);
    bindLoadingButton("filterManagePhotos", filterManagePhotos);
    bindLoadingButton("saveSystemSettings", saveSystemSettings);
    bindLoadingButton("downloadSelectedPhotos", downloadSelectedPhotos);
    bindLoadingButton("deleteSelectedPhotos", deleteSelectedPhotos);
    bindLoadingButton("deleteUserScores", deleteUserScores);
    bindLoadingButton("fetchPreliminaryResult", fetchPreliminaryResult);
    bindLoadingButton("buildFinalResult", buildFinalResultData);
    bindLoadingButton("exportPhotosByScoreRange", exportPhotosByScoreRange);
    bindLoadingButton("downloadProjectData", downloadProjectData);
    bindLoadingButton("inputProjectData", importProjectData);
    document.getElementById("inputProjectArchive").addEventListener("change", updateProjectArchiveDisplay);
    document.getElementById("previewFinalResult")?.addEventListener("click", () => {
        void previewFinalResultPage();
    });
    document.getElementById("personalResultPreviewForm")?.addEventListener("submit", (event) => {
        event.preventDefault();
        void previewPersonalResultPage();
    });
    bindLoadingButton("refreshDisputePhotos", () => refreshRecheckList(true));
    bindLoadingButton("includeSelectedDisputePhotos", () => setSelectedDisputePhotosRecheck(true));
    bindLoadingButton("removeSelectedDisputePhotos", () => setSelectedDisputePhotosRecheck(false));
    initDisputePhotosPagination();
    initDisputePhotosSorting();

    const imgInputUploadEl = document.getElementById("imgInputUpload")
    const selectImageToPoolEl = document.getElementById("selectImagesToPool")
    selectImageToPoolEl.addEventListener("click", () => {
        imgInputUploadEl.click();
    });
    imgInputUploadEl.addEventListener("change", (e) => {
        photosPoolChange(e.target.files)
    });

    bindLoadingButton("refreshAllProgress", loadAllProgress);
    bindLoadingButton("queryProjProgress", queryProjProgress);
    bindLoadingButton("clearProjProgress", cleanProjProgress);

})();
