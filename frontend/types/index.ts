export interface Project {
  id: string;
  name: string;
  description: string | null;
  originalTargetDate?: string | null;
  adjustedTargetDate: string | null;
  createdAt: string;
  milestones: Milestone[];
}

export interface Milestone {
  id: string;
  name: string;
  dueAt: string | null;
  clockPaused: boolean;
  pausedAt: string | null;
  reviewFeedback: string | null;
  reviewedAt: string | null;
  status: "PENDING" | "APPROVED";
}

export interface PauseProjectRequest {
  milestoneId: string;
  reason: string;
}

export interface ClientReviewResponse {
  projectTitle: string;
  clientName: string;
  originalTargetDate: string | null;
  adjustedTargetDate: string | null;
  status: string;
  pausedAt: string | null;
}

export interface PauseMilestoneResponse {
  milestoneId: string;
  clockPaused: boolean;
  pausedAt: string | null;
}

export interface ClientApprovalResponse {
  milestoneId: string;
  clockPaused: boolean;
  feedback: string;
  reviewedAt: string;
}
