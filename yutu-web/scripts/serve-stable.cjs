const fs = require("fs");
const http = require("http");
const path = require("path");
const { pipeline } = require("stream");

const HOST = process.env.STABLE_HOST || "127.0.0.1";
const PORT = Number(process.env.STABLE_PORT || 4174);
const BACKEND_HOST = process.env.BACKEND_HOST || "127.0.0.1";
const BACKEND_PORT = Number(process.env.BACKEND_PORT || 8080);
const DIST_DIR = path.resolve(__dirname, "..", "dist");
const INDEX_FILE = path.join(DIST_DIR, "index.html");

const MIME_TYPES = {
  ".css": "text/css; charset=utf-8",
  ".gif": "image/gif",
  ".html": "text/html; charset=utf-8",
  ".ico": "image/x-icon",
  ".jpeg": "image/jpeg",
  ".jpg": "image/jpeg",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml; charset=utf-8",
  ".txt": "text/plain; charset=utf-8",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2"
};

function shouldProxy(urlPath) {
  return urlPath.startsWith("/api/") || urlPath === "/api" || urlPath.startsWith("/uploads/");
}

function sendFile(filePath, response) {
  fs.createReadStream(filePath)
    .on("error", () => {
      response.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("读取静态文件失败");
    })
    .pipe(response);
}

function serveStatic(urlPath, response) {
  const normalizedPath = decodeURIComponent(urlPath.split("?")[0]);
  const relativePath = normalizedPath === "/" ? "/index.html" : normalizedPath;
  const resolvedPath = path.resolve(DIST_DIR, `.${relativePath}`);

  if (!resolvedPath.startsWith(DIST_DIR)) {
    response.writeHead(403, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Forbidden");
    return;
  }

  fs.stat(resolvedPath, (statError, stat) => {
    const hasExtension = path.extname(resolvedPath) !== "";

    if (!statError && stat.isFile()) {
      response.writeHead(200, {
        "Content-Type": MIME_TYPES[path.extname(resolvedPath)] || "application/octet-stream",
        "Cache-Control": "no-cache"
      });
      sendFile(resolvedPath, response);
      return;
    }

    if (hasExtension) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not Found");
      return;
    }

    response.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-cache"
    });
    sendFile(INDEX_FILE, response);
  });
}

function proxyRequest(request, response) {
  const proxy = http.request(
    {
      host: BACKEND_HOST,
      port: BACKEND_PORT,
      path: request.url,
      method: request.method,
      headers: {
        ...request.headers,
        host: `${BACKEND_HOST}:${BACKEND_PORT}`,
        "x-forwarded-host": request.headers.host || `${HOST}:${PORT}`,
        "x-forwarded-proto": "http"
      }
    },
    (proxyResponse) => {
      response.writeHead(proxyResponse.statusCode || 500, proxyResponse.headers);
      pipeline(proxyResponse, response, () => {});
    }
  );

  proxy.on("error", () => {
    response.writeHead(502, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ code: 502, message: "后端服务不可用" }));
  });

  pipeline(request, proxy, () => {});
}

if (!fs.existsSync(INDEX_FILE)) {
  console.error("dist 目录不存在，请先执行 npm run build");
  process.exit(1);
}

http
  .createServer((request, response) => {
    const urlPath = request.url || "/";
    if (shouldProxy(urlPath)) {
      proxyRequest(request, response);
      return;
    }

    serveStatic(urlPath, response);
  })
  .listen(PORT, HOST, () => {
    console.log(`Stable web server running at http://${HOST}:${PORT}`);
    console.log(`Proxying /api and /uploads to http://${BACKEND_HOST}:${BACKEND_PORT}`);
  });
