import { buildLocalSkuModelImportPreviewFromBuffer, recognizeProductNamesBySupplierSku } from "./skuModelImportDevCore.mjs";

const ENDPOINT = "/__dev/shop/skus/model-import-preview";
const NAME_ENDPOINT = "/__dev/shop/skus/model-product-names";

function readRequestBuffer(request) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    request.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });
}

function parseMultipartFile(body, contentType = "") {
  const boundary = contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i)?.[1] || contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i)?.[2];
  if (!boundary) throw new Error("MULTIPART_BOUNDARY_MISSING");
  const marker = Buffer.from(`--${boundary}`);
  let offset = body.indexOf(marker);
  while (offset >= 0) {
    const nextOffset = body.indexOf(marker, offset + marker.length);
    if (nextOffset < 0) break;
    const part = body.slice(offset + marker.length, nextOffset);
    offset = nextOffset;
    const headerEnd = part.indexOf(Buffer.from("\r\n\r\n"));
    if (headerEnd < 0) continue;
    const header = part.slice(0, headerEnd).toString("utf8");
    if (!/name="file"/i.test(header)) continue;
    const filename = header.match(/filename="([^"]*)"/i)?.[1] || "upload.xlsx";
    let data = part.slice(headerEnd + 4);
    if (data.slice(0, 2).toString() === "\r\n") data = data.slice(2);
    if (data.slice(-2).toString() === "\r\n") data = data.slice(0, -2);
    return { filename, buffer: data };
  }
  throw new Error("MULTIPART_FILE_MISSING");
}

function writeJson(response, status, payload) {
  response.statusCode = status;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.end(JSON.stringify(payload));
}

function readRequestJson(request) {
  return readRequestBuffer(request).then((body) => (body.length ? JSON.parse(body.toString("utf8")) : {}));
}

export function localSkuModelImportDevPlugin() {
  return {
    name: "local-sku-model-import-dev",
    apply: "serve",
    configureServer(server) {
      server.middlewares.use(ENDPOINT, async (request, response) => {
        if (request.method !== "POST") {
          writeJson(response, 405, { message: "METHOD_NOT_ALLOWED" });
          return;
        }
        try {
          const body = await readRequestBuffer(request);
          const { filename, buffer } = parseMultipartFile(body, request.headers["content-type"] || "");
          const result = await buildLocalSkuModelImportPreviewFromBuffer(buffer, { fileName: filename });
          writeJson(response, 200, { data: result });
        } catch (error) {
          writeJson(response, 500, { message: error?.message || "LOCAL_MODEL_IMPORT_FAILED" });
        }
      });
      server.middlewares.use(NAME_ENDPOINT, async (request, response) => {
        if (request.method !== "POST") {
          writeJson(response, 405, { message: "METHOD_NOT_ALLOWED" });
          return;
        }
        try {
          const payload = await readRequestJson(request);
          const rows = Array.isArray(payload.rows) ? payload.rows : [];
          const result = await recognizeProductNamesBySupplierSku(rows);
          writeJson(response, 200, { data: result });
        } catch (error) {
          writeJson(response, 500, { message: error?.message || "LOCAL_MODEL_NAME_EXTRACTION_FAILED" });
        }
      });
    }
  };
}
