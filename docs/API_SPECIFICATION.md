# DeadlineShield REST API

## Overview

- **Base URL (local):** `http://localhost:8080`
- **Base path:** `/api/projects`
- **Content type:** JSON (`application/json`) for request and response bodies.
- **Identifiers:** UUID strings.
- **Dates/times:** ISO 8601 instants in UTC, for example `2026-11-01T10:00:00Z`.
- **CORS:** The backend permits the configured `FRONTEND_ORIGIN` for `/api/**` and allows `GET`, `POST`, and `OPTIONS`.

The API currently has project listing and clock/review operations. Project creation and mutation of project metadata are not implemented.

## Endpoints

### `GET /api/projects`

Returns all persisted projects and their milestones. There is no pagination or filtering yet.

**Success — `200 OK`**

```json
[
  {
    "id": "11111111-1111-4111-8111-111111111111",
    "name": "Website redesign",
    "description": "Refresh the public website",
    "adjustedTargetDate": "2026-12-15T00:00:00Z",
    "createdAt": "2026-10-01T12:00:00Z",
    "milestones": [
      {
        "id": "22222222-2222-4222-8222-222222222222",
        "name": "Approve homepage design",
        "dueAt": "2026-11-20T00:00:00Z",
        "token": "33333333-3333-4333-8333-333333333333",
        "clockPaused": false,
        "pausedAt": null,
        "reviewFeedback": null,
        "reviewedAt": null,
        "status": "PENDING"
      }
    ]
  }
]
```

The JSON shape is based on the current JPA model. `organization` and each milestone's back-reference to `project` are excluded to avoid circular serialization. The model does not currently have a distinct original project target-date field; clients may use milestone `dueAt` values as the original deadline.

**Security note:** The current entity response includes each milestone's `token`, and the API does not require authentication. Do not expose this endpoint or its responses outside a trusted environment until review tokens are excluded from project-list responses and appropriate authentication/authorization is implemented. CORS restricts browser origins; it is not an authentication mechanism.

### `POST /api/projects/{projectId}/pause`

Pauses every currently running milestone clock belonging to the project. Already-paused milestones are left unchanged. This route does not accept a request body.

**Success — `200 OK`**

```json
{
  "projectId": "11111111-1111-4111-8111-111111111111",
  "pausedMilestones": 2
}
```

**Not found — `404 Not Found`:** The project ID does not exist.

### `POST /api/projects/milestones/pause`

Pauses one milestone clock and records the supplied reason in the audit log. Repeating a pause on an already-paused milestone is idempotent and does not replace its original pause timestamp.

**Request**

```json
{
  "milestoneId": "22222222-2222-4222-8222-222222222222",
  "reason": "Waiting for client feedback"
}
```

**Jakarta validation**

| Field | Constraint | Effect |
|---|---|---|
| `milestoneId` | `@NotNull` | Required UUID value. |
| `reason` | `@NotBlank` | Required non-empty, non-whitespace reason. |

The service also rejects missing IDs and blank reasons with `400 Bad Request`.

**Success — `200 OK`**

```json
{
  "milestoneId": "22222222-2222-4222-8222-222222222222",
  "clockPaused": true,
  "pausedAt": "2026-11-01T10:00:00Z"
}
```

**Other responses:** `400 Bad Request` for invalid input; `404 Not Found` when the milestone does not exist.

### `POST /api/projects/reviews/{token}`

Accepts a client's review through a milestone magic-link token. A valid link must resolve to a currently paused milestone that has not already been reviewed and has a pause timestamp. On success, the milestone is approved, the clock resumes, elapsed whole paused days are added to the project's adjusted target date when it is set, and an audit record is saved.

**Request**

```json
{
  "feedback": "The deliverable is approved."
}
```

**Jakarta validation**

| Field | Constraint | Effect |
|---|---|---|
| `feedback` | `@NotBlank` | Required non-empty, non-whitespace review feedback. |

**Success — `200 OK`**

```json
{
  "milestoneId": "22222222-2222-4222-8222-222222222222",
  "clockPaused": false,
  "feedback": "The deliverable is approved.",
  "reviewedAt": "2026-11-01T10:00:00Z"
}
```

**Other responses:** `400 Bad Request` for blank feedback; `404 Not Found` if the token is unknown, the milestone is not paused, the pause timestamp is absent, or the link was already used.

## Error responses

The global exception handler returns JSON errors for handled failures:

```json
{
  "timestamp": "2026-11-01T10:00:00Z",
  "status": 404,
  "message": "Milestone not found"
}
```

Validation errors return `400 Bad Request` with a field-keyed `fieldErrors` object:

```json
{
  "timestamp": "2026-11-01T10:00:00Z",
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": {
    "reason": "must not be blank"
  }
}
```

| Status | Meaning |
|---|---|
| `200 OK` | Request succeeded. |
| `400 Bad Request` | Request validation failed or a supplied value is invalid. |
| `404 Not Found` | Requested project, milestone, or usable review link was not found. |
| `500 Internal Server Error` | Unexpected server failure; the response message is generic and the exception is logged server-side. |

The listed statuses describe the API's handled outcomes. Infrastructure errors (for example, database unavailability) may prevent a response from being produced by the application.

## Security considerations

- Review tokens are bearer credentials: anyone who possesses an active token can submit feedback once for its paused milestone. Keep them out of logs, analytics, URLs shared beyond the intended client, and public project-list responses.
- The project-list endpoint currently serializes the token field from each milestone, and the API has no authentication or authorization layer. Treat all API routes as suitable only for a trusted development environment until these controls are addressed.
- The configured CORS origin is a browser policy, not access control. Non-browser clients can call the API regardless of CORS.
