const assert = require("node:assert/strict");
const fs = require("node:fs");
const http = require("node:http");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { makeApp } = require("./server");

function request(port, method, url, body) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: "127.0.0.1",
      port,
      path: url,
      method,
      headers: body ? { "Content-Type": "application/json" } : {},
    }, (res) => {
      let text = "";
      res.on("data", (part) => { text += part; });
      res.on("end", () => resolve({
        status: res.statusCode,
        body: text ? JSON.parse(text) : null,
      }));
    });
    req.on("error", reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

test("runs all issue CRUD routes", async () => {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), "bug-api-"));
  const dbFile = path.join(folder, "db.json");
  fs.writeFileSync(dbFile, JSON.stringify({ issues: [] }));

  const server = makeApp(dbFile).listen(0, "127.0.0.1");
  await new Promise((resolve) => server.once("listening", resolve));
  const port = server.address().port;

  const issue = {
    id: "issue-1",
    title: "Login button does not work",
    description: "Nothing happens after tapping login",
    status: "OPEN",
    priority: "HIGH",
    assignee: null,
    createdAt: 1000,
    updatedAt: 1000
  };

  try {
    assert.equal((await request(port, "POST", "/api/issues", issue)).status, 201);

    const list = await request(port, "GET", "/api/issues");
    assert.equal(list.status, 200);
    assert.equal(list.body.length, 1);

    const changed = { ...issue, status: "RESOLVED", updatedAt: 2000 };
    const update = await request(port, "PUT", "/api/issues/issue-1", changed);
    assert.equal(update.status, 200);
    assert.equal(update.body.status, "RESOLVED");

    assert.equal((await request(port, "DELETE", "/api/issues/issue-1")).status, 204);
    assert.deepEqual((await request(port, "GET", "/api/issues")).body, []);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    fs.rmSync(folder, { recursive: true, force: true });
  }
});
