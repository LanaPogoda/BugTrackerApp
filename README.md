# Bug Tracker App

This is a small Android app for creating and managing bug reports. It uses Kotlin and Jetpack Compose. Room saves issues on the phone, Retrofit talks to the server, and WorkManager tries the upload again when the phone has internet access.

## Features

- Create, view, edit, and delete issues while offline.
- Keep issues after the app closes by saving them in Room.
- Store each offline change in a pending changes table.
- Combine repeated edits so the app does not send the same work more than once.
- Send local changes first, then download the server's issue list.
- Retry failed uploads with WorkManager.
- Use the issue ID as an `Idempotency-Key` so a retry does not create a duplicate issue.

## Run

Install Android Studio, JDK 17, and Android SDK 35. Open the project and wait for Gradle to finish. Run the `app` configuration on an emulator or phone with API 24 or newer.

Set the server URL in `~/.gradle/gradle.properties` (the URL must end in `/`):

```properties
API_BASE_URL=https://bugs.example.com/api/
```

The project is set to use the example backend through the Android emulator. If the backend is stopped, you can still create and edit issues offline. WorkManager will try to sync them later.

## Run the example backend

The `BackendExample` folder contains a small Express server. It saves issues in `BackendExample/db.json`.

```bash
cd BackendExample
npm install
npm start
```

The server runs at `http://localhost:3000`. The Android emulator reaches the same server through `http://10.0.2.2:3000`, which is already set in `gradle.properties`.

Run the backend test with:

```bash
cd BackendExample
npm test
```

## Server contract

The API uses Unix milliseconds for dates. Status and priority values are uppercase, such as `IN_PROGRESS` and `CRITICAL`. The app expects these routes:

```text
GET    /issues             -> complete JSON array of issues
POST   /issues             -> created issue; honors Idempotency-Key
PUT    /issues/{id}        -> updated issue
DELETE /issues/{id}        -> empty success response
```

`GET /issues` must return the full issue list without pagination. If an issue is missing from the response, the app treats it as deleted. A paginated server would need a separate changes feed that includes deleted issue IDs.

The POST route must return the same issue when it receives the same `Idempotency-Key` again. This stops WorkManager from creating a duplicate if the first response gets lost.

## Architecture

```text
Compose UI -> ViewModel -> IssueRepository -> Room (source of truth)
                                  |              |
                                  |              +-- pending_mutations outbox
                                  +-> WorkManager -> Retrofit API
```

`AppContainer` creates the database, API service, and repository in one place. This keeps the setup short enough to follow without adding a dependency injection library.

## Git workflow

Create a new branch for each feature or bug fix:

```bash
git switch -c feature/issue-filters
git add app README.md
git commit -m "feat: add issue filters"
git push -u origin feature/issue-filters
```

Use `feature/`, `fix/`, or `chore/` at the start of each branch name. Ask another team member to review the pull request before it is merged into `main`. Run the build and tests before merging.
