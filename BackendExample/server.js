const express = require("express");
const fs = require("fs");
const path = require("path");

const normalDbFile = path.join(__dirname, "db.json");

function readIssues(dbFile) {
  const text = fs.readFileSync(dbFile, "utf8");
  const data = JSON.parse(text);
  return Array.isArray(data.issues) ? data.issues : [];
}

function saveIssues(dbFile, issues) {
  const data = JSON.stringify({ issues }, null, 2);
  fs.writeFileSync(dbFile, data);
}

function isValidIssue(issue) {
  return issue &&
    typeof issue.id === "string" &&
    typeof issue.title === "string" &&
    typeof issue.description === "string" &&
    typeof issue.status === "string" &&
    typeof issue.priority === "string" &&
    typeof issue.createdAt === "number" &&
    typeof issue.updatedAt === "number";
}

function makeApp(dbFile = normalDbFile) {
  const app = express();
  app.use(express.json());

  app.get("/api/issues", (req, res) => {
    res.json(readIssues(dbFile));
  });

  app.post("/api/issues", (req, res) => {
    const issue = req.body;
    if (!isValidIssue(issue)) {
      return res.status(400).json({ message: "Issue data is not valid" });
    }

    const issues = readIssues(dbFile);
    const oldIssue = issues.find((item) => item.id === issue.id);

    // A repeated POST returns the issue that was already saved.
    if (oldIssue) {
      return res.json(oldIssue);
    }

    issues.push(issue);
    saveIssues(dbFile, issues);
    res.status(201).json(issue);
  });

  app.put("/api/issues/:id", (req, res) => {
    const issue = { ...req.body, id: req.params.id };
    if (!isValidIssue(issue)) {
      return res.status(400).json({ message: "Issue data is not valid" });
    }

    const issues = readIssues(dbFile);
    const index = issues.findIndex((item) => item.id === req.params.id);
    if (index === -1) {
      return res.status(404).json({ message: "Issue was not found" });
    }

    issues[index] = issue;
    saveIssues(dbFile, issues);
    res.json(issue);
  });

  app.delete("/api/issues/:id", (req, res) => {
    const issues = readIssues(dbFile);
    const leftIssues = issues.filter((item) => item.id !== req.params.id);

    if (leftIssues.length !== issues.length) {
      saveIssues(dbFile, leftIssues);
    }

    // Repeated deletes also succeed, which makes offline retries safe.
    res.status(204).send();
  });

  app.use((error, req, res, next) => {
    console.error(error);
    res.status(500).json({ message: "The server could not complete the request" });
  });

  return app;
}

if (require.main === module) {
  const port = process.env.PORT || 3000;
  makeApp().listen(port, "0.0.0.0", () => {
    console.log(`Bug Tracker backend is running on port ${port}`);
  });
}

module.exports = { makeApp };
