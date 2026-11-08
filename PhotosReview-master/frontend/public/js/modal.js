let modalNum = 0;

function openModal(title, text) {
    const modal = document.getElementById('modal');
    const overlay = document.getElementById('modalOverlay');
    const confirmBtn = document.getElementById('confirmBtn');
    const closeBtn = document.getElementById('closeBtn');
    return new Promise((resolve) => {
        // 获取html元素
        const header = modal.querySelector('.modal-header h3');
        const body = modal.querySelector('.modal-body p');
        if (header) header.textContent = title;
        if (body) body.textContent = text;

        modal.classList.add('active');
        overlay.classList.add('active');
        modalNum++;

        const closeModal = () => {
            modal.classList.remove('active');
            modalNum--;
            if (modalNum === 0) {
                overlay.classList.remove("active");
            }

            // 清除事件监听，防止多次触发
            confirmBtn.removeEventListener('click', onConfirm);
            closeBtn.removeEventListener('click', onClose);
        };

        const onConfirm = () => {
            closeModal();
            resolve(true); // 表示用户点击了确定
        };

        const onClose = () => {
            closeModal();
            resolve(false); // 表示用户点击了关闭
        };
        confirmBtn.addEventListener('click', onConfirm);
        closeBtn.addEventListener('click', onClose);
    })
}

// 输入弹窗
function openPrompt(title, text) {
    const overlay = document.getElementById('modalOverlay');
    let modal = document.getElementById('promptModal');

    if (!modal) {
        modal = document.createElement('div');
        modal.className = 'modal modal-prompt';
        modal.id = 'promptModal';
        modal.innerHTML = `
            <div class="modal-header">
                <h3></h3>
                <span class="close-btn" id="promptCloseBtn">&times;</span>
            </div>
            <div class="modal-body">
                <p></p>
                <input class="modal-prompt-input" id="promptInput" type="text" autocomplete="off">
            </div>
            <div class="modal-footer">
                <button class="confirm-btn" id="promptConfirmBtn">OK</button>
            </div>
        `;
        document.body.appendChild(modal);
    }

    const header = modal.querySelector('.modal-header h3');
    const body = modal.querySelector('.modal-body p');
    const input = modal.querySelector('.modal-prompt-input');
    const confirmBtn = modal.querySelector('#promptConfirmBtn');
    const closeBtn = modal.querySelector('#promptCloseBtn');

    if (!overlay || !header || !body || !input || !confirmBtn || !closeBtn) {
        return Promise.resolve(null);
    }

    header.textContent = title;
    body.textContent = text;
    input.value = '';
    modal.classList.add('active');
    overlay.classList.add('active');
    modalNum++;

    requestAnimationFrame(() => input.focus());

    return new Promise((resolve) => {
        const closePrompt = (value) => {
            modal.classList.remove('active');
            modalNum = Math.max(0, modalNum - 1);
            if (modalNum === 0) {
                overlay.classList.remove('active');
            }

            confirmBtn.removeEventListener('click', onConfirm);
            closeBtn.removeEventListener('click', onClose);
            input.removeEventListener('keydown', onKeydown);
            resolve(value);
        };

        const onConfirm = () => {
            const value = input.value.trim();
            closePrompt(value || null);
        };

        const onClose = () => closePrompt(null);

        const onKeydown = (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                onConfirm();
            } else if (event.key === 'Escape') {
                event.preventDefault();
                onClose();
            }
        };

        confirmBtn.addEventListener('click', onConfirm);
        closeBtn.addEventListener('click', onClose);
        input.addEventListener('keydown', onKeydown);
    });
}

// 气泡弹窗
function showBubble(text, bgcolor = '#fff', fcolor = '#333') {
    const container = document.getElementById('bubbleContainer');
    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    bubble.textContent = text;
    bubble.style.backgroundColor = bgcolor;
    bubble.style.color = fcolor;
    container.appendChild(bubble);

    // 动画结束后移除
    bubble.addEventListener('animationend', () => {
        if (bubble.style.opacity === '0' || getComputedStyle(bubble).opacity === '0') {
            bubble.remove();
        }
    });
}