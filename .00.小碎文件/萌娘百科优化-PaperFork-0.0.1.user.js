// ==UserScript==
// @name         萌娘百科优化
// @namespace
// @homepage
// @version      0.0.8
// @description  for moegirl
// @author
// @match        *://*.moegirl.org.cn/*
// @icon         https://greasyfork.s3.us-east-2.amazonaws.com/t56lsv6uevkishhogpfzb4xqqioe
// @license      MIT
// @grant        GM_addStyle
// @run-at       document-start
// @downloadURL https://update.greasyfork.org/scripts/577883/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91%E4%BC%98%E5%8C%96.user.js
// @updateURL https://update.greasyfork.org/scripts/577883/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91%E4%BC%98%E5%8C%96.meta.js
// ==/UserScript==

(function() {
    'use strict';

    // ============ 自定义背景配置 ============
    const BG_API = 'https://t.alcy.cc/moe';

    // ============ 全局状态 ============
    let appliedSpecificBg = false;   // 条目专属背景是否已应用（缓存）

    // ============ 静态 CSS ============
    GM_addStyle(`
        html,
        body {
            background-color: var(--theme-background-color) !important;
        }

        #moe-global-background,
        #moe-open-in-app {
            display: none !important;
        }

        .n-notification-container,
        .n-message-container {
            display: none !important;
        }

        [class*="stevrhgmNo"],
        [class*="moe-card"][class*="X-"],
        [class*="n-marquee"] {
            display: none !important;
        }

        a[href*="app.moegirl.org.cn"][href*="utm_source=moeskin_header"] {
            display: none !important;
        }

        ins.adsbygoogle-noablate {
            display: none !important;
        }

        /* ============ 自定义背景 ============ */
        html,
        body {
            background-image: url("${BG_API}") !important;
            background-size: cover !important;
            background-position: center center !important;
            background-repeat: no-repeat !important;
            background-attachment: fixed !important;
            background-color: transparent !important;
        }

        /* ============ 外层容器：稍透明 ============ */
        main.moe-flexible-container {
            background-color: rgba(255, 255, 255, 0.28) !important;
            backdrop-filter: blur(8px) saturate(140%);
            -webkit-backdrop-filter: blur(8px) saturate(140%);
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
        }

        /* ============ 正文卡片：更不透明 ============ */
        #moe-article.moe-card {
            background-color: rgba(255, 255, 255, 0.78) !important;
            backdrop-filter: blur(14px) saturate(160%);
            -webkit-backdrop-filter: blur(14px) saturate(160%);
            border-radius: 10px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
        }

        #content.mw-body-container,
        #mw-body {
            background-color: transparent !important;
        }

        /* ============ 侧栏所有卡片 ============ */
        #moe-global-siderail .moe-card {
            background-color: rgba(255, 255, 255, 0.78) !important;
            backdrop-filter: blur(14px) saturate(160%);
            -webkit-backdrop-filter: blur(14px) saturate(160%);
            border-radius: 10px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
        }

        /* ============ 暗色主题适配 ============ */
        @media (prefers-color-scheme: dark) {
            main.moe-flexible-container {
                background-color: rgba(0, 0, 0, 0.28) !important;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
            }
            #moe-article.moe-card,
            #moe-global-siderail .moe-card {
                background-color: rgba(0, 0, 0, 0.7) !important;
                box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
            }
        }

        html[data-theme="dark"] main.moe-flexible-container,
        html.dark main.moe-flexible-container,
        html[class*="dark"] main.moe-flexible-container {
            background-color: rgba(0, 0, 0, 0.28) !important;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        }
        html[data-theme="dark"] #moe-article.moe-card,
        html.dark #moe-article.moe-card,
        html[class*="dark"] #moe-article.moe-card,
        html[data-theme="dark"] #moe-global-siderail .moe-card,
        html.dark #moe-global-siderail .moe-card,
        html[class*="dark"] #moe-global-siderail .moe-card {
            background-color: rgba(0, 0, 0, 0.7) !important;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
        }
    `);

    // ============ 防抖 ============
    function debounce(fn, delay) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    // ============ body 就绪回调（body 可能一开始不存在） ============
    function whenBodyReady(callback) {
        if (document.body) {
            callback();
            return;
        }
        const obs = new MutationObserver(() => {
            if (document.body) {
                obs.disconnect();
                callback();
            }
        });
        obs.observe(document.documentElement, { childList: true, subtree: true });
    }

    // ============ 1. 拦截谷歌脚本 ============
    const googleList = [
        'google-analytics.com',
        'googletagmanager.com',
        'fundingchoicesmessages.google.com',
        'googleads.g.doubleclick.net',
        'pagead2.googlesyndication.com'
    ];

    function removeGoogleScripts() {
        googleList.forEach(key => {
            document.querySelectorAll(`script[src*="${key}"]`).forEach(el => el.remove());
        });
    }

    function watchGoogleScripts() {
        const obs = new MutationObserver(mutations => {
            mutations.forEach(m => {
                m.addedNodes.forEach(node => {
                    if (
                        node.tagName === 'SCRIPT' &&
                        node.src &&
                        googleList.some(k => node.src.includes(k))
                    ) {
                        node.remove();
                    }
                });
            });
        });
        obs.observe(document.documentElement, { childList: true, subtree: true });
    }

    // ============ 2. 删除谷歌 AdSense 全屏广告 ============
    function removeGoogleAds() {
        document.querySelectorAll('ins.adsbygoogle-noablate').forEach(el => el.remove());

        document.querySelectorAll('iframe[id^="aswift"]').forEach(iframe => {
            const src = iframe.src || '';
            const style = iframe.getAttribute('style') || '';
            if (
                src.includes('doubleclick.net') ||
                src.includes('googlesyndication.com') ||
                style.includes('2147483647') ||
                style.includes('100vh')
            ) {
                iframe.closest('ins')?.remove();
                iframe.parentElement?.remove();
            }
        });

        // ⚠️ body 可能还不存在
        if (document.documentElement) document.documentElement.style.overflow = '';
        if (document.body) document.body.style.overflow = '';
    }

    // ============ 3. 自动点击广告关闭按钮 ============
    function autoCloseAd() {
        document.querySelectorAll('div[class^="_01T-L5Cj4R"]').forEach(box => {
            const btn = box.querySelector('a[class^="TAkM8pe4pR"]');
            btn?.click();
        });
    }

    // ============ 4. 清理 推广 / 加载中 广告 ============
    function removeTextAd() {
        const keywords = ['推广', '加载中'];

        document.querySelectorAll('#app a').forEach(a => {
            const t = a.textContent.trim();
            if (keywords.some(k => t.includes(k))) {
                a.closest('div')?.remove();
            }
        });

        document.querySelectorAll('div[class^="stevrhgmNo"] a').forEach(a => {
            if (a.textContent.includes('加载中')) {
                a.closest('div')?.remove();
            }
        });
    }

    // ============ 5. 统一清理 ============
    const doAllClean = debounce(() => {
        removeGoogleScripts();
        removeGoogleAds();
        autoCloseAd();
        removeTextAd();
    }, 200);

    // ============ 6. 检测并应用条目专属背景 ============
    let lastAppliedUrl = '';

    function applyPageSpecificBackground() {
        if (appliedSpecificBg && lastAppliedUrl) return true;

        // 找到所有 moe-img-function，挑“又大又是 commons 图、又不是 logo”的那张
        const candidates = Array.from(document.querySelectorAll('img.moe-img-function'));

        const specificBgEl = candidates.find(img => {
            // 必须来自 commons
            const dataSrc = img.getAttribute('data-src') || '';
            const src = img.getAttribute('src') || '';
            const url = dataSrc || src;
            if (!url) return false;
            if (!/\/moegirl\/commons\//.test(url)) return false;

            // 排除 logo
            if (/logo\.(png|jpe?g|webp|svg)$/i.test(url)) return false;

            // 排除小图（width < 200px 的都不算背景）
            const w = parseInt(img.getAttribute('width') || '0', 10);
            const styleW = parseInt((img.getAttribute('style') || '').match(/width:\s*(\d+)px/)?.[1] || '0', 10);
            const realW = w || styleW;
            if (realW && realW < 200) return false;

            // 排除 z-index:-1 的小装饰
            const style = img.getAttribute('style') || '';
            if (/z-index\s*:\s*-1/.test(style)) return false;

            return true;
        });

        if (!specificBgEl) return false;

        let bgUrl =
            specificBgEl.getAttribute('data-src') ||
            specificBgEl.getAttribute('src') ||
            '';
        if (!bgUrl) return false;

        if (bgUrl === lastAppliedUrl) return true;

        // 隐藏整块角色背景层
        const wrapper =
              specificBgEl.closest('.siderail-character-executed') ||
              specificBgEl.closest('div[data-displaylog]') ||
              specificBgEl.parentElement;
        if (wrapper) wrapper.style.setProperty('display', 'none', 'important');
        specificBgEl.style.setProperty('display', 'none', 'important');

        const bg = `url("${bgUrl}")`;
        if (document.documentElement) {
            document.documentElement.style.setProperty('background-image', bg, 'important');
        }
        if (document.body) {
            document.body.style.setProperty('background-image', bg, 'important');
        }

        appliedSpecificBg = true;
        lastAppliedUrl = bgUrl;
        console.log('[萌娘百科优化] 使用条目专属背景:', bgUrl);
        return true;
    }

    // ============ 7. 设置背景（优先专属图，否则随机 API） ============
    function setCustomBackground() {
        // 先尝试条目专属背景
        if (applyPageSpecificBackground()) return;

        // 没有专属背景，才用随机 API
        const bg = `url("${BG_API}")`;

        // html 一定存在
        if (document.documentElement) {
            document.documentElement.style.setProperty('background-image', bg, 'important');
            document.documentElement.style.setProperty('background-size', 'cover', 'important');
            document.documentElement.style.setProperty('background-position', 'center center', 'important');
            document.documentElement.style.setProperty('background-repeat', 'no-repeat', 'important');
            document.documentElement.style.setProperty('background-attachment', 'fixed', 'important');
            document.documentElement.style.setProperty('background-color', 'transparent', 'important');
        }

        // ⚠️ body 可能还不存在，存在才设置
        if (document.body) {
            document.body.style.setProperty('background-image', bg, 'important');
            document.body.style.setProperty('background-size', 'cover', 'important');
            document.body.style.setProperty('background-position', 'center center', 'important');
            document.body.style.setProperty('background-repeat', 'no-repeat', 'important');
            document.body.style.setProperty('background-attachment', 'fixed', 'important');
            document.body.style.setProperty('background-color', 'transparent', 'important');
        }
    }

    // ============ 8. 启动监听 ============
    function startAdObserver() {
        const obs = new MutationObserver(() => {
            doAllClean();
            applyPageSpecificBackground();
        });
        obs.observe(document.documentElement, { childList: true, subtree: true });
        doAllClean();
    }

    // ============ 初始化 ============
    function init() {
        removeGoogleScripts();
        watchGoogleScripts();

        startAdObserver();

        // 先尝试设置一次背景（此时 body 可能为 null，没关系）
        setCustomBackground();

        // body 出现后补一次（确保 body 背景也被设置上）
        whenBodyReady(() => {
            setCustomBackground();
            doAllClean();
        });

        window.addEventListener('load', () => {
            doAllClean();
            setCustomBackground();
        });

        console.log('[萌娘百科优化] 加载成功');
    }

    init();

})();