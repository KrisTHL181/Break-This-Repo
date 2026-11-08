// Get Project List
async function getProj() {
    const result = await getApi(url + `/api/proj/get_proj_list_admin?adminUid=${uid}&adminToken=${token}`);
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return;
    }
    projList = result.data;

    const projListContainerEl = document.getElementById("projListContainer");
    // const projListSCardContainerEl = document.getElementById("projListSCardContainer");
    projListContainerEl.innerHTML = "";
    // projListSCardContainerEl.innerHTML = "";
    projList.forEach((el, index) => {
        /**
         * Project List渲染
         * @Begin
         */
        const cardEl = document.createElement("div");
        cardEl.classList.add("card");
        const backgroundUrl = "/data/proj/" + el.projId + "/icon/" + el.thumbnail;

        const cardBackgroundEl = document.createElement("div");
        cardBackgroundEl.classList.add("card_background");
        cardBackgroundEl.style.cssText = `background: url("${backgroundUrl}") center / cover;`;
        cardEl.appendChild(cardBackgroundEl);

        const cardContentEl = document.createElement("div");
        cardContentEl.style.zIndex = "1";
        const imgEl = document.createElement("img");
        imgEl.src = backgroundUrl;
        cardContentEl.appendChild(imgEl);

        const cardTextEl = document.createElement("div");
        cardTextEl.classList.add("card_text");
        const h3El = document.createElement("h3");
        h3El.innerText = el.name;
        cardTextEl.appendChild(h3El);

        const pEl = document.createElement("p");

        pEl.innerHTML += `<i class="fa-solid fa-folder"></i>`;
        switch (el.type) {
            case 0:
                pEl.innerHTML += i18n.lookUp("review_review_type");
                break;

            case 1:
                pEl.innerHTML += i18n.lookUp("screening_review_type");
                break;

            default:
                pEl.innerHTML += `Unknow`;
                break;
        }
        pEl.innerHTML += `<br>`;

        pEl.innerHTML += `<i class="fa-solid fa-list-check"></i>`;
        switch (el.status) {
            case 0:
                pEl.innerHTML += `<span class="tag color_blue">${i18n.lookUp("pending")}</span>`
                break;

            case 1:
                pEl.innerHTML += `<span class="tag color_green">${i18n.lookUp("progress")}</span>`
                break;

            case 2:
                pEl.innerHTML += `<span class="tag color_orange">${i18n.lookUp("finished")}</span>`
                break;

            case 3:
                pEl.innerHTML += `<span class="tag color_purple">${i18n.lookUp("rechecking")}</span>`
                break;

            case 4:
                pEl.innerHTML += `<span class="tag color_red">${i18n.lookUp("end")}</span>`
                break;

            default:
                pEl.innerHTML += `<span class="tag color_red">Unknown</span>`
                break;
        }
        pEl.innerHTML += `<br>`;

        pEl.innerHTML += `<i class="fa-regular fa-clock"></i>` + el.time;
        pEl.innerHTML += `<br>`;

        pEl.innerHTML += `<i class="fa-solid fa-display"></i>` + (el.display ? i18n.lookUp("show") : i18n.lookUp("hide"));

        cardTextEl.appendChild(pEl);

        const buttonManageEl = document.createElement("button");
        buttonManageEl.innerHTML = `<i class="fa-solid fa-gear"></i> ${i18n.lookUp("manage_project")}`;
        buttonManageEl.addEventListener("click", () => {
            loadManageProj(index, el.projId);
        });
        cardTextEl.appendChild(buttonManageEl);

        const buttonGalleryEl = document.createElement("button");
        buttonGalleryEl.innerHTML = `<i class="fa-solid fa-images"></i> ${i18n.lookUp("manage_photos")}`;
        buttonGalleryEl.addEventListener("click", () => {
            loadManageGallery(index, el.projId);
        });
        cardTextEl.appendChild(buttonGalleryEl);

        const buttonResultEl = document.createElement("button");
        buttonResultEl.innerHTML = `<i class="fa-solid fa-eye"></i> ${i18n.lookUp("view_results")}`;
        buttonResultEl.addEventListener("click", () => {
            loadViewResults(index, el.projId);
        });
        cardTextEl.appendChild(buttonResultEl);

        const buttonDeleteEl = document.createElement("button");
        buttonDeleteEl.style.background = "rgb(220, 38, 38)";
        buttonDeleteEl.innerHTML = `<i class="fa-solid fa-trash"></i> ${i18n.lookUp("delete")}`;
        buttonDeleteEl.addEventListener("click", () => {
            deleteProj(el.projId);
        });
        cardTextEl.appendChild(buttonDeleteEl);

        cardContentEl.appendChild(cardTextEl);
        cardEl.appendChild(cardContentEl);
        projListContainerEl.appendChild(cardEl);
        /**
         * Project List渲染
         * @End
         */
    });

    if (!projList || projList.length === 0) {
        projListContainerEl.innerHTML = `<p style="font-size: 38px;text-align: center;width: 100%;color: var(--color-fg);">${i18n.lookUp("no_project")}</p>`;
    }
}

async function fetchManageProjCount(projId) {
    const result = await getApi(url + `/api/proj/get_proj_count?proj_id=${projId}&adminUid=${uid}&adminToken=${token}`);
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return null;
    }

    return result.data;
}

// Load Manage Project List
async function loadManageProj(index, id, updateHistory = true) {
    if (!projList[index]) {
        openModal(
            i18n.lookUp("modal_content_fail")[0].title,
            i18n.lookUp("modal_content_fail")[0].message
        );
        return;
    }
    currentManageProjId = id;
    goPage("manageProj", updateHistory);

    const projNameEl = document.querySelector('#manageProj input[name="input_project_name"]');
    const reviewDisplayInput = document.querySelectorAll('#manageProj input[name="review_display"]');
    const reviewStatusInput = document.querySelectorAll('#manageProj input[name="review_status"]');
    const proj = projList[index];
    const manageProjNameEl = document.getElementById("manageProjName");
    const manageTotalEl = document.getElementById("manageTotal");

    manageProjNameEl.textContent = proj.name;
    manageTotalEl.textContent = "--";

    projNameEl.value = proj.name;
    if (proj.display === 0) {
        reviewDisplayInput[1].checked = true;
    } else {
        reviewDisplayInput[0].checked = true;
    }
    switch (proj.status) {
        case 0:
            reviewStatusInput[0].checked = true;
            break;

        case 1:
            reviewStatusInput[1].checked = true;
            break;

        case 2:
            reviewStatusInput[2].checked = true;
            break;

        case 3:
            reviewStatusInput[3].checked = true;
            break;

        case 4:
            reviewStatusInput[4].checked = true;
            break;

        default:
            break;
    }

    const total = await fetchManageProjCount(proj.projId);
    if (total !== null) {
        manageTotalEl.textContent = String(total);
    }
}
// Load Manage Distribution List
async function loadManageDist(index, id) {
    if (!projList[index]) {
        openModal(
            i18n.lookUp("modal_content_fail")[0].title,
            i18n.lookUp("modal_content_fail")[0].message
        );
        return;
    }
    // 获取工程总量
    const total = await fetchManageProjCount(projList[index].projId);
    if (total === null) {
        return;
    }

    const distributionContainerEl = document.getElementById("distributionContainer");
    const recheckDistributionContainerEl = document.getElementById("recheckDistributionContainer");
    const manageProjNameEl = document.getElementById("manageProjName");
    const manageTotalEl = document.getElementById("manageTotal");

    currentManageProjId = id;

    manageProjNameEl.innerHTML = projList[index].name;
    manageTotalEl.innerHTML = total;

    // 获取当前项目的task分发列表
    currentPreliminaryTaskList = JSON.parse(projList[index].task);
    currentRecheckTaskList = JSON.parse(projList[index].recheck);

    /**
     * 通过uid和第n项修改first和end
     * @param uid uid
     * @param n 第n项
     * @param first first
     * @param end end
     */
    const editTask = (uid, n, first, end) => {
        const uidStr = String(uid);
        currentPreliminaryTaskList[uidStr][n] = [first, end];
        renderP();
    }
    /**
     * 新增此uid用户一项任务
     * @param uid uid
     * @param first first
     * @param end end
     * @param type 0:初审 1:复审
     */
    const addTask = (uid, first, end, type = 0) => {
        const uidStr = String(uid);
        if (type === 0){
            // 判断是不是第一个
            if (Object.keys(currentPreliminaryTaskList).length === 0) {
            }
            if (currentPreliminaryTaskList[uidStr]) {
                currentPreliminaryTaskList[uidStr].push([first, end]);
            } else {
                currentPreliminaryTaskList[uidStr] = [[first, end]];
            }
            renderP();
        } else if (type === 1) {
            currentRecheckTaskList.push(uidStr);
            renderR();
        } else {
            return;
        }
    }
    /**
     * 删除此uid用户的第n项任务
     * @param uid
     * @param n
     * @param type 0:初审 1:复审
     */
    const delTask = (uid, n, type = 0) => {
        const uidStr = String(uid);
        if (type === 0) {
            if (currentPreliminaryTaskList[uidStr]) {
                currentPreliminaryTaskList[uidStr].splice(n, 1);
                if (currentPreliminaryTaskList[uidStr].length === 0) {
                    delete currentPreliminaryTaskList[uidStr];
                }
                renderP();
            }
        } else if (type === 1) {
            currentRecheckTaskList = currentRecheckTaskList.filter(item => item !== uidStr);
            renderR();
        }
    }
    // 渲染
    const renderP = () => {
        distributionContainerEl.innerHTML = "";

        /**
         * 添加表单
         * @Begin
         */
            // UID
        const addEl = document.createElement("tr");
        const addUidTdEl = document.createElement("td");
        addUidTdEl.classList.add("text-edit");
        const addUidInputEl = document.createElement("input");
        addUidInputEl.classList.add("text-input", "on-distribution");
        addUidInputEl.setAttribute("autocomplete", "off");
        addUidInputEl.placeholder = "UID";
        addUidTdEl.appendChild(addUidInputEl);
        addEl.appendChild(addUidTdEl);

        // first
        const addFirstTdEl = document.createElement("td");
        addFirstTdEl.classList.add("text-edit");
        const addFirstInputEl = document.createElement("input");
        addFirstInputEl.classList.add("text-input", "on-distribution");
        addFirstInputEl.setAttribute("autocomplete", "off");
        addFirstInputEl.placeholder = i18n.lookUp("first");
        addFirstTdEl.appendChild(addFirstInputEl);
        addEl.appendChild(addFirstTdEl);

        // last
        const addLastTdEl = document.createElement("td");
        addLastTdEl.classList.add("text-edit");
        const addLastInputEl = document.createElement("input");
        addLastInputEl.classList.add("text-input", "on-distribution");
        addLastInputEl.setAttribute("autocomplete", "off");
        addLastInputEl.placeholder = i18n.lookUp("last");
        addLastTdEl.appendChild(addLastInputEl);
        addEl.appendChild(addLastTdEl);

        // add按钮
        const addBtnTdEl = document.createElement("td");
        const addBtnEl = document.createElement("span");
        addBtnEl.classList.add("btn", "edit");
        addBtnEl.title = i18n.lookUp("add");
        addBtnEl.innerHTML = `<i class="fa-solid fa-circle-plus"></i>`;
        addBtnTdEl.appendChild(addBtnEl);
        addEl.appendChild(addBtnTdEl);

        distributionContainerEl.appendChild(addEl);
        /**
         * 添加表单
         * @End
         */

        if (Object.keys(currentPreliminaryTaskList).length === 0) {
            const trEl = document.createElement("tr");
            const tdEl = document.createElement("td");
            tdEl.colSpan = 4;
            tdEl.innerHTML = i18n.lookUp("no_task");
            trEl.appendChild(tdEl);
            distributionContainerEl.appendChild(trEl);
        } else {
            // UID的所有任务
            Object.entries(currentPreliminaryTaskList).forEach(([uid, tasks]) => {
                // UID下的每一项任务
                tasks.forEach((task, index) => {
                    // 创建行
                    const trEl = document.createElement("tr");

                    // UID列
                    const uidTdEl = document.createElement("td");
                    uidTdEl.classList.add("text-edit");
                    uidTdEl.innerHTML = uid;
                    uidTdEl.style.cursor = 'default';
                    trEl.appendChild(uidTdEl);

                    // first列
                    const firstTdEl = document.createElement("td");
                    firstTdEl.classList.add("text-edit");
                    firstTdEl.title = i18n.lookUp("dblclick_to_edit");
                    firstTdEl.innerHTML = task[0];
                    // 双击事件
                    firstTdEl.addEventListener("dblclick", () => {
                        const inputEl = document.createElement("input");
                        inputEl.value = task[0];
                        inputEl.classList.add("text-input", "on-distribution");
                        firstTdEl.innerHTML = "";
                        firstTdEl.appendChild(inputEl);
                        inputEl.focus();
                        // 失去焦点事件
                        inputEl.addEventListener("blur", () => {
                            const newValue = parseInt(inputEl.value, 10);
                            if (isNaN(newValue) || newValue < 0 || newValue >= total || newValue > task[1]) {
                                openModal(
                                    i18n.lookUp("modal_content_fail")[17].title,
                                    i18n.lookUp("modal_content_fail")[17].message
                                );
                                firstTdEl.innerHTML = task[0];
                            } else {
                                editTask(uid, index, newValue, task[1]);
                                firstTdEl.innerHTML = String(newValue);
                            }
                        });
                    });
                    trEl.appendChild(firstTdEl);

                    // last列
                    const lastTdEl = document.createElement("td");
                    lastTdEl.classList.add("text-edit");
                    lastTdEl.title = i18n.lookUp("dblclick_to_edit");
                    lastTdEl.innerHTML = task[1];
                    // 双击事件
                    lastTdEl.addEventListener("dblclick", () => {
                        const inputEl = document.createElement("input");
                        inputEl.value = task[1];
                        inputEl.classList.add("text-input", "on-distribution");
                        lastTdEl.innerHTML = "";
                        lastTdEl.appendChild(inputEl);
                        inputEl.focus();
                        // 失去焦点事件
                        inputEl.addEventListener("blur", () => {
                            const newValue = parseInt(inputEl.value, 10);
                            if (isNaN(newValue) || newValue < 0 || newValue >= total || newValue < task[0]) {
                                openModal(
                                    i18n.lookUp("modal_content_fail")[17].title,
                                    i18n.lookUp("modal_content_fail")[17].message
                                );
                                lastTdEl.innerHTML = task[1];
                            } else {
                                editTask(uid, index, task[0], newValue);
                                lastTdEl.innerHTML = String(newValue);
                            }
                        });
                    });
                    trEl.appendChild(lastTdEl);

                    // 删除按钮
                    const delTdEl = document.createElement("td");
                    const delBtnEl = document.createElement("span");
                    delBtnEl.classList.add("btn", "del");
                    delBtnEl.title = i18n.lookUp("delete");
                    delBtnEl.innerHTML = `<i class="fa-solid fa-trash"></i>`;
                    // 删除按钮事件
                    delBtnEl.addEventListener("click",async  () => {
                        let confirm = await openModal(
                            i18n.lookUp("modal_content_confirm")[4].title,
                            i18n.lookUp("modal_content_confirm")[4].message
                        );
                        if (confirm) {
                            delTask(uid, index);
                        }
                    });
                    delTdEl.appendChild(delBtnEl);
                    trEl.appendChild(delTdEl);

                    distributionContainerEl.appendChild(trEl);
                })
            });
        }

        // 注册Add表单
        addBtnEl.addEventListener("click", () => {
            const uid = parseInt(addUidInputEl.value, 10);
            const first = parseInt(addFirstInputEl.value, 10);
            const last = parseInt(addLastInputEl.value, 10);
            if (isNaN(uid) || isNaN(first) || isNaN(last) || uid <= 0 || first < 0 || last < 0 || first > last) {
                showBubble(i18n.lookUp("modal_content_fail")[17].message, 'red', '#fff');
                return;
            }
            if (last >= total) {
                showBubble(i18n.lookUp("modal_content_fail")[18].message, 'red', '#fff');
                return;
            }
            addTask(uid, first, last);
        });
    }

    const renderR = () => {
        recheckDistributionContainerEl.innerHTML = "";

        /**
         * 添加表单
         * @Begin
         */
            // UID
        const addEl = document.createElement("tr");
        const addUidTdEl = document.createElement("td");
        addUidTdEl.classList.add("text-edit");
        const addUidInputEl = document.createElement("input");
        addUidInputEl.classList.add("text-input", "on-distribution");
        addUidInputEl.setAttribute("autocomplete", "off");
        addUidInputEl.placeholder = "UID";
        addUidTdEl.appendChild(addUidInputEl);
        addEl.appendChild(addUidTdEl);

        // add按钮
        const addBtnTdEl = document.createElement("td");
        const addBtnEl = document.createElement("span");
        addBtnEl.classList.add("btn", "edit");
        addBtnEl.title = i18n.lookUp("add");
        addBtnEl.innerHTML = `<i class="fa-solid fa-circle-plus"></i>`;
        addBtnTdEl.appendChild(addBtnEl);
        addEl.appendChild(addBtnTdEl);

        recheckDistributionContainerEl.appendChild(addEl);

        // 注册Add表单
        addBtnEl.addEventListener("click", () => {
            const uid = parseInt(addUidInputEl.value, 10);
            if (isNaN(uid) || uid <= 0) {
                showBubble(i18n.lookUp("modal_content_fail")[17].message, 'red', '#fff');
                return;
            }
            addTask(uid, 0, 0, 1);
        });

        /**
         * 添加表单
         * @End
         */

        if (currentRecheckTaskList.length === 0) {
            const trEl = document.createElement("tr");
            const tdEl = document.createElement("td");
            tdEl.colSpan = 4;
            tdEl.innerHTML = i18n.lookUp("no_task");
            trEl.appendChild(tdEl);
            recheckDistributionContainerEl.appendChild(trEl);
        } else {
            // UID的所有任务
            currentRecheckTaskList.forEach((uid, index) => {
                // 创建行
                const trEl = document.createElement("tr");

                // UID列
                const uidTdEl = document.createElement("td");
                uidTdEl.classList.add("text-edit");
                uidTdEl.innerHTML = uid;
                uidTdEl.style.cursor = 'default';
                trEl.appendChild(uidTdEl);

                // 删除按钮
                const delTdEl = document.createElement("td");
                const delBtnEl = document.createElement("span");
                delBtnEl.classList.add("btn", "del");
                delBtnEl.title = i18n.lookUp("delete");
                delBtnEl.innerHTML = `<i class="fa-solid fa-trash"></i>`;
                // 删除按钮事件
                delBtnEl.addEventListener("click",async  () => {
                    let confirm = await openModal(
                        i18n.lookUp("modal_content_confirm")[4].title,
                        i18n.lookUp("modal_content_confirm")[4].message
                    );
                    if (confirm) {
                        delTask(uid, index, 1);
                    }
                });
                delTdEl.appendChild(delBtnEl);
                trEl.appendChild(delTdEl);

                recheckDistributionContainerEl.appendChild(trEl);
            });
        }
    }

    // 首次渲染
    renderP();
    renderR();
}
async function refreshManageProjectInfo() {
    await getProj();
    const index = projList.findIndex((proj) => proj.projId === currentManageProjId);
    if (index >= 0) {
        await loadManageDist(index, currentManageProjId);
    }
}
// Save Manage Project
async function saveManageProj() {
    const projName = document.querySelector('#manageProj input[name="input_project_name"]').value;
    const reviewDisplayInput = document.querySelector('#manageProj input[name="review_display"]:checked');
    const reviewStatusInput = document.querySelector('#manageProj input[name="review_status"]:checked');
    const fileInput = document.getElementById("imgInputManageThumbnail");


    // 非空
    if (!projName || projName.trim() === "" || !reviewStatusInput || !reviewDisplayInput) {
        showBubble(i18n.lookUp("modal_content_fail")[11].message, 'red', '#fff');
        return;
    }

    // 长度超过限制
    if (projName.length > 100) {
        showBubble(i18n.lookUp("modal_content_fail")[6].message, 'red', '#fff');
        return;
    }

    const reviewDisplay = parseInt(reviewDisplayInput.value, 10);
    const reviewStatus = parseInt(reviewStatusInput.value, 10);
    const param = {
        projId: currentManageProjId,
        name: projName,
        display: reviewDisplay,
        status: reviewStatus,
        adminUid: uid,
        adminToken: token
    };
    const result = await postApiWithFile(url + "/api/proj/update_proj", param, fileInput);
    console.log(result);
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
    } else {
        showBubble(i18n.lookUp("modal_content_success")[0].message, 'blue', '#fff');
        await refreshManageProjectInfo();
    }
}
// Save Manage Distribution
async function saveManageDist() {
    const task = JSON.stringify(currentPreliminaryTaskList);
    const recheck = JSON.stringify(currentRecheckTaskList);
    const param = {
        projId: currentManageProjId,
        task: task,
        recheck: recheck,
        adminUid: uid,
        adminToken: token
    }
    const result = await postApi(url + "/api/proj/update_task", param);
    if (!result.result) {
        const msg = parseInt(result.message, 10);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
    } else {
        showBubble(i18n.lookUp("modal_content_success")[0].message, 'blue', '#fff');
        await refreshManageProjectInfo();
    }
}
// Delete Project
async function deleteProj(id) {
    let confirm = await openModal(
        i18n.lookUp("modal_content_confirm")[1].title,
        i18n.lookUp("modal_content_confirm")[1].message
    );
    if (!confirm) {
        return;
    }

    confirm = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message
    );
    if (!confirm) {
        return;
    }

    const result = await postApi(url + "/api/proj/delete_proj", {
        projId: id,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await getProj();
    }
}

// 在projList中根据projId查找索引
function findProjIndexById(projId) {
    return projList.findIndex((proj) => proj.projId === projId);
}