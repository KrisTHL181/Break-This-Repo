const i18n = (() => {

    function getBrowserLang() {
        return navigator.language.startsWith("zh") ? "zh" : "en";
    }

    let currentLang = localStorage.getItem("lang") || getBrowserLang();
    let dict = {};

    async function loadDict(lang) {
        try {
            const response = await fetch(`./i18n/${lang}.json`);
            if (!response.ok) {
                throw new Error(`Failed to load language file: ${lang}.json`);
            }
            dict = await response.json();
        } catch (error) {
            console.error("Error loading language file:", error);
        }
    }

    function translate() {
        // 普通文本
        document.querySelectorAll("[data-i18n]").forEach(el => {
            const key = el.dataset.i18n;
            if (dict[key]) {
                el.innerText = dict[key];
            }
        });

        // placeholder
        document.querySelectorAll("[data-i18n-placeholder]").forEach(el => {
            const key = el.dataset.i18nPlaceholder;
            if (dict[key]) {
                el.placeholder = dict[key];
            }
        });

        // title
        document.querySelectorAll("[data-i18n-title]").forEach(el => {
            const key = el.dataset.i18nTitle;
            if (dict[key]) {
                el.title = dict[key];
            }
        });
    }

    function lookUp(key) {
        if (dict[key]) {
            return dict[key]
        } else {
            return "NaN";
        }
    }

    async function set(lang) {
        localStorage.setItem("lang", lang);
        await loadDict(lang);
        translate();
    }

    async function init() {
        await loadDict(currentLang);
        translate();
    }

    return {
        init,
        set,
        lookUp
    }
})()