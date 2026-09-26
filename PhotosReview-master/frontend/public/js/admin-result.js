let currentPreliminaryResult = null;
let currentRecheckPhotos = [];
let currentResultPhotos = [];
let disputePhotosCurrentPage = 1;
let disputePhotosPageSize = 10;
let disputePhotosSortKey = "recheck";
let disputePhotosSortDirection = "desc";
const disputeReasonI18nKeys = {
    large_dispersion: "dispute_reason_large_dispersion",
    large_range: "dispute_reason_large_range",
    polarization: "dispute_reason_polarization",
    outlier: "dispute_reason_outlier"
};

async function loadViewResults(index, id, updateHistory = true) {
    if (!projList[index]) {
        await openModal(
            i18n.lookUp("modal_content_fail")[0].title,
            i18n.lookUp("modal_content_fail")[0].message
        );
        return;
    }

    currentManageProjId = id;
    configureScoreRangeExport(projList[index].max);
    goPage("viewResults", updateHistory);
    resetPreliminaryResultView();

    const cached = readPreliminaryResultCache(id);
    if (!cached) {
        return;
    }

    currentPreliminaryResult = cached;
    renderPreliminaryResult(cached);
    await refreshRecheckList(false);
}

function resultCacheKey(projId) {
    return "preliminaryResult-" + projId;
}

function readPreliminaryResultCache(projId) {
    try {
        const raw = localStorage.getItem(resultCacheKey(projId));
        if (!raw) {
            return null;
        }
        const parsed = JSON.parse(raw);
        if (!parsed
                || parsed.projectId !== projId
                || !Array.isArray(parsed.disputePhotos)
                || !Number.isFinite(Number(parsed.preliminaryMedian))) {
            return null;
        }
        return parsed;
    } catch (error) {
        console.error("Failed to read preliminary result cache:", error);
        return null;
    }
}

function resetPreliminaryResultView() {
    currentPreliminaryResult = null;
    currentRecheckPhotos = [];
    currentResultPhotos = [];
    disputePhotosCurrentPage = 1;
    disputePhotosSortKey = "recheck";
    disputePhotosSortDirection = "desc";
    updateDisputePhotosSortHeaders();

    [
        "resultProjectName",
        "resultScoredTotal",
        "resultDisputeCount",
        "resultDisputeRate",
        "resultPreliminaryAverage",
        "resultPreliminaryMedian"
    ].forEach((id) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = "--";
        }
    });

    const timeEl = document.getElementById("preliminaryResultTime");
    if (timeEl) {
        timeEl.dataset.i18n = "not_fetched";
        timeEl.textContent = i18n.lookUp("not_fetched");
    }
    renderDisputePhotosPage();
}

function formatResultDecimal(value, fractionDigits = 1) {
    const number = Number(value);
    return Number.isFinite(number) ? number.toFixed(fractionDigits) : "--";
}

function renderPreliminaryResult(data) {
    const values = {
        resultProjectName: data.projectName ?? "--",
        resultScoredTotal: Number.isFinite(Number(data.scoredTotal)) ? String(data.scoredTotal) : "--",
        resultDisputeCount: Number.isFinite(Number(data.disputeCount)) ? String(data.disputeCount) : "--",
        resultDisputeRate: formatResultDecimal(data.disputeRate) + "%",
        resultPreliminaryAverage: formatResultDecimal(data.preliminaryAverage, 2),
        resultPreliminaryMedian: formatResultDecimal(data.preliminaryMedian, 2)
    };

    Object.entries(values).forEach(([id, value]) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    });

    const timeEl = document.getElementById("preliminaryResultTime");
    if (timeEl) {
        const date = new Date(data.time);
        if (data.time && !Number.isNaN(date.getTime())) {
            delete timeEl.dataset.i18n;
            timeEl.textContent = date.toLocaleString();
        } else {
            timeEl.dataset.i18n = "not_fetched";
            timeEl.textContent = i18n.lookUp("not_fetched");
        }
    }

    mergeResultPhotos();
}

function getResultApiError(result) {
    const code = parseInt(result.message, 10);
    const failures = i18n.lookUp("modal_content_fail");
    const fallback = {
        title: i18n.lookUp("error"),
        message: result.message || i18n.lookUp("load_failed_retry")
    };
    return Array.isArray(failures) && failures[code] ? failures[code] : fallback;
}

async function fetchPreliminaryResult() {
    if (!currentManageProjId) {
        return;
    }

    const result = await getApi(
        url + "/api/result/preliminary?projId=" + encodeURIComponent(currentManageProjId)
        + "&adminUid=" + uid + "&adminToken=" + encodeURIComponent(token)
    );
    if (!result.result) {
        const error = getResultApiError(result);
        await openModal(error.title, error.message);
        return;
    }

    const data = {
        ...(result.data || {}),
        time: new Date().toISOString()
    };
    try {
        localStorage.setItem(resultCacheKey(currentManageProjId), JSON.stringify(data));
    } catch (error) {
        console.error("Failed to save preliminary result cache:", error);
    }

    currentPreliminaryResult = data;
    renderPreliminaryResult(data);
    await refreshRecheckList(false);
    showBubble(i18n.lookUp("results_fetched"), "blue", "#fff");
}

async function buildFinalResultData() {
    if (!currentManageProjId) {
        return;
    }

    const confirmed = await openModal(
        i18n.lookUp("modal_content_confirm")[6].title,
        i18n.lookUp("modal_content_confirm")[6].message
    );
    if (!confirmed) {
        return;
    }

    const result = await postApi(url + "/api/result/build_overall_result", {
        projId: currentManageProjId,
        adminUid: uid,
        adminToken: token
    });
    if (!result.result) {
        const error = getResultApiError(result);
        await openModal(error.title, error.message);
        return;
    }

    showBubble(i18n.lookUp("final_result_built"), "blue", "#fff");
}

function configureScoreRangeExport(projectMax) {
    const minInput = document.getElementById("exportPhotosMinScore");
    const maxInput = document.getElementById("exportPhotosMaxScore");
    const normalizedMax = Number(projectMax);
    if (!minInput || !maxInput || !Number.isFinite(normalizedMax) || normalizedMax < 1) {
        return;
    }

    minInput.max = String(normalizedMax);
    maxInput.max = String(normalizedMax);
    minInput.value = "1";
    maxInput.value = String(normalizedMax);
}

async function exportPhotosByScoreRange() {
    if (!currentManageProjId) {
        await openModal(i18n.lookUp("error"), i18n.lookUp("missing_project_id"));
        return;
    }

    const project = projList.find((item) => item.projId === currentManageProjId);
    const projectMax = Number(project?.max);
    const minInput = document.getElementById("exportPhotosMinScore");
    const maxInput = document.getElementById("exportPhotosMaxScore");
    const minScore = Number(minInput?.value);
    const maxScore = Number(maxInput?.value);

    if (!Number.isFinite(projectMax)
            || !Number.isFinite(minScore)
            || !Number.isFinite(maxScore)
            || minScore < 1
            || maxScore > projectMax
            || minScore > maxScore) {
        await openModal(
            i18n.lookUp("error"),
            i18n.lookUp("invalid_score_range").replace("{max}", String(projectMax))
        );
        return;
    }

    startAdminFileDownload("/api/result/export_photos_by_score_range", {
        projId: currentManageProjId,
        minScore,
        maxScore,
        uid,
        token
    });
    showBubble(i18n.lookUp("archive_download_started"), "blue", "#fff");
}

async function previewFinalResultPage() {
    if (!currentManageProjId) {
        await openModal(i18n.lookUp("error"), i18n.lookUp("missing_project_id"));
        return;
    }
    go_url(
        url + "/overall_view.html?proj_id=" + encodeURIComponent(currentManageProjId),
        1
    );
}

async function previewPersonalResultPage() {
    if (!currentManageProjId) {
        await openModal(i18n.lookUp("error"), i18n.lookUp("missing_project_id"));
        return;
    }

    const authorInput = document.getElementById("personalResultAuthor");
    const selectedAuthor = authorInput?.value.trim() || "";
    if (!selectedAuthor) {
        await openModal(i18n.lookUp("error"), i18n.lookUp("missing_personal_result_author"));
        authorInput?.focus();
        return;
    }

    go_url(
        url + "/personal_view.html?proj_id=" + encodeURIComponent(currentManageProjId)
        + "&author=" + encodeURIComponent(selectedAuthor),
        1
    );
}

async function refreshRecheckList(showError = true) {
    if (!currentManageProjId) {
        return;
    }

    const result = await getApi(
        url + "/api/proj/get_recheck_list?projId=" + encodeURIComponent(currentManageProjId)
        + "&adminUid=" + uid + "&adminToken=" + encodeURIComponent(token)
    );
    if (!result.result) {
        if (showError) {
            const error = getResultApiError(result);
            await openModal(error.title, error.message);
        }
        return;
    }
    currentRecheckPhotos = Array.isArray(result.data) ? result.data : [];
    mergeResultPhotos();
}

function mergeResultPhotos() {
    const merged = new Map();
    const disputePhotos = currentPreliminaryResult?.disputePhotos || [];

    disputePhotos.forEach((photo) => {
        const photoId = Number(photo.photoid);
        if (Number.isInteger(photoId)) {
            merged.set(photoId, {...photo, photoid: photoId, recheck: false});
        }
    });

    currentRecheckPhotos.forEach((photo) => {
        const photoId = Number(photo.photoid);
        if (!Number.isInteger(photoId)) {
            return;
        }
        merged.set(photoId, {
            ...(merged.get(photoId) || {}),
            ...photo,
            photoid: photoId,
            recheck: true
        });
    });

    currentResultPhotos = Array.from(merged.values());
    renderDisputePhotosPage();
}

function isFinalReviewed(photo) {
    const finalScore = Number(photo.final_score);
    const maxScore = Number(currentPreliminaryResult?.max);
    return Boolean(photo.recheck)
        && Number.isFinite(finalScore)
        && Number.isFinite(maxScore)
        && finalScore >= 1
        && finalScore <= maxScore;
}

function getDisputePhotoStatusRank(photo) {
    if (isFinalReviewed(photo)) {
        return 2;
    }
    return photo.recheck ? 1 : 0;
}

function compareDisputePhotos(left, right) {
    let comparison = 0;

    if (disputePhotosSortKey === "photoid") {
        comparison = Number(left.photoid) - Number(right.photoid);
    } else if (disputePhotosSortKey === "name") {
        comparison = String(left.name || "").localeCompare(
            String(right.name || ""),
            undefined,
            {numeric: true, sensitivity: "base"}
        );
    } else if (disputePhotosSortKey === "disputeIndex") {
        const leftIndex = Number(left.disputeIndex);
        const rightIndex = Number(right.disputeIndex);
        const leftMissing = !Number.isFinite(leftIndex);
        const rightMissing = !Number.isFinite(rightIndex);
        if (leftMissing !== rightMissing) {
            return leftMissing ? 1 : -1;
        }
        if (!leftMissing) {
            comparison = leftIndex - rightIndex;
        }
    } else if (disputePhotosSortKey === "disputeReasons") {
        const leftReasons = Array.isArray(left.disputeReasons) ? left.disputeReasons : [];
        const rightReasons = Array.isArray(right.disputeReasons) ? right.disputeReasons : [];
        comparison = leftReasons.length - rightReasons.length;
        if (comparison === 0) {
            comparison = leftReasons.join(",").localeCompare(rightReasons.join(","));
        }
    } else if (disputePhotosSortKey === "recheck") {
        comparison = getDisputePhotoStatusRank(left) - getDisputePhotoStatusRank(right);
    }

    if (comparison !== 0) {
        return disputePhotosSortDirection === "asc" ? comparison : -comparison;
    }
    return Number(left.photoid) - Number(right.photoid);
}

function getSortedDisputePhotos() {
    return [...currentResultPhotos].sort(compareDisputePhotos);
}

function updateDisputePhotosSortHeaders() {
    document.querySelectorAll("#disputePhotosTable thead th[data-sort-key]").forEach((header) => {
        const isActive = header.dataset.sortKey === disputePhotosSortKey;
        header.setAttribute(
            "aria-sort",
            isActive
                ? (disputePhotosSortDirection === "asc" ? "ascending" : "descending")
                : "none"
        );
    });
}

function createDisputePhotoRow(photo) {
    const row = document.createElement("tr");
    row.dataset.photoId = String(photo.photoid);

    const idCell = document.createElement("td");
    idCell.innerHTML = '<label class="checkbox">'
        + '<input type="checkbox" name="input_dispute_photo" value="' + photo.photoid + '">'
        + '<span class="box"></span>' + photo.photoid
        + '</label>';
    row.appendChild(idCell);

    const thumbnailCell = document.createElement("td");
    if (photo.name) {
        const image = document.createElement("img");
        image.src = "/data/proj/" + currentManageProjId + "/thumbnail/" + encodeURIComponent(photo.name);
        image.alt = i18n.lookUp("thumbnail");
        image.style.cursor = "pointer";
        image.addEventListener("click", () => {
            go_url("/data/proj/" + currentManageProjId + "/img/" + encodeURIComponent(photo.name), 1);
        });
        thumbnailCell.appendChild(image);
    } else {
        thumbnailCell.textContent = "--";
    }
    row.appendChild(thumbnailCell);

    const disputeIndexCell = document.createElement("td");
    const disputeIndex = Number(photo.disputeIndex);
    disputeIndexCell.textContent = Number.isFinite(disputeIndex)
        ? disputeIndex.toFixed(2)
        : "--";
    row.appendChild(disputeIndexCell);

    const disputeReasonsCell = document.createElement("td");
    const disputeReasons = Array.isArray(photo.disputeReasons)
        ? photo.disputeReasons
            .map((reason) => disputeReasonI18nKeys[reason])
            .filter(Boolean)
            .map((i18nKey) => i18n.lookUp(i18nKey))
        : [];
    disputeReasonsCell.textContent = disputeReasons.length > 0 ? disputeReasons.join(", ") : "--";
    row.appendChild(disputeReasonsCell);

    const statusCell = document.createElement("td");
    const status = document.createElement("span");
    const finalReviewed = isFinalReviewed(photo);
    const statusKey = finalReviewed
        ? "final_reviewed"
        : (photo.recheck ? "rechecking" : "disputed");
    const statusColor = finalReviewed
        ? "color_green"
        : (photo.recheck ? "color_purple" : "color_orange");
    status.classList.add("tag", statusColor);
    status.dataset.i18n = statusKey;
    status.textContent = i18n.lookUp(statusKey);
    statusCell.appendChild(status);
    row.appendChild(statusCell);

    const actionCell = document.createElement("td");
    const actionGroup = document.createElement("div");
    actionGroup.classList.add("photo-actions");

    const toggleButton = document.createElement("span");
    const toggleKey = photo.recheck ? "remove_from_recheck" : "include_in_recheck";
    toggleButton.classList.add("btn", photo.recheck ? "del" : "edit");
    toggleButton.dataset.i18nTitle = toggleKey;
    toggleButton.title = i18n.lookUp(toggleKey);
    toggleButton.innerHTML = photo.recheck
        ? '<i class="fa-solid fa-circle-minus"></i>'
        : '<i class="fa-solid fa-circle-plus"></i>';
    toggleButton.addEventListener("click", async () => {
        toggleButton.style.pointerEvents = "none";
        try {
            await setPhotoRecheck(photo, !photo.recheck);
        } finally {
            toggleButton.style.pointerEvents = "";
        }
    });

    const previewButton = document.createElement("span");
    previewButton.classList.add("btn", "edit");
    previewButton.dataset.i18nTitle = "preview";
    previewButton.title = i18n.lookUp("preview");
    previewButton.innerHTML = '<i class="fa-solid fa-eye"></i>';
    previewButton.addEventListener("click", () => {
        document.getElementById("disputePhotosTableBody").querySelectorAll("tr").forEach(el => {
            el.removeAttribute("class");
        })
        row.classList.add("mark");
        previewPhotoReviewData(photo.photoid, 1)}
    );
    actionGroup.appendChild(previewButton);
    actionGroup.appendChild(toggleButton);

    actionCell.appendChild(actionGroup);
    row.appendChild(actionCell);
    return row;
}

async function requestPhotoRecheck(photoId, recheck) {
    return postApi(url + "/api/proj/set_recheck", {
        photoid: photoId,
        projId: currentManageProjId,
        recheck,
        adminUid: uid,
        adminToken: token
    });
}

async function setPhotoRecheck(photo, recheck) {
    const result = await requestPhotoRecheck(photo.photoid, recheck);
    if (!result.result) {
        const error = getResultApiError(result);
        await openModal(error.title, error.message);
        return;
    }
    await refreshRecheckList(false);
    showBubble(i18n.lookUp("recheck_updated"), "blue", "#fff");
}

async function setSelectedDisputePhotosRecheck(recheck) {
    const selectedPhotoIds = Array.from(
        document.querySelectorAll('input[name="input_dispute_photo"]:checked')
    ).map((element) => Number(element.value));

    if (selectedPhotoIds.length === 0) {
        return;
    }

    const failures = [];
    for (const photoId of selectedPhotoIds) {
        const photo = currentResultPhotos.find((item) => Number(item.photoid) === photoId);
        if (!photo || Boolean(photo.recheck) === recheck) {
            continue;
        }
        const result = await requestPhotoRecheck(photoId, recheck);
        if (!result.result) {
            failures.push(result);
        }
    }

    await refreshRecheckList(false);
    if (failures.length > 0) {
        const error = getResultApiError(failures[0]);
        await openModal(error.title, error.message);
        return;
    }
    showBubble(i18n.lookUp("recheck_updated"), "blue", "#fff");
}

function renderDisputePhotosPage() {
    const body = document.getElementById("disputePhotosTableBody");
    const pagination = document.getElementById("disputePhotosPagination");
    const pageNumbers = document.getElementById("disputePhotosPageNumbers");
    const prevButton = document.getElementById("disputePhotosPrevPage");
    const nextButton = document.getElementById("disputePhotosNextPage");
    const pageSizeSelect = document.getElementById("disputePhotosPageSize");
    const selectCurrentPage = document.getElementById("selectCurrentDisputePhotosPage");
    if (!body) {
        return;
    }

    body.innerHTML = "";

    if (selectCurrentPage) {
        selectCurrentPage.checked = false;
        selectCurrentPage.disabled = currentResultPhotos.length === 0;
    }
    if (currentResultPhotos.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 6;
        cell.dataset.i18n = currentPreliminaryResult ? "no_dispute_photos" : "no_result_data";
        cell.textContent = i18n.lookUp(cell.dataset.i18n);
        row.appendChild(cell);
        body.appendChild(row);
        if (pagination) {
            pagination.style.display = "none";
        }
        return;
    }

    const sortedPhotos = getSortedDisputePhotos();
    const totalPages = Math.max(1, Math.ceil(sortedPhotos.length / disputePhotosPageSize));
    disputePhotosCurrentPage = Math.min(Math.max(1, disputePhotosCurrentPage), totalPages);
    const start = (disputePhotosCurrentPage - 1) * disputePhotosPageSize;
    sortedPhotos
        .slice(start, start + disputePhotosPageSize)
        .forEach((photo) => body.appendChild(createDisputePhotoRow(photo)));

    if (!pagination || !pageNumbers || !prevButton || !nextButton) {
        return;
    }
    pagination.style.display = "flex";
    prevButton.disabled = disputePhotosCurrentPage === 1;
    nextButton.disabled = disputePhotosCurrentPage === totalPages;
    pageNumbers.innerHTML = "";
    if (pageSizeSelect) {
        pageSizeSelect.value = String(disputePhotosPageSize);
    }

    getManagePhotosPaginationItems(totalPages, disputePhotosCurrentPage).forEach((page) => {
        const button = document.createElement("button");
        button.type = "button";
        button.classList.add("page-number");
        if (typeof page !== "number") {
            button.classList.add("page-ellipsis");
            button.disabled = true;
            button.textContent = "...";
        } else {
            button.textContent = String(page);
            if (page === disputePhotosCurrentPage) {
                button.classList.add("active");
            }
            button.addEventListener("click", () => {
                disputePhotosCurrentPage = page;
                renderDisputePhotosPage();
            });
        }
        pageNumbers.appendChild(button);
    });
}

function initDisputePhotosPagination() {
    const pagination = document.getElementById("disputePhotosPagination");
    if (!pagination || pagination.dataset.initialized === "true") {
        return;
    }
    pagination.dataset.initialized = "true";

    const selectCurrentPage = document.getElementById("selectCurrentDisputePhotosPage");
    const body = document.getElementById("disputePhotosTableBody");
    selectCurrentPage?.addEventListener("change", () => {
        body?.querySelectorAll('input[name="input_dispute_photo"]')
            .forEach((checkbox) => {
                checkbox.checked = selectCurrentPage.checked;
            });
    });
    body?.addEventListener("change", (event) => {
        if (!event.target.matches('input[name="input_dispute_photo"]') || !selectCurrentPage) {
            return;
        }
        const pageCheckboxes = Array.from(
            body.querySelectorAll('input[name="input_dispute_photo"]')
        );
        selectCurrentPage.checked = pageCheckboxes.length > 0
            && pageCheckboxes.every((checkbox) => checkbox.checked);
    });
    document.getElementById("disputePhotosPrevPage")?.addEventListener("click", () => {
        if (disputePhotosCurrentPage > 1) {
            disputePhotosCurrentPage -= 1;
            renderDisputePhotosPage();
        }
    });
    document.getElementById("disputePhotosNextPage")?.addEventListener("click", () => {
        const totalPages = Math.max(1, Math.ceil(currentResultPhotos.length / disputePhotosPageSize));
        if (disputePhotosCurrentPage < totalPages) {
            disputePhotosCurrentPage += 1;
            renderDisputePhotosPage();
        }
    });
    document.getElementById("disputePhotosPageSize")?.addEventListener("change", (event) => {
        disputePhotosPageSize = Number(event.target.value) || 10;
        disputePhotosCurrentPage = 1;
        renderDisputePhotosPage();
    });

}

function initDisputePhotosSorting() {
    const headers = document.querySelectorAll("#disputePhotosTable thead th[data-sort-key]");
    headers.forEach((header) => {
        if (header.dataset.sortInitialized === "true") {
            return;
        }
        header.dataset.sortInitialized = "true";

        const activateSort = () => {
            const sortKey = header.dataset.sortKey;
            if (disputePhotosSortKey === sortKey) {
                disputePhotosSortDirection = disputePhotosSortDirection === "asc" ? "desc" : "asc";
            } else {
                disputePhotosSortKey = sortKey;
                disputePhotosSortDirection = sortKey === "recheck" ? "desc" : "asc";
            }
            disputePhotosCurrentPage = 1;
            updateDisputePhotosSortHeaders();
            renderDisputePhotosPage();
        };

        header.addEventListener("click", (event) => {
            if (event.target.closest(".checkbox")) {
                return;
            }
            activateSort();
        });
        header.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                activateSort();
            }
        });
    });
    updateDisputePhotosSortHeaders();
}
