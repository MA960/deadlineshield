"use client";

import {
  AlertCircle,
  ArrowDownRight,
  ArrowUpRight,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  Clock3,
  FolderKanban,
  LoaderCircle,
  Pause,
  RefreshCw,
  ShieldCheck,
  X,
} from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import {
  ApiError,
  getProjects,
  pauseMilestone,
} from "../services/api";
import type { Milestone, Project } from "../types";

function formatDate(value: string | null | undefined): string {
  if (!value) return "Not set";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Not set";
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(date);
}

function projectOriginalDate(project: Project): string | null {
  if (project.originalTargetDate) {
    return project.originalTargetDate;
  }
  const dates = project.milestones
    .map((milestone) => milestone.dueAt)
    .filter((date): date is string => Boolean(date))
    .sort((left, right) => Date.parse(left) - Date.parse(right));
  return dates[0] ?? null;
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 0) {
    return "The backend could not be reached. Check that the Spring Boot API is running on localhost:8080.";
  }
  if (error instanceof ApiError && error.status === 404) {
    return "The projects endpoint is not available yet. Ensure the backend exposes GET /api/projects.";
  }
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}

function LoadingSkeleton() {
  return (
    <div className="animate-pulse space-y-4" aria-label="Loading projects">
      {[0, 1, 2].map((row) => (
        <div
          className="grid grid-cols-4 gap-6 border-b border-zinc-100 px-6 py-6"
          key={row}
        >
          <div className="col-span-2 flex items-center gap-3">
            <div className="size-10 rounded-xl bg-zinc-100" />
            <div className="space-y-2">
              <div className="h-4 w-36 rounded bg-zinc-100" />
              <div className="h-3 w-24 rounded bg-zinc-100" />
            </div>
          </div>
          <div className="h-4 w-24 self-center rounded bg-zinc-100" />
          <div className="h-9 w-28 self-center justify-self-end rounded-lg bg-zinc-100" />
        </div>
      ))}
    </div>
  );
}

function StatCard({
  label,
  value,
  detail,
  icon: Icon,
  tone,
  loading,
}: {
  label: string;
  value: number;
  detail: string;
  icon: typeof FolderKanban;
  tone: "blue" | "amber" | "violet";
  loading: boolean;
}) {
  const tones = {
    blue: "bg-blue-50 text-blue-700 ring-blue-100",
    amber: "bg-amber-50 text-amber-700 ring-amber-100",
    violet: "bg-violet-50 text-violet-700 ring-violet-100",
  };

  return (
    <section className="rounded-2xl border border-zinc-200/80 bg-white p-5 shadow-sm shadow-zinc-950/[0.02]">
      <div className="flex items-start justify-between">
        <p className="text-sm font-medium text-zinc-500">{label}</p>
        <span className={`grid size-10 place-items-center rounded-xl ring-1 ${tones[tone]}`}>
          <Icon aria-hidden="true" className="size-5" />
        </span>
      </div>
      {loading ? (
        <div className="mt-5 h-9 w-16 animate-pulse rounded-lg bg-zinc-100" />
      ) : (
        <p className="mt-3 text-3xl font-semibold tracking-tight text-zinc-900">{value}</p>
      )}
      <p className="mt-2 text-xs text-zinc-500">{detail}</p>
    </section>
  );
}

export default function AgencyDashboardPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [selectedMilestoneId, setSelectedMilestoneId] = useState("");
  const [pauseReason, setPauseReason] = useState("");
  const [pauseError, setPauseError] = useState<string | null>(null);
  const [isPausing, setIsPausing] = useState(false);

  const loadProjects = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const result = await getProjects();
      setProjects(result);
    } catch (error) {
      setLoadError(errorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    async function fetchInitialProjects() {
      try {
        const result = await getProjects();
        if (active) setProjects(result);
      } catch (error) {
        if (active) setLoadError(errorMessage(error));
      } finally {
        if (active) setIsLoading(false);
      }
    }

    void fetchInitialProjects();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedProject) return;

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape" && !isPausing) {
        setSelectedProject(null);
        setPauseError(null);
      }
    }

    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [selectedProject, isPausing]);

  const stats = useMemo(() => {
    const milestones = projects.flatMap((project) => project.milestones);
    return {
      activeProjects: projects.filter((project) =>
        project.milestones.some((milestone) => !milestone.clockPaused),
      ).length,
      pausedClocks: milestones.filter((milestone) => milestone.clockPaused).length,
      pendingApprovals: milestones.filter(
        (milestone) => milestone.status === "PENDING" && milestone.clockPaused,
      ).length,
    };
  }, [projects]);

  const availableMilestones = selectedProject?.milestones.filter(
    (milestone) => !milestone.clockPaused,
  ) ?? [];

  function openPauseDialog(project: Project) {
    const firstAvailable = project.milestones.find((milestone) => !milestone.clockPaused);
    setSelectedProject(project);
    setSelectedMilestoneId(firstAvailable?.id ?? "");
    setPauseReason("");
    setPauseError(null);
  }

  function closePauseDialog() {
    if (isPausing) return;
    setSelectedProject(null);
    setPauseError(null);
  }

  async function handlePauseSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedMilestoneId || !pauseReason.trim()) {
      setPauseError("Choose a milestone and enter a reason.");
      return;
    }

    setIsPausing(true);
    setPauseError(null);
    try {
      const result = await pauseMilestone(selectedMilestoneId, pauseReason.trim());
      setProjects((current) =>
        current.map((project) => ({
          ...project,
          milestones: project.milestones.map((milestone) =>
            milestone.id === result.milestoneId
              ? { ...milestone, clockPaused: result.clockPaused, pausedAt: result.pausedAt }
              : milestone,
          ),
        })),
      );
      setSelectedProject(null);
    } catch (error) {
      setPauseError(errorMessage(error));
    } finally {
      setIsPausing(false);
    }
  }

  return (
    <main className="min-h-screen bg-[#f7f8fa] text-zinc-900">
      <header className="border-b border-zinc-200/80 bg-white">
        <div className="mx-auto flex h-[72px] max-w-7xl items-center justify-between px-5 sm:px-8">
          <Link className="flex items-center gap-3" href="/" aria-label="DeadlineShield home">
            <span className="grid size-10 place-items-center rounded-xl bg-blue-600 text-white shadow-sm shadow-blue-600/20">
              <ShieldCheck aria-hidden="true" className="size-[21px]" />
            </span>
            <span className="text-[17px] font-semibold tracking-tight">
              Deadline<span className="text-blue-600">Shield</span>
            </span>
          </Link>
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-zinc-500 sm:inline">Agency workspace</span>
            <span className="grid size-9 place-items-center rounded-full bg-zinc-100 text-sm font-semibold text-zinc-700">
              DS
            </span>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-7xl px-5 pb-16 pt-9 sm:px-8 sm:pt-12">
        <div className="mb-8 flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <p className="mb-2 text-sm font-medium text-blue-700">Overview</p>
            <h1 className="text-3xl font-semibold tracking-tight text-zinc-950 sm:text-[34px]">
              Agency dashboard
            </h1>
            <p className="mt-2 max-w-xl text-sm leading-6 text-zinc-500">
              Keep every project on track, and give your team breathing room when timelines change.
            </p>
          </div>
          <span className="inline-flex h-9 items-center gap-2 self-start rounded-full border border-emerald-200 bg-white px-3 text-xs font-medium text-zinc-600 sm:self-auto">
            <span className="size-2 rounded-full bg-emerald-500" />
            Agency workspace
          </span>
        </div>

        {loadError && (
          <div
            className="mb-6 flex flex-col gap-4 rounded-xl border border-red-200 bg-red-50 px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
            role="alert"
          >
            <div className="flex items-start gap-3">
              <AlertCircle aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-red-600" />
              <div>
                <p className="text-sm font-semibold text-red-900">Unable to load projects</p>
                <p className="mt-1 text-sm leading-5 text-red-800">{loadError}</p>
              </div>
            </div>
            <button
              className="inline-flex h-9 shrink-0 items-center justify-center gap-2 self-start rounded-lg border border-red-200 bg-white px-3 text-sm font-medium text-red-800 transition hover:bg-red-100 focus:outline-none focus:ring-4 focus:ring-red-100 sm:self-auto"
              onClick={() => void loadProjects()}
              type="button"
            >
              <RefreshCw aria-hidden="true" className="size-4" />
              Retry
            </button>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            detail="Projects with a running milestone clock"
            icon={FolderKanban}
            label="Active projects"
            loading={isLoading}
            tone="blue"
            value={stats.activeProjects}
          />
          <StatCard
            detail="Milestone clocks currently on hold"
            icon={Clock3}
            label="Paused clocks"
            loading={isLoading}
            tone="amber"
            value={stats.pausedClocks}
          />
          <StatCard
            detail="Paused milestones awaiting client feedback"
            icon={CheckCircle2}
            label="Pending approvals"
            loading={isLoading}
            tone="violet"
            value={stats.pendingApprovals}
          />
        </div>

        <section className="mt-8 overflow-hidden rounded-2xl border border-zinc-200/80 bg-white shadow-sm shadow-zinc-950/[0.02]">
          <div className="flex flex-col gap-3 border-b border-zinc-100 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
            <div>
              <h2 className="text-base font-semibold text-zinc-900">Projects</h2>
              <p className="mt-1 text-sm text-zinc-500">
                Compare original and adjusted delivery targets.
              </p>
            </div>
            <button
              className="inline-flex h-9 items-center gap-2 self-start rounded-lg border border-zinc-200 bg-white px-3 text-sm font-medium text-zinc-600 transition hover:bg-zinc-50 hover:text-zinc-900 focus:outline-none focus:ring-4 focus:ring-zinc-100 sm:self-auto"
              onClick={() => void loadProjects()}
              type="button"
            >
              <RefreshCw aria-hidden="true" className={`size-4 ${isLoading ? "animate-spin" : ""}`} />
              Refresh
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left">
              <thead className="bg-zinc-50/80">
                <tr className="text-[11px] font-semibold uppercase tracking-[0.08em] text-zinc-500">
                  <th className="px-6 py-3.5">Project</th>
                  <th className="px-6 py-3.5">Milestones</th>
                  <th className="px-6 py-3.5">Original target</th>
                  <th className="px-6 py-3.5">Adjusted target</th>
                  <th className="px-6 py-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="p-0">
                      <LoadingSkeleton />
                    </td>
                  </tr>
                ) : projects.length > 0 ? (
                  projects.map((project) => {
                    const pausedCount = project.milestones.filter(
                      (milestone) => milestone.clockPaused,
                    ).length;
                    const availableCount = project.milestones.length - pausedCount;
                    const originalDate = projectOriginalDate(project);

                    return (
                      <tr className="transition hover:bg-zinc-50/70" key={project.id}>
                        <td className="px-6 py-5">
                          <div className="flex items-center gap-3">
                            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-blue-50 text-blue-700">
                              <FolderKanban aria-hidden="true" className="size-[18px]" />
                            </span>
                            <div>
                              <p className="font-semibold text-zinc-900">{project.name}</p>
                              <p className="mt-0.5 max-w-[240px] truncate text-xs text-zinc-500">
                                {project.description || "No project description"}
                              </p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-5">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium text-zinc-800">
                              {project.milestones.length}
                            </span>
                            <span className="text-xs text-zinc-500">total</span>
                            {pausedCount > 0 && (
                              <span className="ml-1 inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-1 text-[11px] font-medium text-amber-700">
                                <Pause aria-hidden="true" className="size-3" />
                                {pausedCount} paused
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-5">
                          <div className="flex items-center gap-2 text-sm text-zinc-600">
                            <CalendarDays aria-hidden="true" className="size-4 text-zinc-400" />
                            {formatDate(originalDate)}
                          </div>
                        </td>
                        <td className="px-6 py-5">
                          <div className="flex items-center gap-2 text-sm font-medium text-zinc-800">
                            <CalendarDays aria-hidden="true" className="size-4 text-blue-500" />
                            {formatDate(project.adjustedTargetDate)}
                            {project.adjustedTargetDate && originalDate && (
                              Date.parse(project.adjustedTargetDate) > Date.parse(originalDate) ? (
                                <ArrowUpRight aria-label="Target moved later" className="size-4 text-amber-600" />
                              ) : (
                                <ArrowDownRight aria-label="Target moved earlier" className="size-4 text-emerald-600" />
                              )
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-5 text-right">
                          <button
                            className="inline-flex h-9 items-center gap-2 rounded-lg border border-zinc-200 bg-white px-3 text-sm font-medium text-zinc-700 transition hover:border-amber-300 hover:bg-amber-50 hover:text-amber-800 focus:outline-none focus:ring-4 focus:ring-amber-100 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:border-zinc-200 disabled:hover:bg-white disabled:hover:text-zinc-700"
                            disabled={availableCount === 0}
                            onClick={() => openPauseDialog(project)}
                            type="button"
                          >
                            <Pause aria-hidden="true" className="size-3.5" />
                            Pause clock
                            <ChevronDown aria-hidden="true" className="size-3.5 text-zinc-400" />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td className="px-6 py-16 text-center" colSpan={5}>
                      <span className="mx-auto grid size-12 place-items-center rounded-2xl bg-zinc-100 text-zinc-500">
                        <FolderKanban aria-hidden="true" className="size-5" />
                      </span>
                      <p className="mt-4 text-sm font-semibold text-zinc-800">No projects yet</p>
                      <p className="mt-1 text-sm text-zinc-500">
                        Projects will appear here once they are available from the API.
                      </p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {!isLoading && projects.length > 0 && (
            <div className="flex items-center justify-between border-t border-zinc-100 px-6 py-3.5">
              <p className="text-xs text-zinc-500">
                Showing <span className="font-medium text-zinc-700">{projects.length}</span>{" "}
                {projects.length === 1 ? "project" : "projects"}
              </p>
              <span className="inline-flex items-center gap-1.5 text-xs font-medium text-zinc-500">
                <span className="size-1.5 rounded-full bg-emerald-500" />
                Live data
              </span>
            </div>
          )}
        </section>
        <p className="mt-5 text-center text-xs text-zinc-400">
          DeadlineShield keeps project timelines transparent and accountable.
        </p>
      </div>

      {selectedProject && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/40 px-4 py-8 backdrop-blur-[2px]"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closePauseDialog();
          }}
        >
          <section
            aria-labelledby="pause-dialog-title"
            aria-modal="true"
            className="w-full max-w-md rounded-2xl border border-zinc-200 bg-white p-6 shadow-2xl shadow-zinc-950/20"
            role="dialog"
          >
            <div className="flex items-start justify-between">
              <div className="flex items-start gap-3">
                <span className="grid size-10 place-items-center rounded-xl bg-amber-50 text-amber-700">
                  <Pause aria-hidden="true" className="size-[18px]" />
                </span>
                <div>
                  <h2 id="pause-dialog-title" className="text-lg font-semibold text-zinc-900">
                    Pause a milestone clock
                  </h2>
                  <p className="mt-1 text-sm text-zinc-500">{selectedProject.name}</p>
                </div>
              </div>
              <button
                aria-label="Close dialog"
                className="grid size-8 place-items-center rounded-lg text-zinc-400 transition hover:bg-zinc-100 hover:text-zinc-700 focus:outline-none focus:ring-4 focus:ring-zinc-100 disabled:opacity-50"
                disabled={isPausing}
                onClick={closePauseDialog}
                type="button"
              >
                <X aria-hidden="true" className="size-4" />
              </button>
            </div>

            <form className="mt-6 space-y-5" onSubmit={handlePauseSubmit}>
              <div>
                <label className="mb-2 block text-sm font-medium text-zinc-700" htmlFor="milestone">
                  Milestone
                </label>
                <select
                  className="h-11 w-full rounded-lg border border-zinc-200 bg-white px-3 text-sm text-zinc-800 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                  id="milestone"
                  onChange={(event) => setSelectedMilestoneId(event.target.value)}
                  required
                  value={selectedMilestoneId}
                >
                  {availableMilestones.map((milestone: Milestone) => (
                    <option key={milestone.id} value={milestone.id}>
                      {milestone.name || `Milestone ${milestone.id.slice(0, 8)}`}
                    </option>
                  ))}
                </select>
                {availableMilestones.length === 0 && (
                  <p className="mt-2 text-xs text-zinc-500">All milestone clocks are already paused.</p>
                )}
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-zinc-700" htmlFor="reason">
                  Reason for pausing
                </label>
                <textarea
                  className="min-h-24 w-full resize-y rounded-lg border border-zinc-200 bg-white px-3 py-2.5 text-sm text-zinc-800 outline-none transition placeholder:text-zinc-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                  id="reason"
                  maxLength={500}
                  onChange={(event) => setPauseReason(event.target.value)}
                  placeholder="For example, waiting on client feedback or required materials"
                  required
                  value={pauseReason}
                />
                <p className="mt-1.5 text-right text-xs text-zinc-400">{pauseReason.length}/500</p>
              </div>

              {pauseError && (
                <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-800" role="alert">
                  <AlertCircle aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
                  <span>{pauseError}</span>
                </div>
              )}

              <div className="flex justify-end gap-3 border-t border-zinc-100 pt-4">
                <button
                  className="h-10 rounded-lg border border-zinc-200 px-4 text-sm font-medium text-zinc-700 transition hover:bg-zinc-50 focus:outline-none focus:ring-4 focus:ring-zinc-100 disabled:opacity-50"
                  disabled={isPausing}
                  onClick={closePauseDialog}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-100 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={isPausing || availableMilestones.length === 0}
                  type="submit"
                >
                  {isPausing ? (
                    <LoaderCircle aria-hidden="true" className="size-4 animate-spin" />
                  ) : (
                    <Pause aria-hidden="true" className="size-3.5" />
                  )}
                  {isPausing ? "Pausing…" : "Pause clock"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
