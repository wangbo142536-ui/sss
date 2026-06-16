import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
// Dev-only local SKU import model proxy. It is only mounted by Vite serve and is not bundled into production.
import { localSkuModelImportDevPlugin } from "./dev/skuModelImportDevPlugin.mjs";

export default defineConfig({
  plugins: [vue(), localSkuModelImportDevPlugin()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  }
});
