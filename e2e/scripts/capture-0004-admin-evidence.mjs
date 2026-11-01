import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, "../..");
const evidenceRoot = process.env.E2E_EVIDENCE_DIR
  ? path.resolve(repoRoot, process.env.E2E_EVIDENCE_DIR)
  : path.join(repoRoot, "specs", "done", "evidence");
const outDir = path.join(evidenceRoot, "0004");
fs.mkdirSync(outDir, { recursive: true });

const adminWeb = process.env.E2E_ADMIN_WEB_URL ?? "http://localhost:3001";
const adminApi = process.env.ADMIN_API_INTERNAL_URL ?? "http://localhost:8082";
const username = process.env.E2E_ADMIN_SUPERADMIN_USERNAME ?? "superadmin";
const password =
  process.env.E2E_ADMIN_SUPERADMIN_PASSWORD ?? "LocalOnly-Cappadocia-2026!";

const notes = [];
function note(line) {
  notes.push(line);
  console.log(line);
}

async function captureRedirectEvidence(browser) {
  const page = await browser.newPage();
  try {
    await page.goto(`${adminWeb}/`, { waitUntil: "networkidle" });
    await page.waitForURL(/\/login/);
    await page.getByTestId("admin-login-page").waitFor();
    await page.screenshot({
      path: path.join(outDir, "AC-6-admin-redirect-401.png"),
      fullPage: true,
    });
    note("AC-6-admin-redirect-401.png: anonymous / request redirected to /login");
  } finally {
    await page.close();
  }
}

async function captureCsrfFetchFailure(browser) {
  const page = await browser.newPage();
  try {
    await page.route("**/admin-api/auth/csrf", (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Internal Server Error",
          status: 500,
          detail: "CSRF endpoint unavailable.",
          instance: "/admin-api/auth/csrf",
          code: "system.internal_error",
          requestId: "0004-csrf-fetch-fail",
        }),
      }),
    );
    await page.goto(`${adminWeb}/login?returnTo=/`, { waitUntil: "networkidle" });
    await page.getByTestId("admin-login-retry").waitFor();
    await page.screenshot({
      path: path.join(outDir, "AC-6-admin-csrf-fetch-fail.png"),
      fullPage: true,
    });
    note("AC-6-admin-csrf-fetch-fail.png: CSRF fetch failure shows error + retry");
  } finally {
    await page.close();
  }
}

async function captureCsrfRetry(browser) {
  const page = await browser.newPage();
  let loginAttempts = 0;
  try {
    await page.route("**/admin-api/auth/login", async (route) => {
      loginAttempts += 1;
      if (loginAttempts === 1) {
        await route.fulfill({
          status: 403,
          contentType: "application/problem+json",
          body: JSON.stringify({
            type: "about:blank",
            title: "Forbidden",
            status: 403,
            detail: "CSRF validation failed.",
            instance: "/admin-api/auth/login",
            code: "security.csrf_invalid",
            requestId: "0004-csrf-retry",
          }),
        });
        return;
      }
      await route.continue();
    });
    await page.goto(`${adminWeb}/login?returnTo=/`, { waitUntil: "networkidle" });
    await page.getByTestId("admin-login-identifier").fill(username);
    await page.getByTestId("admin-login-password").fill(password);
    await page.getByTestId("admin-login-submit").click();
    await page.getByTestId("admin-home-page").waitFor({ timeout: 15_000 });
    if (loginAttempts !== 2) {
      throw new Error(`Expected one CSRF retry, saw ${loginAttempts} login attempts`);
    }
    await page.screenshot({
      path: path.join(outDir, "AC-6-admin-csrf-retry.png"),
      fullPage: true,
    });
    note("AC-6-admin-csrf-retry.png: first login POST forced 403, retry succeeded");
  } finally {
    await page.close();
  }
}

function applySetCookies(jar, response) {
  for (const raw of response.headers.getSetCookie()) {
    const [pair] = raw.split(";");
    const separator = pair.indexOf("=");
    if (separator > 0) {
      jar[pair.slice(0, separator)] = pair.slice(separator + 1);
    }
  }
}

function cookieHeader(jar) {
  return Object.entries(jar)
    .map(([name, value]) => `${name}=${value}`)
    .join("; ");
}

async function writeScaffoldEvidence() {
  const jar = {};
  const anonymous = await fetch(`${adminApi}/admin-api/system/sample-items`, {
    headers: { Accept: "application/problem+json" },
  });
  const anonymousBody = await anonymous.text();

  const csrfResponse = await fetch(`${adminApi}/admin-api/auth/csrf`);
  applySetCookies(jar, csrfResponse);
  const csrfToken = decodeURIComponent(jar["XSRF-TOKEN"] ?? "");
  const loginResponse = await fetch(`${adminApi}/admin-api/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Origin: adminWeb,
      Cookie: cookieHeader(jar),
      "X-XSRF-TOKEN": csrfToken,
    },
    body: JSON.stringify({ identifier: username, password }),
  });
  applySetCookies(jar, loginResponse);
  const authenticated = await fetch(`${adminApi}/admin-api/system/sample-items`, {
    headers: {
      Accept: "application/problem+json",
      Cookie: cookieHeader(jar),
    },
  });
  const authenticatedBody = await authenticated.text();

  fs.writeFileSync(
    path.join(outDir, "AC-6-scaffold-removed.log"),
    [
      "# AC-6 scaffold removal evidence",
      `captured_at=${new Date().toISOString()}`,
      `GET /admin-api/system/sample-items (anonymous) -> ${anonymous.status}`,
      anonymousBody,
      `POST /admin-api/auth/login -> ${loginResponse.status}`,
      `GET /admin-api/system/sample-items (authenticated) -> ${authenticated.status}`,
      authenticatedBody,
      "",
    ].join("\n"),
    "utf8",
  );
  note(
    `AC-6-scaffold-removed.log: anonymous=${anonymous.status} authenticated=${authenticated.status}`,
  );
}

const browser = await chromium.launch();
try {
  await captureRedirectEvidence(browser);
  await captureCsrfFetchFailure(browser);
  await captureCsrfRetry(browser);
  await writeScaffoldEvidence();
} finally {
  await browser.close();
}

fs.writeFileSync(
  path.join(outDir, "AC-6-csrf-and-gate.log"),
  ["# AC-6 CSRF and session gate evidence", ...notes, ""].join("\n"),
  "utf8",
);
