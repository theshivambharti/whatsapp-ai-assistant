import express from "express";
import path from "path";
import fs from "fs";
import { fileURLToPath } from "url";
import { createServer as createViteServer } from "vite";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  // Proxy API to send test request (to bypass CORS)
  app.post("/api/test-request", async (req, res) => {
    const { url, headers } = req.body;
    if (!url) {
      return res.status(400).json({ error: "URL is required" });
    }

    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...headers,
        },
        body: JSON.stringify({
          app: "WhatsApp",
          sender: "John",
          phone: "919999999999",
          message: "Hello",
          timestamp: new Date().toISOString()
        }),
      });

      const bodyText = await response.text();
      res.json({
        status: response.status,
        statusText: response.statusText,
        body: bodyText,
      });
    } catch (error) {
      res.status(500).json({
        error: error.message || "Network request failed",
      });
    }
  });

  // Route to download the compiled Android APK
  app.get("/download-apk", (req, res) => {
    let apkPath = path.join(__dirname, "app/build/outputs/apk/debug/app-debug.apk");
    if (!fs.existsSync(apkPath)) {
      apkPath = path.join(__dirname, ".build-outputs/app-debug.apk");
    }
    if (fs.existsSync(apkPath)) {
      res.setHeader("Content-Type", "application/vnd.android.package-archive");
      res.setHeader("Content-Disposition", "attachment; filename=whatsapp-ai-assistant.apk");
      const fileStream = fs.createReadStream(apkPath);
      fileStream.pipe(res);
    } else {
      res.status(404).send(`
        <html>
          <head>
            <title>APK Not Built Yet</title>
            <style>
              body { font-family: system-ui, sans-serif; text-align: center; padding: 50px; background: #0b141a; color: white; }
              a { color: #25d366; text-decoration: none; font-weight: bold; }
              .card { background: #111b21; padding: 30px; border-radius: 12px; display: inline-block; max-width: 500px; box-shadow: 0 4px 12px rgba(0,0,0,0.3); }
            </style>
          </head>
          <body>
            <div class="card">
              <h2>APK Build in Progress</h2>
              <p>The Android APK is currently compiling or hasn't finished building yet. Please refresh this page in a few seconds.</p>
              <p><a href="/">Go Back to Dashboard</a></p>
            </div>
          </body>
        </html>
      `);
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    // Serve static React web app from dist
    app.use(express.static(path.join(__dirname, "dist")));

    // Fallback for SPA routing
    app.get("*", (req, res) => {
      res.sendFile(path.join(__dirname, "dist", "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server is running on port ${PORT}`);
  });
}

startServer().catch((err) => {
  console.error("Failed to start server:", err);
});
