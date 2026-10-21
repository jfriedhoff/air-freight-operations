const LIGHT = "corporate";
const DARK = "business";

const themeButton = document.getElementById("theme-btn") as HTMLButtonElement;

/* The theme the user chose, or null: then the page follows the light or dark setting of the system. It is kept in
   a cookie and not in localStorage: a cookie belongs to the host and not to the port, so Flight Operations (:8080)
   and Load Planning (:8081) share the choice. */
function savedTheme(): string | null {
    const match = document.cookie.match(/(?:^|; )theme=(\w+)/);
    return match && (match[1] === LIGHT || match[1] === DARK) ? match[1] : null;
}

function isDark(): boolean {
    const theme = document.documentElement.dataset.theme;
    return theme ? theme === DARK : window.matchMedia("(prefers-color-scheme: dark)").matches;
}

function showTheme(theme: string | null): void {
    if (theme) {
        document.documentElement.dataset.theme = theme;
    }
    themeButton.textContent = isDark() ? "Licht" : "Donker";
}

themeButton.addEventListener("click", () => {
    const theme = isDark() ? LIGHT : DARK;
    document.cookie = `theme=${theme}; path=/; max-age=31536000; SameSite=Lax`;
    showTheme(theme);
});

showTheme(savedTheme());
