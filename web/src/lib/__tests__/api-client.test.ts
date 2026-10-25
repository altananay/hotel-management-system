// @vitest-environment node
import { delay, HttpResponse, http } from "msw";
import { setupServer } from "msw/node";
import { z } from "zod";
import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { apiGet } from "@/lib/api-client";

const server = setupServer();
const schema = z.object({ ok: z.boolean() });

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe("apiGet timeout and error mapping (AC-3, B-13)", () => {
  it("shouldAbortAfterTheConfiguredTimeoutAndMapToTimeout", async () => {
    server.use(
      http.get("http://localhost:8081/api/test", async () => {
        await delay(150);
        return HttpResponse.json({ ok: true });
      }),
    );

    await expect(apiGet("/api/test", schema, { timeoutMs: 20 })).rejects.toMatchObject({
      code: "system.timeout",
      status: 0,
    });
  });

  it("shouldMapNetworkFailureWithoutLeakingDetails", async () => {
    server.use(http.get("http://localhost:8081/api/test", () => HttpResponse.error()));

    await expect(apiGet("/api/test", schema)).rejects.toMatchObject({
      code: "system.network_error",
      status: 0,
    });
  });

  it("shouldMapMalformedJsonToInvalidResponse", async () => {
    server.use(
      http.get(
        "http://localhost:8081/api/test",
        () =>
          new HttpResponse("{", {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    await expect(apiGet("/api/test", schema)).rejects.toMatchObject({
      code: "system.invalid_response",
    });
  });

  it("shouldMapSchemaFailureToInvalidResponse", async () => {
    server.use(
      http.get("http://localhost:8081/api/test", () => HttpResponse.json({ ok: "nope" })),
    );

    await expect(apiGet("/api/test", schema)).rejects.toMatchObject({
      code: "system.invalid_response",
    });
  });

  it("shouldPreserveProblemCodeForNonOkResponses", async () => {
    server.use(
      http.get("http://localhost:8081/api/test", () =>
        HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "stack trace must not leak",
            instance: "/api/test",
            code: "system.internal_error",
            requestId: "0199f2c0-0000-7000-8000-000000000001",
          },
          { status: 500, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );

    await expect(apiGet("/api/test", schema)).rejects.toMatchObject({
      code: "system.internal_error",
      status: 500,
    });
  });

  it("shouldReturnParsedDataForAValidResponse", async () => {
    server.use(
      http.get("http://localhost:8081/api/test", () => HttpResponse.json({ ok: true })),
    );

    await expect(apiGet("/api/test", schema)).resolves.toEqual({ ok: true });
  });
});
