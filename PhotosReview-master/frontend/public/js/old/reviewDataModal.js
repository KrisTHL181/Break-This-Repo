function reviewDataText(value, fallback = "-") {
    return value === undefined || value === null || value === "" ? fallback : String(value);
}
function getReviewTypeLabel(projectType) {
    return Number(projectType) === 1
        ? (i18n.lookUp("screening_review_type") || "筛片模式")
        : (i18n.lookUp("review_review_type") || "审片模式");
}
function getReviewEntries(data) {
    if (Number(data.project_type) === 1 && Array.isArray(data.value)) {
        return [{
            reviewer: data.value[0],
            score: data.value[1],
            comment: data.value[2]
        }];
    }

    if (data.value && typeof data.value === "object" && !Array.isArray(data.value)) {
        return Object.entries(data.value).map(([reviewer, item]) => ({
            reviewer,
            score: Array.isArray(item) ? item[0] : "",
            comment: Array.isArray(item) ? item[1] : ""
        }));
    }

    return [];
}
function getReviewAverageScore(entries) {
    const scores = entries
        .map((entry) => Number(entry.score))
        .filter((score) => Number.isFinite(score));
    if (scores.length === 0) {
        return "-";
    }
    const average = scores.reduce((sum, score) => sum + score, 0) / scores.length;
    return Number.isInteger(average) ? String(average) : average.toFixed(1);
}
function getReviewVarianceScore(entries) {
    const scores = entries
        .map((entry) => Number(entry.score))
        .filter((score) => Number.isFinite(score));
    if (scores.length === 0) {
        return "-";
    }
    const average = scores.reduce((sum, score) => sum + score, 0) / scores.length;
    const squaredDifferences = scores.map((score) => Math.pow(score - average, 2));
    const variance = squaredDifferences.reduce((sum, diff) => sum + diff, 0) / squaredDifferences.length;
    return Number.isInteger(variance) ? String(variance) : variance.toFixed(1);
}
function appendReviewDataMeta(parent, label, value) {
    const itemEl = document.createElement("div");
    itemEl.classList.add("review-data-meta-item");

    const labelEl = document.createElement("span");
    labelEl.classList.add("review-data-label");
    labelEl.textContent = label;
    itemEl.appendChild(labelEl);

    const valueEl = document.createElement("span");
    valueEl.classList.add("review-data-value");
    valueEl.textContent = reviewDataText(value);
    itemEl.appendChild(valueEl);

    parent.appendChild(itemEl);
}
function appendReviewStat(parent, label, value) {
    const statEl = document.createElement("div");
    statEl.classList.add("review-data-stat");

    const valueEl = document.createElement("strong");
    valueEl.textContent = reviewDataText(value);
    statEl.appendChild(valueEl);

    const labelEl = document.createElement("span");
    labelEl.textContent = label;
    statEl.appendChild(labelEl);

    parent.appendChild(statEl);
}
function createReviewCommentEl(comment) {
    const commentEl = document.createElement("div");
    commentEl.classList.add("review-data-comment");
    const normalizedComment = comment === undefined || comment === null ? "" : String(comment);
    if (normalizedComment === "") {
        commentEl.classList.add("review-data-empty-comment");
        commentEl.textContent = i18n.lookUp("no_note");
    } else {
        commentEl.textContent = normalizedComment;
    }
    return commentEl;
}
function appendReviewScoreRow(parent, entry) {
    const rowEl = document.createElement("div");
    rowEl.classList.add("review-data-review-card");

    const topEl = document.createElement("div");
    topEl.classList.add("review-data-review-top");

    const reviewerEl = document.createElement("div");
    reviewerEl.classList.add("review-data-reviewer");
    const reviewerLabelEl = document.createElement("span");
    reviewerLabelEl.textContent = "UID";
    reviewerEl.appendChild(reviewerLabelEl);
    const reviewerValueEl = document.createElement("strong");
    reviewerValueEl.textContent = reviewDataText(entry.reviewer);
    reviewerEl.appendChild(reviewerValueEl);
    topEl.appendChild(reviewerEl);

    const scoreEl = document.createElement("div");
    scoreEl.classList.add("review-data-score-pill");
    scoreEl.textContent = i18n.lookUp("score") + ": " + reviewDataText(entry.score);
    topEl.appendChild(scoreEl);

    rowEl.appendChild(topEl);
    rowEl.appendChild(createReviewCommentEl(entry.comment));
    parent.appendChild(rowEl);
}
function renderReviewDataModal(data) {
    const modal = document.getElementById("reviewDataModal");
    const overlay = document.getElementById("modalOverlay");
    const titleEl = document.getElementById("reviewDataModalTitle");
    const bodyEl = document.getElementById("reviewDataModalBody");
    const closeBtnEl = document.getElementById("reviewDataCloseBtn");
    const confirmBtnEl = document.getElementById("reviewDataConfirmBtn");

    if (!modal || !overlay || !titleEl || !bodyEl || !closeBtnEl || !confirmBtnEl) {
        return;
    }

    const closeModal = () => {
        modal.classList.remove("active");
        modalNum--;
        if (modalNum === 0) {
            overlay.classList.remove("active");
        }
    }

    const entries = getReviewEntries(data);
    titleEl.textContent = "Preview";
    bodyEl.innerHTML = "";

    const shellEl = document.createElement("div");
    shellEl.classList.add("review-data-shell");

    const summaryEl = document.createElement("section");
    summaryEl.classList.add("review-data-summary");

    const summaryMainEl = document.createElement("div");
    summaryMainEl.classList.add("review-data-summary-main");

    const photoIdEl = document.createElement("div");
    photoIdEl.classList.add("review-data-photoid");
    photoIdEl.textContent = "Photoid " + reviewDataText(data.photoid);
    summaryMainEl.appendChild(photoIdEl);

    const nameEl = document.createElement("h4");
    nameEl.textContent = reviewDataText(data.name, "Unname");
    summaryMainEl.appendChild(nameEl);

    const authorEl = document.createElement("p");
    authorEl.textContent = reviewDataText(data.author);
    summaryMainEl.appendChild(authorEl);

    summaryEl.appendChild(summaryMainEl);

    const typeEl = document.createElement("div");
    typeEl.classList.add("review-data-type-badge");
    typeEl.textContent = getReviewTypeLabel(data.project_type);
    summaryEl.appendChild(typeEl);
    shellEl.appendChild(summaryEl);

    const statEl = document.createElement("section");
    statEl.classList.add("review-data-stats");
    appendReviewStat(statEl, i18n.lookUp("average"), getReviewAverageScore(entries));
    appendReviewStat(statEl, i18n.lookUp("variance"), getReviewVarianceScore(entries));
    shellEl.appendChild(statEl);

    const metaSectionEl = document.createElement("section");
    metaSectionEl.classList.add("review-data-section");
    const metaTitleEl = document.createElement("h5");
    metaTitleEl.textContent = i18n.lookUp("basic_info");
    metaSectionEl.appendChild(metaTitleEl);

    const metaEl = document.createElement("div");
    metaEl.classList.add("review-data-meta");
    appendReviewDataMeta(metaEl, "Photoid", data.photoid);
    appendReviewDataMeta(metaEl, i18n.lookUp("author"), data.author);
    appendReviewDataMeta(metaEl, i18n.lookUp("file_name"), data.name);
    appendReviewDataMeta(metaEl, i18n.lookUp("proj"), data.proj);
    appendReviewDataMeta(metaEl, i18n.lookUp("project_type"), getReviewTypeLabel(data.project_type));
    metaSectionEl.appendChild(metaEl);
    shellEl.appendChild(metaSectionEl);

    const reviewSectionEl = document.createElement("section");
    reviewSectionEl.classList.add("review-data-section");
    const reviewTitleEl = document.createElement("h5");
    reviewTitleEl.textContent = i18n.lookUp("scoring_details");
    reviewSectionEl.appendChild(reviewTitleEl);

    const listEl = document.createElement("div");
    listEl.classList.add("review-data-review-list");
    if (entries.length === 0) {
        const emptyEl = document.createElement("div");
        emptyEl.classList.add("review-data-no-score");
        emptyEl.textContent = i18n.lookUp("score_empty");
        listEl.appendChild(emptyEl);
    } else {
        entries.forEach((entry) => appendReviewScoreRow(listEl, entry));
    }
    reviewSectionEl.appendChild(listEl);
    shellEl.appendChild(reviewSectionEl);

    bodyEl.appendChild(shellEl);

    closeBtnEl.onclick = closeModal;
    confirmBtnEl.onclick = closeModal;
    modal.classList.add("active");
    overlay.classList.add("active");
    modalNum++;
}