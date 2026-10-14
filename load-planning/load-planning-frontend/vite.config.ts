import {defineConfig} from "vite";
import tailwindcss from "@tailwindcss/vite";

import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

const API = process.env.API_URL ?? "http://localhost:8081";

export default defineConfig({
    plugins: [
        tailwindcss(),
    ],
    base: "./",
    server: {
        // the browser talks to the Vite dev server; calls to /api are forwarded to the service, so there is no CORS
        proxy: {
            "/api": {target: API, changeOrigin: true},
        },
    },
    build: {
        rollupOptions: {
            input: {
                main: resolve(__dirname, "index.html"),
                flightsToLoad: resolve(__dirname, "html/flights-to-load.html"),
                cargo: resolve(__dirname, "html/cargo.html"),
            },
        },
        outDir: "../load-planning-service/src/main/resources/static",
        emptyOutDir: true,
    },
});
