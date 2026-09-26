async function loadManageGallery(index, id, updateHistory = true) {
    if (!projList[index]) {
        openModal(
            i18n.lookUp("modal_content_fail")[0].title,
            i18n.lookUp("modal_content_fail")[0].message
        );
        return;
    }
    currentManageProjId = id;
    goPage("manageGallery", updateHistory);
    await loadManageDist(index, id);
    loadManagePhotosList();
}

async function createThumbnailFromFile(file, maxSize = 100) {
    const img = new Image();
    // 生成本地临时 URL，不会把图片转成 Base64
    const objectUrl = URL.createObjectURL(file);

    try {
        img.src = objectUrl;
        // 等待图片加载完成
        await img.decode();

        let width = img.naturalWidth;
        let height = img.naturalHeight;

        if (width > height) {
            if (width > maxSize) {
                height = height * (maxSize / width);
                width = maxSize;
            }
        } else {
            if (height > maxSize) {
                width = width * (maxSize / height);
                height = maxSize;
            }
        }

        if (!width || !height) {
            throw new Error("图片宽高异常");
        }

        const canvas = document.createElement("canvas");
        canvas.width = Math.round(width);
        canvas.height = Math.round(height);

        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

        // 输出低分辨率图片
        return canvas.toDataURL("image/jpeg", 0.75);
    } finally {
        // 释放临时 URL
        URL.revokeObjectURL(objectUrl);
    }
}
// 添加图片后
async function photosPoolChange(files) {
    // 图片池
    const filePoolEl = document.getElementById("filePool");
    // 图片扩展名
    const imgExt = ["jpg", "png", "jpeg", "webp"];
    // 队列
    const queue = new Queue();

    [...files].forEach(file => {
        queue.add(async () => {
            // 分配uuid
            const uuid = generateUuid();
            const fileName = file.name;
            const fileExt = fileName.split(".").pop().toLowerCase();
            if (!imgExt.includes(fileExt)) {
                return;
            }

            const itemEl = document.createElement("div");
            itemEl.classList.add("item");
            // 创建图片预览
            const imgEl = document.createElement("img");

            try {
                // 创建缩略图
                imgEl.src = await createThumbnailFromFile(file);
                itemEl.appendChild(imgEl);

                const progressContainerEl = document.createElement("div");
                progressContainerEl.classList.add("progress-container");
                const progressBar = document.createElement("div");
                progressBar.classList.add("progress-bar");
                progressBar.style.width = '0%';
                progressBar.style.display = "none";
                progressContainerEl.appendChild(progressBar);
                const closeEl = document.createElement("div");
                closeEl.classList.add("close");
                closeEl.innerHTML = '×';
                closeEl.addEventListener("click", () => {
                    delete photosPool[uuid];
                    itemEl.remove();
                });
                const labelEl = document.createElement("div");
                labelEl.classList.add("label", "color_blue");
                labelEl.innerHTML = "READY";

                itemEl.appendChild(progressContainerEl);
                itemEl.appendChild(closeEl);
                itemEl.appendChild(labelEl);
                filePoolEl.appendChild(itemEl);

                let fileX = {};
                fileX.file = file;
                fileX.progressBar = progressBar;
                fileX.labelEl = labelEl;
                // 添加数组
                photosPool[uuid] = fileX;
            } catch (e) {
                console.error("Failed to create thumbnail:", e);
            }
        });
    });
}
// 上传
async function uploadImages() {
    const filePoolEl = document.getElementById("filePool");
    const author = document.querySelector('#manageGallery input[name="input_author"]').value;
    if (!currentManageProjId || !author|| Object.keys(photosPool).length === 0) {
        openModal(
            i18n.lookUp("modal_content_fail")[11].title,
            i18n.lookUp("modal_content_fail")[11].message
        );
        return;
    }
    if (author.length > 50) {
        openModal(
            i18n.lookUp("modal_content_fail")[6].title,
            i18n.lookUp("modal_content_fail")[6].message
        );
        return;
    }

    const uuids = Object.keys(photosPool);
    const files = Object.values(photosPool);
    const queue = new Queue();
    let successCount = 0;

    // 添加完成事件
    queue.addFinalTask(() => {
        loadManagePhotosList();
        refreshManageProjectInfo();
        showBubble(i18n.lookUp("successful_rows") + successCount, "blue", "#fff");
    });
    // 添加任务
    for (let i = 0; i < files.length; i++) {
        const fileX = files[i];
        const uuid = uuids[i];

        queue.add(async () => {
            const progressBar = fileX.progressBar;
            const labelEl = fileX.labelEl
            let param = {
                projId: currentManageProjId,
                author: author,
                adminUid: uid,
                adminToken: token
            };

            progressBar.style.display = "block";
            labelEl.classList.remove(...labelEl.classList);
            labelEl.classList.add("label", "color_blue");
            labelEl.innerHTML = 'UPLOADING';

            const result = await postApiWithFileOnProgress(
                url + "/api/proj/upload_image",
                param,
                fileX.file,
                progressBar
            );
            if (!result.result) {
                const msg = result.message;
                showBubble(i18n.lookUp("modal_content_fail")[19].message, "red", "#fff");
                labelEl.classList.remove(...labelEl.classList);
                labelEl.classList.add("label", "color_red");
                labelEl.innerHTML = "FAIL";
            } else {
                labelEl.classList.remove(...labelEl.classList);
                labelEl.classList.add("label", "color_green");
                labelEl.innerHTML = "SUCCESS";
                delete photosPool[uuid];
                successCount += 1;
            }
        });
    }
}
// 清除
function cleanPhotosPool() {
    const filePoolEl = document.getElementById("filePool");
    filePoolEl.innerHTML = '';
    Object.keys(photosPool).forEach(key => delete photosPool[key]);
}

let managePhotosCurrentPage = 1;
let managePhotosPageSize = 10;
let managePhotosCurrentAuthor = null;

async function renderReviewDataModal(data, type) {
    const modal = document.getElementById("reviewDataModal");
    const overlay = document.getElementById("modalOverlay");
    const titleEl = document.getElementById("reviewDataModalTitle");
    const bodyEl = document.getElementById("reviewDataModalBody");
    const closeBtnEl = document.getElementById("reviewDataCloseBtn");
    const confirmBtnEl = document.getElementById("reviewDataConfirmBtn");

    if (!modal || !overlay || !titleEl || !bodyEl || !closeBtnEl || !confirmBtnEl) {
        return;
    }

    const displayText = (value, fallback = "-") => {
        return value === undefined || value === null || value === "" ? fallback : String(value);
    };
    const createTextElement = (tagName, className, text) => {
        const element = document.createElement(tagName);
        element.classList.add(className);
        element.textContent = text;
        return element;
    };
    const normalizeReview = (reviewer, review) => {
        if (Array.isArray(review)) {
            const noteEmpty = review[1] === undefined || review[1] === null || review[1] === "";
            return {
                reviewer: displayText(reviewer),
                score: displayText(review[0]),
                note: noteEmpty ? i18n.lookUp("no_note") : String(review[1]),
                noteEmpty
            };
        }
        if (review && typeof review === "object") {
            const note = review.note ?? review.comment;
            const noteEmpty = note === undefined || note === null || note === "";
            return {
                reviewer: displayText(review.uid ?? review.reviewer ?? reviewer),
                score: displayText(review.score),
                note: noteEmpty ? i18n.lookUp("no_note") : String(note),
                noteEmpty
            };
        }
        return {
            reviewer: displayText(reviewer),
            score: displayText(review),
            note: i18n.lookUp("no_note"),
            noteEmpty: true
        };
    };
    const normalizeReviews = (value) => {
        if (Array.isArray(value)) {
            if (value.length > 0 && Array.isArray(value[0])) {
                return value.map((review) => normalizeReview(review[0], review.slice(1)));
            }
            if (value.length > 0) {
                return [normalizeReview(value[0], value.slice(1))];
            }
        } else if (value && typeof value === "object") {
            return Object.entries(value).map(([reviewer, review]) => normalizeReview(reviewer, review));
        }
        return [];
    };

    const reviews = normalizeReviews(data.preliminary);
    const isRecheck = data.is_recheck === true;
    const recheckReviews = isRecheck ? normalizeReviews(data.recheck) : [];

    const closeModal = () => {
        if (!modal.classList.contains("active")) {
            return;
        }
        modal.classList.remove("active");
        modalNum = Math.max(0, modalNum - 1);
        if (modalNum === 0) {
            overlay.classList.remove("active");
        }
    };

    titleEl.textContent = i18n.lookUp("preview");
    modal.classList.add("photos-review-modal");

    const shellEl = document.createElement("div");
    shellEl.classList.add("photos-review-shell");

    const summarySectionEl = document.createElement("section");
    summarySectionEl.classList.add("photos-review-section");

    const summaryEl = document.createElement("div");
    summaryEl.classList.add("photos-review-summary");
    [
        [i18n.lookUp("photoid"), data.photoid],
        [i18n.lookUp("file_name"), data.name],
        [i18n.lookUp("author"), data.author],
        [i18n.lookUp("recheck_status"), i18n.lookUp(isRecheck ? "yes" : "no")]
    ].forEach(([label, value]) => {
        const itemEl = document.createElement("div");
        itemEl.classList.add("photos-review-summary-item");
        itemEl.appendChild(createTextElement("span", "photos-review-summary-label", label));
        itemEl.appendChild(createTextElement("strong", "photos-review-summary-value", displayText(value)));
        summaryEl.appendChild(itemEl);
    });
    summarySectionEl.appendChild(summaryEl);
    shellEl.appendChild(summarySectionEl);

    const reviewGroups = [
        {
            title: i18n.lookUp("preliminary_scoring_details"),
            items: reviews,
            deleteEndpoint: "/api/review/delete_score"
        },
        ...(isRecheck ? [{
            title: i18n.lookUp("recheck_scoring_details"),
            items: recheckReviews,
            deleteEndpoint: "/api/review/delete_recheck_score"
        }] : [])
    ];
    reviewGroups.forEach((reviewGroup) => {
        if (type === 1 && reviewGroup.title === i18n.lookUp("preliminary_scoring_details") && isRecheck) {
            return;
        }
        const reviewsSectionEl = document.createElement("section");
        reviewsSectionEl.classList.add("photos-review-section");
        reviewsSectionEl.appendChild(
            createTextElement("h5", "photos-review-section-title", `${reviewGroup.title} (${reviewGroup.items.length})`)
        );

        const reviewsListEl = document.createElement("div");
        reviewsListEl.classList.add("photos-review-list");

        if (reviewGroup.items.length === 0) {
            reviewsListEl.appendChild(
                createTextElement(
                    "div",
                    "photos-review-empty",
                    i18n.lookUp("score_empty")
                )
            );
        } else {
            reviewGroup.items.forEach((review) => {
                const reviewEl = document.createElement("article");
                reviewEl.classList.add("photos-review-item");

                const reviewHeaderEl = document.createElement("div");
                reviewHeaderEl.classList.add("photos-review-item-header");

                const reviewerEl = document.createElement("div");
                reviewerEl.classList.add("photos-review-reviewer");
                reviewerEl.appendChild(createTextElement("span", "photos-review-field-label", i18n.lookUp("uid")));
                reviewerEl.appendChild(createTextElement("strong", "photos-review-reviewer-value", review.reviewer));
                reviewHeaderEl.appendChild(reviewerEl);

                const scoreEl = document.createElement("div");
                scoreEl.classList.add("photos-review-score");
                scoreEl.appendChild(
                    createTextElement(
                        "span",
                        "photos-review-field-label",
                        i18n.lookUp("score")
                    )
                );
                scoreEl.appendChild(createTextElement("strong", "photos-review-score-value", review.score));
                reviewHeaderEl.appendChild(scoreEl);

                const deleteBtnEl = document.createElement("button");
                deleteBtnEl.type = "button";
                deleteBtnEl.classList.add("photos-review-btn", "delete");
                deleteBtnEl.title = i18n.lookUp("delete");
                const deleteIconEl = document.createElement("i");
                deleteIconEl.classList.add("fa-solid", "fa-trash");
                deleteBtnEl.appendChild(deleteIconEl);
                deleteBtnEl.appendChild(document.createTextNode(i18n.lookUp("delete")));
                reviewHeaderEl.appendChild(deleteBtnEl);

                deleteBtnEl.addEventListener("click", async () => {
                    const confirm = await openModal(
                        i18n.lookUp("modal_content_confirm")[5].title,
                        i18n.lookUp("modal_content_confirm")[5].message
                    );
                    if (!confirm) {
                        return;
                    }

                    const param = {
                        "photoid": data.photoid,
                        "uid": parseInt(review.reviewer, 10),
                        "adminUid": uid,
                        "adminToken": token
                    }
                    const result = await postApi(
                        url + reviewGroup.deleteEndpoint,
                        param
                    )

                    if (!result.result) {
                        const msg = parseInt(result.message, 10);
                        await openModal(
                            i18n.lookUp("modal_content_fail")[msg].title,
                            i18n.lookUp("modal_content_fail")[msg].message
                        );
                        return;
                    }
                    showBubble(
                        i18n.lookUp("modal_content_success")[0].message,
                        "blue",
                        "#fff"
                    )
                    reviewEl.remove();
                })

                const noteEl = document.createElement("div");
                noteEl.classList.add("photos-review-note");
                noteEl.appendChild(
                    createTextElement(
                        "span",
                        "photos-review-field-label",
                        i18n.lookUp("note")
                    )
                );
                const noteContentEl = createTextElement("p", "photos-review-note-content", review.note);
                if (review.noteEmpty) {
                    noteContentEl.classList.add("photos-review-note-empty");
                }
                noteEl.appendChild(noteContentEl);

                reviewEl.appendChild(reviewHeaderEl);
                reviewEl.appendChild(noteEl);
                reviewsListEl.appendChild(reviewEl);
            });
        }

        reviewsSectionEl.appendChild(reviewsListEl);
        shellEl.appendChild(reviewsSectionEl);
    });

    if (isRecheck) {
        const maxScore = Number(data.max_score);
        const finalScoreSectionEl = document.createElement("section");
        finalScoreSectionEl.classList.add("photos-review-section");
        finalScoreSectionEl.appendChild(
            createTextElement("h5", "photos-review-section-title", i18n.lookUp("confirm_final_score"))
        );

        const finalScoreFormEl = document.createElement("div");
        finalScoreFormEl.classList.add("photos-review-final-score-form");
        const finalScoreInputGroupEl = document.createElement("div");
        finalScoreInputGroupEl.classList.add("photos-review-final-score-input-group");
        finalScoreInputGroupEl.appendChild(
            createTextElement("label", "photos-review-field-label", i18n.lookUp("final_score"))
        );

        const finalScoreInputEl = document.createElement("input");
        finalScoreInputEl.type = "text";
        finalScoreInputEl.inputMode = "decimal";
        finalScoreInputEl.autocomplete = "off";
        finalScoreInputEl.classList.add("photos-review-score-input");
        finalScoreInputEl.placeholder = i18n.lookUp("final_score_range").replace("{max}", displayText(maxScore));
        if (Number(data.final_score) >= 1) {
            finalScoreInputEl.value = Number(data.final_score).toFixed(1);
        }
        finalScoreInputGroupEl.appendChild(finalScoreInputEl);
        finalScoreInputGroupEl.appendChild(
            createTextElement(
                "p",
                "photos-review-final-score-hint",
                i18n.lookUp("final_score_range").replace("{max}", displayText(maxScore))
            )
        );
        finalScoreFormEl.appendChild(finalScoreInputGroupEl);

        const submitFinalScoreBtnEl = document.createElement("button");
        submitFinalScoreBtnEl.type = "button";
        submitFinalScoreBtnEl.classList.add("photos-review-btn");
        const submitFinalScoreIconEl = document.createElement("i");
        submitFinalScoreIconEl.classList.add("fa-solid", "fa-check");
        submitFinalScoreBtnEl.appendChild(submitFinalScoreIconEl);
        submitFinalScoreBtnEl.appendChild(document.createTextNode(i18n.lookUp("submit")));
        submitFinalScoreBtnEl.addEventListener("click", async () => {
            const scoreText = finalScoreInputEl.value.trim();
            const score = Number(scoreText);
            const validPrecision = /^\d+(?:\.\d)?$/.test(scoreText);
            if (!validPrecision || !Number.isFinite(score) || score < 1 || score > maxScore) {
                await openModal(
                    i18n.lookUp("error"),
                    i18n.lookUp("final_score_invalid").replace("{max}", displayText(maxScore))
                );
                return;
            }

            submitFinalScoreBtnEl.disabled = true;
            const result = await postApi(url + "/api/review/final", {
                photoid: Number(data.photoid),
                uid,
                token,
                score
            });
            submitFinalScoreBtnEl.disabled = false;
            if (!result.result) {
                const msg = parseInt(result.message, 10);
                await openModal(
                    i18n.lookUp("modal_content_fail")[msg]?.title || i18n.lookUp("error"),
                    i18n.lookUp("modal_content_fail")[msg]?.message || result.message
                );
                return;
            }
            finalScoreInputEl.value = score.toFixed(1);
            if (typeof refreshRecheckList === "function") {
                await refreshRecheckList(false);
            }
            showBubble(i18n.lookUp("final_score_saved"), "blue", "#fff");
        });
        finalScoreFormEl.appendChild(submitFinalScoreBtnEl);
        finalScoreSectionEl.appendChild(finalScoreFormEl);
        shellEl.appendChild(finalScoreSectionEl);
    }
    bodyEl.replaceChildren(shellEl);

    closeBtnEl.onclick = closeModal;
    confirmBtnEl.onclick = closeModal;
    modal.classList.add("active");
    overlay.classList.add("active");
    modalNum++;
}

async function previewPhotoReviewData(photoId, type = 0) {
    const result = await getApi(
        url + "/api/result/fetch_photo_data?adminUid=" + uid
        + "&adminToken=" + encodeURIComponent(token)
        + "&photoid=" + photoId
    );
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg]?.title || i18n.lookUp("error"),
            i18n.lookUp("modal_content_fail")[msg]?.message || result.message
        );
        return;
    }
    await renderReviewDataModal(result.data || {}, type);
}

function createManagePhotoRow(photo, index) {
    const trEl = document.createElement("tr");
    trEl.setAttribute("data-photo-id", photo.id);
    trEl.setAttribute("data-photo-index", index);

    const photoIdTdEl = document.createElement("td");
    photoIdTdEl.innerHTML = `<label class="checkbox">
                    <input type="checkbox" name="input_manage_photo" value="${photo.id}"><span class="box"></span>${photo.id}
                </label>
                `;
    trEl.appendChild(photoIdTdEl);

    const thumbnailTdEl = document.createElement("td");
    const thumbnailImgEl = document.createElement("img");
    thumbnailImgEl.src = `/data/proj/${currentManageProjId}/thumbnail/${photo.name}`;
    thumbnailImgEl.style.cursor = "pointer";
    thumbnailImgEl.addEventListener("click", () => {
        go_url(`/data/proj/${currentManageProjId}/img/${photo.name}`, 1);
    });
    thumbnailTdEl.appendChild(thumbnailImgEl);
    trEl.appendChild(thumbnailTdEl);

    const authorTdEl = document.createElement("td");
    authorTdEl.innerHTML = photo.author;
    trEl.appendChild(authorTdEl);

    const actionTdEl = document.createElement("td");
    const actionGroupEl = document.createElement("div");
    actionGroupEl.classList.add("photo-actions");

    const previewBtnEl = document.createElement("span");
    previewBtnEl.classList.add("btn", "edit");
    previewBtnEl.title = i18n.lookUp("preview");
    previewBtnEl.innerHTML = `<i class="fa-solid fa-eye"></i>`;
    previewBtnEl.addEventListener("click", async () => {
        document.getElementById("managePhotosTableBody").querySelectorAll("tr").forEach(el => {
            el.removeAttribute("class");
        })
        trEl.classList.add("mark");
        previewPhotoReviewData(photo.id);
    });
    actionGroupEl.appendChild(previewBtnEl);

    const delBtnEl = document.createElement("span");
    delBtnEl.classList.add("btn", "del");
    delBtnEl.title = i18n.lookUp("delete");
    delBtnEl.innerHTML = `<i class="fa-solid fa-trash"></i>`;
    delBtnEl.addEventListener("click", async () => {
        const confirm = await openModal(
            i18n.lookUp("modal_content_confirm")[4].title,
            i18n.lookUp("modal_content_confirm")[4].message
        );
        if (confirm) {
            delPhoto(photo.id, trEl, index);
        }
    });
    actionGroupEl.appendChild(delBtnEl);
    actionTdEl.appendChild(actionGroupEl);
    trEl.appendChild(actionTdEl);

    return trEl;
}

function getManagePhotosPaginationItems(totalPages, currentPage) {
    const maxButtons = 17;
    if (totalPages <= maxButtons) {
        return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const edgePageCount = maxButtons - 1;
    const middlePageCount = maxButtons - 2;
    const middleHalf = Math.floor(middlePageCount / 2);

    if (currentPage <= middleHalf + 2) {
        return [
            ...Array.from({ length: edgePageCount }, (_, index) => index + 1),
            "ellipsis-right"
        ];
    }

    if (currentPage >= totalPages - middleHalf - 1) {
        return [
            "ellipsis-left",
            ...Array.from({ length: edgePageCount }, (_, index) => totalPages - edgePageCount + index + 1)
        ];
    }

    return [
        "ellipsis-left",
        ...Array.from({ length: middlePageCount }, (_, index) => currentPage - middleHalf + index),
        "ellipsis-right"
    ];
}

/**
 * 根据当前照片列表、页码和每页数量，渲染照片管理表格及分页控件。
 */
function renderManagePhotosPage() {
    // 获取照片表格、统计信息和分页区域所需的页面元素。
    const managePhotosTableBodyEl = document.getElementById("managePhotosTableBody");
    const photosTotalEl = document.getElementById("photosTotal");
    const paginationEl = document.getElementById("managePhotosPagination");
    const pageNumbersEl = document.getElementById("managePhotosPageNumbers");
    const prevBtnEl = document.getElementById("managePhotosPrevPage");
    const nextBtnEl = document.getElementById("managePhotosNextPage");
    const pageSizeSelectEl = document.getElementById("managePhotosPageSize");
    const selectCurrentPageEl = document.getElementById("selectCurrentManagePhotosPage");

    if (!managePhotosTableBodyEl || !photosTotalEl) {
        return;
    }

    // 清空旧表格内容，并同步照片总数和每页显示数量。
    managePhotosTableBodyEl.innerHTML = "";
    const length = currentManagePhotosList.length;
    photosTotalEl.innerHTML = String(length);

    if (selectCurrentPageEl) {
        selectCurrentPageEl.checked = false;
        selectCurrentPageEl.disabled = length === 0;
    }

    if (pageSizeSelectEl) {
        pageSizeSelectEl.value = String(managePhotosPageSize);
    }

    // 列表为空时展示空状态，同时隐藏分页控件。
    if (length === 0) {
        const trEl = document.createElement("tr");
        const tdEl = document.createElement("td");
        tdEl.colSpan = 4;
        tdEl.innerHTML = i18n.lookUp("no_photo");
        trEl.appendChild(tdEl);
        managePhotosTableBodyEl.appendChild(trEl);
        if (paginationEl) {
            paginationEl.style.display = "none";
        }
        return;
    }

    // 计算分页范围并校正当前页，防止删除或切换数据后页码越界。
    const totalPages = Math.max(1, Math.ceil(length / managePhotosPageSize));
    managePhotosCurrentPage = Math.min(Math.max(1, managePhotosCurrentPage), totalPages);
    const start = (managePhotosCurrentPage - 1) * managePhotosPageSize;
    const end = Math.min(start + managePhotosPageSize, length);

    // 截取当前页数据并逐行渲染照片信息。
    currentManagePhotosList.slice(start, end).forEach((photo, offset) => {
        managePhotosTableBodyEl.appendChild(createManagePhotoRow(photo, start + offset));
    });

    // 分页元素不完整时保留已渲染的表格，不继续更新分页区域。
    if (!paginationEl || !pageNumbersEl || !prevBtnEl || !nextBtnEl) {
        return;
    }

    // 显示分页控件，并根据当前页更新上一页、下一页按钮状态。
    paginationEl.style.display = "flex";
    prevBtnEl.disabled = managePhotosCurrentPage === 1;
    nextBtnEl.disabled = managePhotosCurrentPage === totalPages;
    pageNumbersEl.innerHTML = "";

    // 创建页码或省略号按钮；点击具体页码后重新渲染对应页面。
    getManagePhotosPaginationItems(totalPages, managePhotosCurrentPage).forEach((page) => {
        const pageBtnEl = document.createElement("button");
        pageBtnEl.type = "button";
        pageBtnEl.classList.add("page-number");

        if (typeof page !== "number") {
            pageBtnEl.classList.add("page-ellipsis");
            pageBtnEl.disabled = true;
            pageBtnEl.innerHTML = "...";
            pageNumbersEl.appendChild(pageBtnEl);
            return;
        }

        if (page === managePhotosCurrentPage) {
            pageBtnEl.classList.add("active");
        }
        pageBtnEl.innerHTML = String(page);
        pageBtnEl.addEventListener("click", () => {
            managePhotosCurrentPage = page;
            renderManagePhotosPage();
        });
        pageNumbersEl.appendChild(pageBtnEl);
    });
}

/**
 * 初始化照片管理分页相关事件，确保同一组控件只绑定一次监听器。
 */
function initManagePhotosPagination() {
    const paginationEl = document.getElementById("managePhotosPagination");
    // 分页容器不存在或已经初始化时不重复绑定事件。
    if (!paginationEl || paginationEl.dataset.initialized === "true") {
        return;
    }
    paginationEl.dataset.initialized = "true";

    const selectCurrentPageEl = document.getElementById("selectCurrentManagePhotosPage");
    const managePhotosTableBodyEl = document.getElementById("managePhotosTableBody");
    selectCurrentPageEl?.addEventListener("change", () => {
        managePhotosTableBodyEl?.querySelectorAll('input[name="input_manage_photo"]')
            .forEach((checkboxEl) => {
                checkboxEl.checked = selectCurrentPageEl.checked;
            });
    });
    managePhotosTableBodyEl?.addEventListener("change", (event) => {
        if (!event.target.matches('input[name="input_manage_photo"]') || !selectCurrentPageEl) {
            return;
        }
        const pageCheckboxes = Array.from(
            managePhotosTableBodyEl.querySelectorAll('input[name="input_manage_photo"]')
        );
        selectCurrentPageEl.checked = pageCheckboxes.length > 0
            && pageCheckboxes.every((checkboxEl) => checkboxEl.checked);
    });

    // 上一页：页码减一后重新渲染列表。
    document.getElementById("managePhotosPrevPage")?.addEventListener("click", () => {
        if (managePhotosCurrentPage > 1) {
            managePhotosCurrentPage -= 1;
            renderManagePhotosPage();
        }
    });
    // 下一页：未到末页时页码加一并重新渲染列表。
    document.getElementById("managePhotosNextPage")?.addEventListener("click", () => {
        const totalPages = Math.max(1, Math.ceil(currentManagePhotosList.length / managePhotosPageSize));
        if (managePhotosCurrentPage < totalPages) {
            managePhotosCurrentPage += 1;
            renderManagePhotosPage();
        }
    });
    // 每页数量变更：更新分页大小，并从第一页重新渲染。
    document.getElementById("managePhotosPageSize")?.addEventListener("change", (event) => {
        managePhotosPageSize = Number(event.target.value) || 10;
        managePhotosCurrentPage = 1;
        renderManagePhotosPage();
    });
    // 刷新：保留当前作者筛选条件和页码，重新请求照片列表。
    document.getElementById("refreshManagePhotos")?.addEventListener("click", () => {
        loadManagePhotosList(managePhotosCurrentAuthor, managePhotosCurrentPage);
    });
}

/**
 * 按作者筛选条件加载当前项目的照片列表，并渲染指定页。
 * @param {string|null} author 作者筛选条件；null 表示不限制作者。
 * @param {number} page 数据加载成功后需要展示的页码。
 */
async function loadManagePhotosList(author = null, page = 1) {
    // 确保分页交互已初始化，并记录筛选条件供刷新操作复用。
    initManagePhotosPagination();
    managePhotosCurrentAuthor = author;
    // 接口使用空字符串表示不按作者筛选。
    if (author === null) {
        author = "";
    }
    // 请求当前项目中符合作者条件的全部照片。
    const result = await getApi(url + `/api/review/fetch_photo_list_all?projId=${currentManageProjId}&author=${author}&adminUid=${uid}&adminToken=${token}`);
    // 请求失败时展示接口对应的错误信息，并停止更新页面。
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return;
    }

    // 保存最新数据和目标页码，再统一渲染照片表格及分页控件。
    currentManagePhotosList = result.data || [];
    managePhotosCurrentPage = page || 1;
    renderManagePhotosPage();
}

// 删除单个照片
async function delPhoto(photoId, tr, index, isBatch = false) {
    const resultDel = await postApi(url + "/api/proj/delete_photo", {
        id: photoId,
        adminUid: uid,
        adminToken: token
    });
    if (!resultDel.result) {
        const msg = parseInt(resultDel.message, 10);
        showBubble(i18n.lookUp("modal_content_fail")[msg].message, "red", "#fff");
        return false;
    } else {
        if (!isBatch) {
            showBubble(i18n.lookUp("modal_content_success")[0].message, "blue", "#fff");
        }
        // 从数组中删除
        currentManagePhotosList.splice(index, 1);
        // 更新分页表格
        renderManagePhotosPage();
        return true;
    }
}

// Stream the selected original images to the browser as a ZIP download.
async function downloadSelectedPhotos() {
    const photoIds = Array.from(
        document.querySelectorAll('input[name="input_manage_photo"]:checked')
    ).map((element) => Number(element.value));

    if (photoIds.length === 0) {
        return;
    }

    const params = new URLSearchParams({
        adminUid: String(uid),
        adminToken: token,
        proj: currentManageProjId
    });
    photoIds.forEach((photoId) => {
        params.append("list", String(photoId));
    });

    const downloadEl = document.createElement("a");
    downloadEl.href = `${url}/api/data_manager/download_images?${params.toString()}`;
    downloadEl.download = "";
    downloadEl.hidden = true;
    document.body.appendChild(downloadEl);
    downloadEl.click();
    downloadEl.remove();

    showBubble(
        i18n.lookUp("download_selected_photos_started"),
        "blue",
        "#fff"
    );
}

// 删除选中照片
async function deleteSelectedPhotos() {
    const arr = Array.from(document.querySelectorAll('input[name="input_manage_photo"]:checked'))
        .map(el => Number(el.value));
    const length = arr.length;
    if (length === 0) {
        return;
    }

    const confirm = await openModal(
        i18n.lookUp("modal_content_confirm")[4].title,
        i18n.lookUp("modal_content_confirm")[4].message
    );
    if (!confirm) {
        return;
    }

    const param = {
        ids: arr,
        adminUid: uid,
        adminToken: token
    }
    const result = await postApi(url + "/api/proj/delete_photos", param);
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        )
        return;
    }
    const affected = result.data;
    showBubble(i18n.lookUp("successful_rows") + affected, "blue", "#fff");
    const authorInputEl = document.querySelector('#manageGallery input[name="input_author_manage_photos"]');
    let author = authorInputEl.value.trim();
    if (!author || author === "") {
        author = null;
    }
    await loadManagePhotosList(author);
}

// Delete every score submitted by one user in the current project.
async function deleteUserScores() {
    const uidInputEl = document.getElementById("deleteScoreUid");
    const targetUidText = uidInputEl?.value.trim() || "";

    // The backend accepts a Java Integer, so reject malformed and out-of-range UIDs first.
    if (!/^\d+$/.test(targetUidText)) {
        await openModal(
            i18n.lookUp("modal_content_fail")[7].title,
            i18n.lookUp("modal_content_fail")[7].message
        );
        return;
    }

    const targetUid = Number(targetUidText);
    if (!Number.isInteger(targetUid) || targetUid < 0 || targetUid > 2147483647) {
        await openModal(
            i18n.lookUp("modal_content_fail")[7].title,
            i18n.lookUp("modal_content_fail")[7].message
        );
        return;
    }

    const firstConfirm = await openModal(
        i18n.lookUp("modal_content_confirm")[4].title,
        i18n.lookUp("modal_content_confirm")[4].message
    );
    if (!firstConfirm) {
        return;
    }

    const secondConfirm = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message
    );
    if (!secondConfirm) {
        return;
    }

    const result = await postApi(url + "/api/review/delete_score_all", {
        proj: currentManageProjId,
        uid: targetUid,
        adminUid: uid,
        adminToken: token
    });

    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return;
    }

    showBubble(
        `${i18n.lookUp("deleted_score_count")}${result.data ?? 0}`,
        "blue",
        "#fff"
    );
    uidInputEl.value = "";
}

// 筛选
async function filterManagePhotos() {
    const authorInputEl = document.querySelector('#manageGallery input[name="input_author_manage_photos"]');
    let author = authorInputEl.value.trim();
    if (!author || author === "") {
        author = null;
    }
    await loadManagePhotosList(author);
}
