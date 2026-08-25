export type BackgroundTaskStatus = "IDLE" | "RUNNING" | "PARTIAL" | "COMPLETED" | "FAILED";

export type BackgroundTaskStage = {
  code: string;
  label: string;
};

export type BackgroundTaskCounter = {
  label: string;
  value: string;
};
