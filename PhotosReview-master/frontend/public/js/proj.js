const user_uid = getCookie('review_uid');
const user_token = getCookie('review_token');
const mode = getUrlGet('type');
const proj = getUrlGet('proj');
let projName;

function getReviewPhotoUrl(photoName) {
    return url + '/photo/review/' + encodeURIComponent(proj) + '/' + encodeURIComponent(photoName + '.webp');
}

// 设置类
class classSetting {
    constructor() {
        let settings_cookie = getCookie('review_settings');
        if (!settings_cookie) {
            this.batchOperationList = false;
            this.batchOperationRange = false;
            this.lockContent = false; // 锁定内容
            this.autoNext = false; // 自动切换下一张
            this.autoCommit = false; //评分自动提交
            this.adjustImgParamIndividually = false; // 图片参数单独调整
        } else {
            let settings_obj = JSON.parse(settings_cookie);
            this.batchOperationList = settings_obj.batchOperationList;
            this.batchOperationRange = settings_obj.batchOperationRange;
            this.lockContent = settings_obj.lockContent;
            this.autoNext = settings_obj.autoNext;
            this.autoCommit = settings_obj.autoCommit;
            this.adjustImgParamIndividually = settings_obj.adjustImgParamIndividually;
        }

        document.getElementById('batchOperationList').addEventListener('change', () => this.toggleBatchOperationList());
        document.getElementById('batchOperationRange').addEventListener('change', () => this.toggleBatchOperationRange());
        document.getElementById('lockContent').addEventListener('change', () => this.toggleLockContent());
        document.getElementById('autoNext').addEventListener('change', () => this.toggleAutoNext());
        document.getElementById('autoCommit').addEventListener('change', () => this.toggleAutoCommit());
        document.getElementById('adjustImgParamIndividually').addEventListener('change', () => this.toggleAdjustImgParamIndividually());

        this.saveSettings();
        this.updateUI();
    }

    toggleBatchOperationList() {
        this.batchOperationList = !this.batchOperationList;
        console.log('批量操作（列表）:', this.batchOperationList);

        if (this.batchOperationList) {
            document.getElementById('batchOperationRange').disabled = true; // 互斥
            document.getElementById('batchOperationRange').check = false; // 互斥
            this.batchOperationRange = false; // 互斥
            this.saveSettings();
        }
        else {
            document.getElementById('batchOperationRange').disabled = false; // 互斥
            this.saveSettings();
        }
    }
    toggleBatchOperationRange() {
        this.batchOperationRange = !this.batchOperationRange;
        console.log('批量操作（范围）:', this.batchOperationRange);

        if (this.batchOperationRange) {
            document.getElementById('batchOperationList').disabled = true; // 互斥
            document.getElementById('batchOperationList').checked = false; // 互
            this.batchOperationList = false; // 互斥
            this.saveSettings();
        }
        else {
            document.getElementById('batchOperationList').disabled = false; // 互斥
            this.saveSettings();
        }
    }
    toggleLockContent() {
        this.lockContent = !this.lockContent;
        console.log('锁定内容:', this.lockContent);
        this.saveSettings();
    }
    toggleAutoNext() {
        this.autoNext = !this.autoNext;
        console.log('自动切换下一张:', this.autoNext);
        this.saveSettings();
    }
    toggleAutoCommit() {
        this.autoCommit = !this.autoCommit;
        console.log('评分自动提交:', this.autoCommit);
        this.saveSettings();
    }
    toggleAdjustImgParamIndividually() {
        this.adjustImgParamIndividually = !this.adjustImgParamIndividually;
        console.log('单独调整图片参数:', this.adjustImgParamIndividually);
        this.saveSettings();
    }

    outputSettings() {
        let arr = {
            batchOperationList: this.batchOperationList,
            batchOperationRange: this.batchOperationRange,
            lockContent: this.lockContent,
            autoNext: this.autoNext,
            autoCommit: this.autoCommit,
            adjustImgParamIndividually: this.adjustImgParamIndividually
        }
        return arr;
    }
    saveSettings() {
        let settings = this.outputSettings();
        // cookie
        setCookie('review_settings', JSON.stringify(settings), 365);
    }
    updateUI() {
        document.getElementById('batchOperationList').checked = this.batchOperationList;
        document.getElementById('batchOperationRange').checked = this.batchOperationRange;
        document.getElementById('lockContent').checked = this.lockContent;
        document.getElementById('autoNext').checked = this.autoNext;
        document.getElementById('autoCommit').checked = this.autoCommit;
        document.getElementById('adjustImgParamIndividually').checked = this.adjustImgParamIndividually;

        // 互斥
        if (this.batchOperationList) {
            document.getElementById('batchOperationRange').disabled = true; // 互斥
        }
        else {
            document.getElementById('batchOperationRange').disabled = false; // 互斥
        }

        if (this.batchOperationRange) {
            document.getElementById('batchOperationList').disabled = true; // 互斥
        }
        else {
            document.getElementById('batchOperationList').disabled = false; // 互斥
        }
    }
}

// 图片列表类
class classPhotoList {
    constructor() {
        this.photoList = [];
        this.page = 0;
    }

    getScore() {
        let checkedRadio = document.querySelector('#scoreRadios input[name="opt"]:checked');
        return checkedRadio ? parseInt(checkedRadio.value) : 0;
    }
    selectRadio(num) {
        const radios = document.querySelectorAll('#scoreRadios input[type="radio"]');
        radios.forEach(radio => {
            radio.checked = (parseInt(radio.value) === num);
        });
    }
    renderScoreRadios(maxScore) {
        const container = document.getElementById("scoreRadios");
        container.innerHTML = "";
        for (let i = 1; i <= maxScore; i++) {
            const label = document.createElement("label");
            label.innerHTML = `<input type="radio" name="opt" value="${i}"><span class="label-box"></span><span>${i}</span>`;
            container.appendChild(label);
        }
    }

    //以photoid为索引获取tempid
    getTempidByPhotoid(photoid) {
        for (let i = 0; i < this.photoList.length; i++) {
            if (this.photoList[i].photoid === photoid) {
                return i;
            }
        }
        return -1; // 如果未找到，返回-1
    }

    // 以tempid闭区间范围获取photoid数组列表
    getPhotoidListByRange(start, end) {
        let photoidList = [];
        for (let i = start; i <= end; i++) {
            if (this.photoList[i]) {
                photoidList.push(this.photoList[i].photoid);
            }
        }
        return photoidList;
    }


    // 更新页面照片
    updatePhoto() {
        const tempid = this.page
        pageView.html_img.src = '';
        pageView.html_img.src = getReviewPhotoUrl(this.photoList[tempid].name);
        //修改数据
        pageView.html_photoid.innerHTML = this.photoList[tempid].photoid;
        pageView.html_tempid.innerHTML = tempid;
        pageView.html_status.innerHTML = this.photoList[tempid].status === true ? i18n.lookUp("done") : i18n.lookUp("undone");
        pageView.html_status.style.color = this.photoList[tempid].status === true ? 'limegreen' : 'red';
        pageView.html_remaining.innerHTML = this.remaining;
        //修改html评分
        if (!settings.lockContent) {
            this.selectRadio(this.photoList[tempid].score);
            pageView.html_note.value = this.photoList[tempid].note || '';
        }
        // 修改画布
        if (settings.adjustImgParamIndividually) {
            pageView.changeCanvasSize(this.photoList[tempid].width_percent, this.photoList[tempid].rotation);
        }
        // 保存位置
        setCookie("review_last_photoid", this.photoList[tempid].photoid, 365);
    }

    // 更新数组数据
    updateArray(id, score, note) {
        score = Number(score);
        this.photoList[id].score = score;
        this.photoList[id].note = note;
        this.photoList[id].status = true;
    }

    // 提交
    async submit() {
        // 获取评分
        const score = this.getScore();
        // 获取评论
        const note = pageView.html_note.value.trim();
        // 检查评分
        if (score === 0) {
            showBubble(i18n.lookUp("score_empty"), 'red', '#fff');
            return;
        }
        // 批注检测
        if (note.length > 500) {
            showBubble(i18n.lookUp("modal_content_fail")[6].message, 'red', '#fff');
        }
        // 正则表达式
        const regex = /^\d+(,\d+)*$/;

        // 复审
        if (mode === 'recheck') {
            // 批注检测
            if (note.length === 0) {
                showBubble(i18n.lookUp("modal_content_fail")[32].message, 'red', '#fff');
                return;
            }
            // 获取当前page图片数据
            const this_photoid = this.photoList[this.page].photoid;
            let param = {
                uid: parseInt(user_uid),
                token: user_token,
                photoid: this_photoid,
                score: score,
                note: note
            }
            // 提交
            let obj = await postApi(url + "/api/review/recheck?proj=" + proj, param);
            // 判断结果
            if (!obj.result) {
                const msg = parseInt(obj.message);
                await openModal(
                    i18n.lookUp("modal_content_fail")[msg].title,
                    i18n.lookUp("modal_content_fail")[msg].message
                );
                return;
            }
            showBubble(i18n.lookUp("affected_rows") + "1", 'blue', '#fff');
            // 减少剩余
            if (!this.photoList[this.page].status) {
                this.remaining -= 1;
            }
            // 更新当前页面数据
            this.updateArray(this.page, score, note);
            // 自动切换下一张
            if (settings.autoNext) {
                pageView.nextPage();
            }
            this.updatePhoto();
            return;
        }

        // 批量提交（列表）
        if (settings.batchOperationList) {
            // 获取List数据
            const list = document.getElementById('html_list').value.trim();
            if (!list) {
                showBubble(i18n.lookUp("temp_list_empty"), 'red', '#fff');
                return;
            }
            if (!regex.test(list)) {
                showBubble(i18n.lookUp("temp_list_invalid"), 'red', '#fff');
                return;
            }
            let this_tempid_list = list.split(',').map(item => item.trim());
            console.log(this_tempid_list)
            // 将tempid列表转换为photoid列表
            let this_photoid_list = [];
            for (let i = 0; i < this_tempid_list.length; i++) {
                let tempid = parseInt(this_tempid_list[i]);
                if (isNaN(tempid) || tempid < 0 || tempid >= this.photoList.length) {
                    showBubble("Tempid: " + tempid + i18n.lookUp("out_of_range"), 'red', '#fff');
                    return;
                }
                this_photoid_list.push(this.photoList[tempid].photoid);
            }
            let param = {
                uid: parseInt(user_uid),
                token: user_token,
                proj: proj,
                photoid: this_photoid_list,
                score: score,
                note: note
            }
            // 提交
            let obj = await postApi(url + "/api/review/submit?submit_type=batch&proj=" + proj, param);
            // 判断结果
            if (!obj.result) {
                const msg = parseInt(obj.message);
                await openModal(
                    i18n.lookUp("modal_content_fail")[msg].title,
                    i18n.lookUp("modal_content_fail")[msg].message
                );
                return;
            }
            showBubble(i18n.lookUp("affected_rows") + obj.data, 'blue', '#fff');
            // 更新
            this_photoid_list.forEach((photoid, index) => {
                let tempid = this.getTempidByPhotoid(photoid);
                if (tempid !== -1) {
                    // 减少剩余
                    if (!this.photoList[tempid].status) {
                        this.remaining -= 1;
                    }
                    this.updateArray(tempid, score, note);
                }
            });

        }

        // 批量提交（范围）
        else if (settings.batchOperationRange) {
            // 获取Range数据
            const range = document.getElementById('html_range').value.trim();
            if (!range) {
                showBubble(i18n.lookUp("temp_range_empty"), 'red', '#fff');
                return;
            }
            if (!regex.test(range)) {
                showBubble(i18n.lookUp("temp_range_invalid"), 'red', '#fff');
                return;
            }
            let this_tempid_range = range.split(',').map(item => item.trim());
            // 检查范围格式
            if (this_tempid_range.length !== 2) {
                showBubble(i18n.lookUp("temp_range_invalid"), 'red', '#fff');
                return;
            }
            let start = parseInt(this_tempid_range[0]);
            let end = parseInt(this_tempid_range[1]);
            if (end >= this.photoList.length) {
                showBubble(i18n.lookUp("temp_range_invalid"), 'red', '#fff');
                return;
            }
            // 转为photoid列表
            let this_photoid_list = this.getPhotoidListByRange(start, end);
            let json_photoid_list = JSON.stringify(this_photoid_list);
            let param = {
                uid: parseInt(user_uid),
                token: user_token,
                proj: proj,
                photoid: this_photoid_list,
                score: score,
                note: note
            }
            // 提交
            let obj = await postApi(url + "/api/review/submit?submit_type=batch&proj=" + proj, param);
            // 判断结果
            if (!obj.result) {
                const msg = parseInt(obj.message);
                await openModal(
                    i18n.lookUp("modal_content_fail")[msg].title,
                    i18n.lookUp("modal_content_fail")[msg].message
                );
                return;
            }
            showBubble(i18n.lookUp("affected_rows") + obj.data, 'blue', '#fff');
            // 更新
            this_photoid_list.forEach((photoid, index) => {
                let tempid = this.getTempidByPhotoid(photoid);
                if (tempid !== -1) {
                    // 减少剩余
                    if (!this.photoList[tempid].status) {
                        this.remaining -= 1;
                    }
                    this.updateArray(tempid, score, note);
                }
            });

        }

        // 单次提交
        else {
            // 获取当前page图片数据
            const this_photoid = this.photoList[this.page].photoid;
            let param = {
                uid: parseInt(user_uid),
                token: user_token,
                photoid: this_photoid,
                score: score,
                note: note
            }
            // 提交
            let obj = await postApi(url + "/api/review/submit?submit_type=single&proj=" + proj, param);
            // 判断结果
            if (!obj.result) {
                const msg = parseInt(obj.message);
                await openModal(
                    i18n.lookUp("modal_content_fail")[msg].title,
                    i18n.lookUp("modal_content_fail")[msg].message
                );
                return;
            }
            showBubble(i18n.lookUp("affected_rows") + "1", 'blue', '#fff');
            // 减少剩余
            if (!this.photoList[this.page].status) {
                this.remaining -= 1;
            }
            // 更新当前页面数据
            this.updateArray(this.page, score, note);

            // 自动切换下一张
            if (settings.autoNext) {
                pageView.nextPage();
            }

        }

        this.updatePhoto();

    }

    // 切换模式
    switchMode() {
        if (mode === 'project') {
            go_url('?proj=' + proj + '&type=history', 0);
        } else {
            go_url('?proj=' + proj + '&type=project', 0);
        }
    }

    // 首次加载
    async fetchList() {
        let obj;
        // 复审
        if (mode === 'recheck') {
            obj = await getApi(url + '/api/review/fetch_recheck_list?uid=' + user_uid + '&token=' + user_token + '&proj=' + proj);
        } else {
            obj = await getApi(url + '/api/review/fetch_photo_list?uid=' + user_uid + '&token=' + user_token + '&proj=' + proj);
        }

        if (!obj.result) {
            const msg = parseInt(obj.message);
            await openModal(
                i18n.lookUp("modal_content_fail")[msg].title,
                i18n.lookUp("modal_content_fail")[msg].message
            );
            window.location.href = "javascript:history.back()";
            return;
        }

        // 获取工程mode
        switch (mode) {

            case 'project':
                this.type = 'project';
                // 列表
                this.photoList = obj.data.list.unread;
                // 判断
                if (this.photoList.length === 0) {
                    pageView.rightPane.innerHTML = `
                            <h1>${i18n.lookUp('task_completed')}</h1>
            <p style="text-align: center;">
                <button class="button-long" style="width: 98%;font-size: 12px;" onclick="photoList.switchMode()"><i
                        class="fa-solid fa-square-up-right"></i> <span>${i18n.lookUp('switch_mode')}</span></button>
            </p>
                        `;
                    return;
                }
                pageView.html_mode.innerHTML = i18n.lookUp("project");
                break;

            case 'history':
                this.type = 'history';
                // 列表
                this.photoList = obj.data.list.read;
                // 判断
                if (this.photoList.length === 0) {
                    pageView.rightPane.innerHTML = `
                            <h1>${i18n.lookUp('no_history_record')}</h1>
            <p style="text-align: center;">
                <button class="button-long" style="width: 98%;font-size: 12px;" onclick="photoList.switchMode()"><i
                        class="fa-solid fa-square-up-right"></i> <span>${i18n.lookUp('switch_mode')}</span></button>
            </p>
                        `;
                    return;
                }
                pageView.html_mode.innerHTML = i18n.lookUp("history");
                break;

            case 'recheck':
                this.type = 'recheck';
                // 列表
                this.photoList = obj.data.list;
                // 判断
                if (this.photoList.length === 0) {
                    pageView.rightPane.innerHTML = `
                            <h1>${i18n.lookUp('no_history_record')}</h1>
                        `;
                    return;
                }
                pageView.html_mode.innerHTML = i18n.lookUp("recheck");
                pageView.hideBatchOperation();
                break;

            default:
                await openModal(
                    i18n.lookUp("error"),
                    i18n.lookUp("unknown_mode")
                );
                window.location.href = "javascript:history.back()";
                return;

        }

        // 渲染评分单选框
        this.renderScoreRadios(obj.data.max || 10);
        // 设置评分快捷键
        shortcutsKey.setScoreShortcut(obj.data.max || 10);

        switch (obj.data.type) {
            case 0:
                pageView.html_proj_name.innerHTML = `${projName} (${i18n.lookUp("review_review_type")})`;
                break;

            case 1:
                pageView.html_proj_name.innerHTML = `${projName} (${i18n.lookUp("screening_review_type")})`;
                break;
        }

        saveCache.list = [];
        this.photoList.forEach(element => {
            // 存入缓存名单
            saveCache.list.push(getReviewPhotoUrl(element.name))
            // 为每个元素添加画布属性
            element.width_percent = 70;
            element.rotation = 0;
        });

        // 总量
        this.all = obj.data.all;
        // 剩余
        this.remaining = obj.data.remaining;
        pageView.html_all.innerHTML = this.all;
        pageView.html_remaining.innerHTML = this.remaining;
        // 获取Cookie中的上一次浏览位置
        let lastPhotoid = parseInt(getCookie("review_last_photoid"));
        // 判断如果存在且有效，则跳转到该位置；否则默认显示第一张
        if (this.getTempidByPhotoid(lastPhotoid) === -1 || !lastPhotoid) {
            this.updatePhoto();
        }
        else {
            pageView.goPageByPhoto(lastPhotoid);
        }
    }
}

//页面类
class classPageView {
    constructor() {
        this.rightPane = document.getElementById('rightPane');
        this.html_proj_name = document.getElementById('html_proj_name');
        this.html_mode = document.getElementById('html_mode');
        this.html_img = document.getElementById('html_img');
        this.html_goPage_1 = document.getElementById('html_goPage_1');
        this.html_goPage_2 = document.getElementById('html_goPage_2');
        this.html_photoid = document.getElementById('html_photoid');
        this.html_tempid = document.getElementById('html_tempid');
        this.html_status = document.getElementById('html_status');
        this.html_all = document.getElementById('html_all');
        this.html_remaining = document.getElementById('html_remaining');
        this.html_score = document.querySelector('#scoreRadios');
        this.html_note = document.getElementById('html_note');
        this.html_list = document.getElementById('html_list');
        this.html_range = document.getElementById('html_range');
        this.img_width_percent = 70;

        this.html_score.addEventListener('change', () => {

            if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                photoList.submit();
            }

        })
    }

    // 清除Edit
    clear() {
        // 清除评分
        const radios = document.querySelectorAll('#scoreRadios input[type="radio"]');
        radios.forEach(radio => {
            radio.checked = false;
        });
        // 清除
        this.html_note.value = '';
    }

    // 图片放大
    zoomIn() {
        if (this.img_width_percent >= 110) {
            showBubble(i18n.lookUp("reached_max_size"), 'red', '#fff');
            return;
        }
        this.img_width_percent += 5;
        this.html_img.style.width = this.img_width_percent + '%';
        // 储存画布数据
        if (settings.adjustImgParamIndividually) {
            photoList.photoList[photoList.page].width_percent = this.img_width_percent;
        }
    }
    // 图片缩小
    zoomOut() {
        if (this.img_width_percent <= 20) {
            showBubble(i18n.lookUp("reached_min_size"), 'red', '#fff');
            return;
        }
        this.img_width_percent -= 5;
        this.html_img.style.width = this.img_width_percent + '%';
        // 储存画布数据
        if (settings.adjustImgParamIndividually) {
            photoList.photoList[photoList.page].width_percent = this.img_width_percent;
        }
    }

    // 旋转照片90度
    rotate() {
        let currentRotation = this.html_img.style.transform.replace(/[^0-9]/g, '');
        if (!currentRotation) currentRotation = 0;
        let newRotation = (parseInt(currentRotation) + 90) % 360;
        this.html_img.style.transform = `rotate(${newRotation}deg)`;
        // 储存画布数据
        if (settings.adjustImgParamIndividually) {
            photoList.photoList[photoList.page].rotation = newRotation;
        }
    }

    // 下一页
    nextPage() {
        if (photoList.page < photoList.photoList.length - 1) {
            photoList.page++;
            photoList.updatePhoto();
        } else {
            showBubble(i18n.lookUp("reached_bottom"), 'red', '#fff');
        }
    }
    // 上一页
    prevPage() {
        if (photoList.page > 0) {
            photoList.page--;
            photoList.updatePhoto();
        } else {
            showBubble(i18n.lookUp("reached_top"), 'red', '#fff');
        }
    }

    // 通过tempid切换图片
    goPageByTemp(id) {
        if (id > photoList.photoList.length - 1 || id < 0) {
            showBubble(i18n.lookUp("no_this_tempid"), 'red', '#fff');
        }
        else {
            photoList.page = id;
            photoList.updatePhoto()
        }
    }

    // 通过photoid切换图片
    goPageByPhoto(id) {
        id = photoList.getTempidByPhotoid(id);
        if (id === -1) {
            showBubble(i18n.lookUp("no_this_photoid"), 'red', '#fff');
            return;
        }
        console.log(id);
        this.goPageByTemp(id)
    }

    // 直接改变画布
    changeCanvasSize(width_percent, rotation) {
        this.img_width_percent = width_percent;
        this.html_img.style.width = this.img_width_percent + '%';
        this.html_img.style.transform = `rotate(${rotation}deg)`;
    }

    // 隐藏批量操作
    hideBatchOperation() {
        document.getElementById("batch_widget").style.display = "none";
        document.getElementById('batchOperationList').disabled = true;
        document.getElementById('batchOperationRange').disabled = true;
    }
}

// 快捷键类
class classShortcutsKey {
    constructor() {
        this["arrowleft"] = [
            () => pageView.prevPage()
        ];
        this["arrowright"] = [
            () => pageView.nextPage()
        ];
        this["arrowup"] = [
            () => pageView.zoomIn()
        ];
        this["arrowdown"] = [
            () => pageView.zoomOut()
        ];
        this["r"] = [
            () => pageView.rotate()
        ];
        this["1"] = [
            () => {
                photoList.selectRadio(1);
                if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                    photoList.submit();
                }
            }
        ];
        this["2"] = [
            () => {
                photoList.selectRadio(2);
                if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                    photoList.submit();
                }
            }
        ];
        this["3"] = [
            () => {
                photoList.selectRadio(3);
                if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                    photoList.submit();
                }
            }
        ];
        this["4"] = [
            () => {
                photoList.selectRadio(4);
                if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                    photoList.submit();
                }
            }
        ];
        this["enter"] = [
            () => {
                photoList.submit();
            }
        ]

        document.addEventListener("keydown", (e) => {
            const tag = e.target.tagName.toLowerCase();
            const isEditable = e.target.isContentEditable;
            // 如果焦点在输入框、文本域或可编辑区域，就不触发快捷键
            if (tag === "input" || tag === "textarea" || isEditable) {
                return;
            }

            const key = e.key.toLowerCase();
            if (shortcutsKey[key]) {
                e.preventDefault();
                shortcutsKey[key].forEach(fn => fn());
            }
        });
        pageView.html_goPage_1.oninput = () => {
            let a = pageView.html_goPage_1.value;
            a = Number(a);
            if (a) {
                pageView.goPageByPhoto(a)
            }
        }
        pageView.html_goPage_2.oninput = () => {
            let a = pageView.html_goPage_2.value;
            if (a) {
                a = Number(a);
                pageView.goPageByTemp(pageView.html_goPage_2.value)
            }
        }
    }

    debounce(func, wait) {
        let timeout;
        return function () {
            const context = this, args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(context, args), wait);
        };
    }

    // 评分快捷键
    setScoreShortcut(score) {
        if (score>10) {
            score = 10;
        }
        for (let i = 1; i <= score; i++) {
            this[i.toString()] = [
                () => {
                    photoList.selectRadio(i);
                    if (settings.autoCommit && !settings.batchOperationList && !settings.batchOperationRange) {
                        photoList.submit();
                    }
                }
            ];
        }
    }
}

// 缓存类
class classSaveCache {
    constructor() {
        this.list = []
    }

    async cacheImages(urls = this.list) {
        showBubble(i18n.lookUp("loading"), 'blue', '#fff')
        let done = 0, saved = 0, failed = 0;
        const fill = document.getElementById("bar-fill");
        const resultEl = document.getElementById("bar-text");
        const loading_bar = document.getElementById("loading_bar")
        loading_bar.style.display = 'block'
        if (!urls.length) {
            loading_bar.style.display = 'none'
            showBubble(i18n.lookUp("successful_rows") + 0 + " , " + i18n.lookUp("failed_rows") + 0, 'blue', '#fff')
            return;
        }
        for (const photoUrl of urls) {
            try {
                const res = await fetch(photoUrl, { cache: 'force-cache' });
                if (!res.ok && res.type !== 'opaque') throw new Error();
                saved++;
            } catch {
                failed++;
            } finally {
                done++;
                let r = Math.round((done / urls.length) * 100) + '%'
                fill.style.width = r;
                resultEl.innerHTML = 'Loading... ' + r
            }
        }
        loading_bar.style.display = 'none'
        showBubble(i18n.lookUp("successful_rows") + saved + " , " + i18n.lookUp("failed_rows") + failed, 'blue', '#fff')
    }
}

// 实例化设置类
const settings = new classSetting();
// 实例化图片列表类
const photoList = new classPhotoList();
// 实例化页面类
const pageView = new classPageView();
// 实例化快捷键类
const shortcutsKey = new classShortcutsKey();
// 实例化缓存类
const saveCache = new classSaveCache();

// 检查登录情况（2分钟一次）
async function check_login() {
    // 获取结果
    let login_result = await getApi(url + `/api/user/check_token?uid=${user_uid}&token=${user_token}`);
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

// 获取工程信息
async function getProjInfo() {
    const obj = await getApi(url + `/api/proj/get_proj?uid=${user_uid}&token=${user_token}&proj=${proj}`);
    if (!obj.result) {
        const msg = parseInt(obj.message);
        await openModal(
            i18n.lookUp("modal_content_fail")[msg].title,
            i18n.lookUp("modal_content_fail")[msg].message
        );
        document.getElementsByTagName("html")[0].innerHTML = '';
        return;
    }
    projName = obj.data.name;
    return;
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
                    window.location.href = url + "/";
                });
            }
        });

        document.getElementsByTagName("title")[0].innerText = i18n.lookUp("proj") + " - " + projName;

    } else {
        setTimeout(() => {
            loadWebsiteInfo()
        }, 500);
    }
}

(async () => {
    await i18n.init();
    // 检查cookie
    if (!user_uid || !user_token) {
        document.body.innerHTML = '';
        window.location.href = "./login.html";
        return false;
    }
    check_login();

    await getProjInfo();
    await loadWebsiteInfo();
    await photoList.fetchList();
})()