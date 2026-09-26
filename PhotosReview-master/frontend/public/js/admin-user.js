async function loadManageUserList() {
    const result = await getApi(url + `/api/user/get_user_list_admin?adminUid=${uid}&adminToken=${token}`);
    if (!result.result) {
        return;
    }

    const tbody = document.getElementById("manageUserTableBody");
    if (!tbody) {
        return;
    }
    tbody.innerHTML = "";

    if (!result.data || result.data.length === 0) {
        const emptyTr = document.createElement("tr");
        emptyTr.innerHTML = `<td colspan="4">${i18n.lookUp("no_user")}</td>`;
        tbody.appendChild(emptyTr);
        return;
    }

    result.data.forEach((user) => {
        const tr = document.createElement("tr");
        const operationTd = document.createElement("td");
        const buttonEl = document.createElement("button");
        buttonEl.classList.add("button-common", "user-op-btn");
        buttonEl.innerHTML = i18n.lookUp("operation");

        // 弹窗事件
        buttonEl.addEventListener("click", () => {
            renderUserOpModal(user);
        })

        operationTd.appendChild(buttonEl);
        tr.innerHTML = `
            <td>${user.uid}</td>
            <td>${user.allname}</td>
            <td>${getUserStatusText(user.status)}</td>
        `;
        tr.appendChild(operationTd);
        tbody.appendChild(tr);
    });
}

function getUserStatusText(status) {
    if (status === 0) {
        return i18n.lookUp("admin_user");
    }
    if (status === 2) {
        return i18n.lookUp("banned_user");
    }
    return i18n.lookUp("normal_user");
}
function getManageUserUid() {
    const uidInput = document.querySelector('#manageUser input[name="input_manage_user_uid"]');
    const targetUid = parseInt(uidInput.value, 10);
    if (!targetUid || targetUid < 10000 || targetUid > 999999999) {
        openModal(
            i18n.lookUp("modal_content_fail")[7].title,
            i18n.lookUp("modal_content_fail")[7].message
        );
        return null;
    }
    return targetUid;
}
function getManageUserAllname() {
    const allnameInput = document.querySelector('#manageUser input[name="input_manage_user_allname"]');
    const allname = allnameInput.value.trim();
    if (!allname) {
        openModal(
            i18n.lookUp("modal_content_fail")[11].title,
            i18n.lookUp("modal_content_fail")[11].message
        );
        return null;
    }
    if (allname.length > 10) {
        openModal(
            i18n.lookUp("modal_content_fail")[6].title,
            i18n.lookUp("modal_content_fail")[6].message
        );
        return null;
    }
    return allname;
}
async function registerUser() {
    const targetUid = getManageUserUid();
    if (!targetUid) {
        return;
    }
    const allname =  getManageUserAllname();
    if (!allname) {
        return;
    }
    if (allname.length > 10) {
        showBubble(i18n.lookUp("modal_content_fail")[6].message, 'red', '#fff');
        return;
    }
    const statusEl = document.querySelector('#manageUser input[name="register_user_status"]:checked');
    if (!statusEl) {
        showBubble(i18n.lookUp("modal_content_fail")[11].message, 'red', '#fff');
        return;
    }
    const result = await postApi(url + "/api/user/register", {
        uid: targetUid,
        allname: allname,
        status: parseInt(statusEl.value, 10),
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
        await loadManageUserList();
        document.querySelector('#manageUser input[name="input_manage_user_allname"]').value = "";
        document.querySelector('#manageUser input[name="input_manage_user_uid"]').value = "";
    }
}
async function resetUserPassword(targetUid) {
    if (!targetUid) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message,
    )
    if (!r) {
        return false;
    }
    const result = await postApi(url + "/api/user/reset_password", {
        uid: targetUid,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}
async function renameUsername(targetUid, name) {
    if (!targetUid) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message,
    )
    if (!r) {
        return false;
    }
    if (name.length > 10) {
        showBubble(i18n.lookUp("modal_content_fail")[6].message, 'red', '#fff');
        return false;
    }
    const result = await postApi(url + "/api/user/rename", {
        uid: targetUid,
        name: name,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        showBubble(i18n.lookUp("modal_content_fail")[msg].message, 'red', '#fff');
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}
async function banUser(targetUid) {
    if (!targetUid) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message,
    )
    if (!r) {
        return false;
    }
    const result = await postApi(url + "/api/user/ban_user", {
        uid: targetUid,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}
async function unbanUser(targetUid) {
    if (!targetUid) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message,
    )
    if (!r) {
        return false;
    }
    const result = await postApi(url + "/api/user/unban_user", {
        uid: targetUid,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}
async function setAdmin(targetUid, type) {
    if (!targetUid) {
        return false;
    }
    if (type !== 0 && type !== 1) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[5].title,
        i18n.lookUp("modal_content_confirm")[5].message,
    )
    if (!r) {
        return false;
    }
    const result = await postApi(url + "/api/user/setAdmin", {
        uid: targetUid,
        type: type,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}
async function deleteUser(targetUid) {
    if (!targetUid) {
        return false;
    }
    const r = await openModal(
        i18n.lookUp("modal_content_confirm")[2].title,
        i18n.lookUp("modal_content_confirm")[2].message
    );
    if (!r) {
        return false;
    }

    const result = await postApi(url + "/api/user/delete_user", {
        uid: targetUid,
        adminUid: uid,
        adminToken: token
    });
    const msg = parseInt(result.message, 10);
    if (!result.result) {
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        return false;
    } else {
        await openModal(
            i18n.lookUp("modal_content_success")[0].title,
            i18n.lookUp("modal_content_success")[0].message
        );
        await loadManageUserList();
        return true;
    }
}

async function renderUserOpModal(data) {
    const modal = document.getElementById("userOpModal");
    const overlay = document.getElementById("modalOverlay");
    const bodyEl = document.getElementById("userOpModalBody");
    const closeBtnEl = document.getElementById("userOpCloseBtn");
    const confirmBtnEl = document.getElementById("userOpConfirmBtn");

    if (!modal || !overlay || !bodyEl || !closeBtnEl || !confirmBtnEl) {
        return;
    }

    const closeModal = () => {
        modal.classList.remove("active");
        modalNum--;
        if (modalNum === 0) {
            overlay.classList.remove("active");
        }
        overlay.classList.remove("active");
    }

    // 获取容器
    const body = document.getElementById("userOpModalBody");
    body.innerHTML = ``;

    // 总览模块
    body.innerHTML += `
            <div class="user-op-section">
                <div class="user-op-summary">
                    <div class="user-op-uid">UID ${data.uid}</div>
                    <h4>${data.allname}</h4>
                </div>
            </div>
    `;

    // 修改用户名模块
    const renameEl = document.createElement("div");
    renameEl.classList.add("user-op-section");
    renameEl.innerHTML += `
        <h5>${i18n.lookUp("rename")}</h5>
        <input class="text-input" name="newName" type="text" placeholder="${i18n.lookUp("new_name")}" />
    `
    const renameBtn = document.createElement("button");
    renameBtn.classList.add("button-common");
    renameBtn.innerHTML = i18n.lookUp("submit")
    renameEl.appendChild(renameBtn);

    // 修改用户名
    const renameAddEventListener = async () => {
        renameBtn.innerHTML;
        const name = document.querySelector('#userOpModalBody input[name="newName"]').value;
        if (!name) {
            return;
        }
        const result = await renameUsername(data.uid, name);
        if (result) {
            closeModal();
        }
    }
    bindLoadingButtonEl(renameBtn, renameAddEventListener);

    // 用户操作模块
    const opEl = document.createElement("div");
    opEl.classList.add("user-op-section");
    opEl.innerHTML += `
        <h5>${i18n.lookUp("account_settings")}</h5>
    `
    // 操作按钮列表
    const opListEl = document.createElement("div");
    opListEl.classList.add("user-op-button-list");

    // 重设密码按钮
    const resetPwdBtn = document.createElement("button");
    resetPwdBtn.classList.add("button-common", "user-op-btn");
    resetPwdBtn.innerHTML = i18n.lookUp("reset_password_default");
    // 封禁/解封用户按钮
    const banOrUnbanBtn = document.createElement("button");
    banOrUnbanBtn.classList.add("button-common", "user-op-btn");
    banOrUnbanBtn.innerHTML = data.status === 2 ? i18n.lookUp("unban_user") : i18n.lookUp("ban_user");
    // 设置/撤去管理员按钮
    const setOrRemoveAdminBtn = document.createElement("button");
    setOrRemoveAdminBtn.classList.add("button-common", "user-op-btn");
    if (data.status === 0) {
        setOrRemoveAdminBtn.innerHTML = i18n.lookUp("remove_admin");
    } else if (data.status === 1) {
        setOrRemoveAdminBtn.innerHTML = i18n.lookUp("set_admin");
    } else {
        setOrRemoveAdminBtn.innerHTML = i18n.lookUp("set_admin");
        setOrRemoveAdminBtn.disabled = true;
    }
    // 删除用户按钮
    const deleteBtn = document.createElement("button");
    deleteBtn.classList.add("button-common", "user-op-btn", "del");
    deleteBtn.innerHTML = i18n.lookUp("delete");
    // 添加
    opListEl.appendChild(resetPwdBtn);
    opListEl.appendChild(banOrUnbanBtn);
    opListEl.appendChild(setOrRemoveAdminBtn);
    opListEl.appendChild(deleteBtn);
    opEl.appendChild(opListEl);

    // 添加事件
    const resetPwdAddEventListener = async () => {
        const result = await resetUserPassword(data.uid);
        if (result) {
            closeModal();
        }
    }
    const banOrUnbanAddEventListener = async () => {
        let result;
        if (data.status === 2) {
            result = await unbanUser(data.uid);
        } else {
            result = await banUser(data.uid);
        }
        if (result) {
            closeModal();
        }
    }
    const setAdminAddEventListener = async () => {
        let result;
        if (data.status === 0) {
            result = await setAdmin(data.uid, 1);
        } else if (data.status === 1) {
            result = await setAdmin(data.uid, 0);
        } else {
            result = false;
        }
        if (result) {
            closeModal();
        }
    }
    const deleteAddEventListener = async () => {
        const result = await deleteUser(data.uid);
        if (result) {
            closeModal();
        }
    }
    bindLoadingButtonEl(resetPwdBtn, resetPwdAddEventListener);
    bindLoadingButtonEl(banOrUnbanBtn, banOrUnbanAddEventListener);
    bindLoadingButtonEl(setOrRemoveAdminBtn, setAdminAddEventListener);
    bindLoadingButtonEl(deleteBtn, deleteAddEventListener);

    body.appendChild(renameEl);
    body.appendChild(opEl);

    closeBtnEl.onclick = closeModal;
    confirmBtnEl.onclick = closeModal;
    modal.classList.add("active");
    overlay.classList.add("active");
    modalNum++;
}