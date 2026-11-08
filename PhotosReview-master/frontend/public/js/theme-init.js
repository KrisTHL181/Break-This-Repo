(() => {
    try {
        document.documentElement.classList.toggle('dark', localStorage.getItem('darkmode') === '1');
    } catch (error) {
        document.documentElement.classList.remove('dark');
    }
})();
