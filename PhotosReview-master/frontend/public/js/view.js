const url = window.location.origin;
const rootElement = document.documentElement;

window.onload = async () => {
    const loader = document.getElementById('loading-overlay');
    loader.classList.add('fade-out');
    loader.remove();

    // 夜间模式切换
    const darkToggle = document.getElementById('darkToggle');
    const darkIcon = document.getElementById('darkIcon');
    function setDarkMode(on) {
        rootElement.classList.toggle('dark', on);
        document.body.classList.toggle('dark', on);
        if (on) {
            try{
                changeSpecificFillValues('#f5f5f5', "#282a2b");
                changeSpecificFillValues('#42a5f5', "#247ac0");
            } catch (error) {}
            darkToggle.classList.add('active');
            darkIcon.classList.remove('fa-moon');
            darkIcon.classList.add('fa-sun');
            localStorage.setItem('darkmode', '1');
        } else {
            try{
                changeSpecificFillValues('#282a2b', "#f5f5f5");
                changeSpecificFillValues('#247ac0', "#42a5f5");
            } catch (error) {}
            darkToggle.classList.remove('active');
            darkIcon.classList.remove('fa-sun');
            darkIcon.classList.add('fa-moon');
            localStorage.setItem('darkmode', '0');
        }
    }
    darkToggle.onclick = function () {
        const isDark = rootElement.classList.contains('dark');
        setDarkMode(!isDark);
    };
    const dark = localStorage.getItem('darkmode');
    setDarkMode(dark === '1');

    // language
    const langSelect = document.getElementById('languageSelect');
    langSelect.onclick = function () {
        if (localStorage.getItem("lang") === 'en') {
            i18n.set("zh");
            window.location.reload();
        } else {
            i18n.set("en");
            window.location.reload();
        }
    }
}