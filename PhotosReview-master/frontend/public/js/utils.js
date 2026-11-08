/**
 * Sets a cookie with the specified name, value, and expiration in days.
 *
 * @function
 * @param {string} name - The name of the cookie.
 * @param {string} value - The value to store in the cookie.
 * @param {number} days - The number of days until the cookie expires.
 */

function setCookie(name, value, days) {
    const d = new Date();
    d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
    document.cookie = name + "=" + value + ";expires=" + d.toUTCString() + ";path=/";
}

/**
 * Retrieves the value of a cookie by its name.
 *
 * @function
 * @param {string} name - The name of the cookie to retrieve.
 * @returns {string} The value of the cookie, or an empty string if not found.
 */

function getCookie(name) {
    const arr = document.cookie.split(';');
    for (let i = 0; i < arr.length; i++) {
        let c = arr[i].trim();
        if (c.indexOf(name + "=") === 0) return c.substring(name.length + 1, c.length);
    }
    return "";
}
/**
 * Deletes a cookie by setting its expiration date to the past.
 *
 * @function
 * @param {string} name - The name of the cookie to delete.
 */
function deleteCookie(name) {
    document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/";
}

// 跳转页面
function go_url(url, method) {
    if (method === 0) {
        window.location.href = url;
    }
    if (method === 1) {
        window.open(url);
    }
}

function getUrlGet(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name) || '';
}

function generateUuid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// 队列
class Queue {
    constructor() {
        this.queue = [];
        this.running = false;
    }

    add(task) {
        this.queue.push(task);
        this.run();
    }

    async run() {
        if (this.running) return;
        this.running = true;

        while (this.queue.length > 0) {
            const task = this.queue.shift();
            try {
                await task();
            } catch (e) {
                console.error("Task failed:", e);
            }
        }
        // 结束
        await this.runFinalTask();
        this.running = false;
    }

    addFinalTask(task) {
        this.finalTask = task;
    }

    async runFinalTask() {
        if (this.finalTask) {
            try {
                await this.finalTask();
            } catch (e) {
                console.error("Final task failed:", e);
            }
        }
    }
}