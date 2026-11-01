import type {
  ClientApprovalResponse,
  PauseMilestoneResponse,
  PauseProjectRequest,
  Project,
} from "../types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly responseBody?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...init?.headers,
      },
    });
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Unknown network error";
    throw new ApiError(`Could not reach the DeadlineShield API: ${message}`, 0);
  }

  const responseText = await response.text();
  let body: unknown;
  if (responseText) {
    try {
      body = JSON.parse(responseText);
    } catch {
      body = responseText;
    }
  }

  if (!response.ok) {
    const serverMessage =
      typeof body === "object" &&
      body !== null &&
      "message" in body &&
      typeof body.message === "string"
        ? body.message
        : response.statusText || "Request failed";

    throw new ApiError(serverMessage, response.status, body);
  }

  if (body === undefined) {
    throw new ApiError("The API returned an empty response.", response.status);
  }

  return body as T;
}

export async function getProjects(): Promise<Project[]> {
  return request<Project[]>("/api/projects");
}

export async function pauseMilestone(
  milestoneId: string,
  reason: string,
): Promise<PauseMilestoneResponse> {
  const requestBody: PauseProjectRequest = { milestoneId, reason };
  return request<PauseMilestoneResponse>("/api/projects/milestones/pause", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(requestBody),
  });
}

export async function submitClientReview(
  token: string,
  feedback: string,
): Promise<ClientApprovalResponse> {
  return request<ClientApprovalResponse>(
    `/api/projects/reviews/${encodeURIComponent(token)}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ feedback }),
    },
  );
}
