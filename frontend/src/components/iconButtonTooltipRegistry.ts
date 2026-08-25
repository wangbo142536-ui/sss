import { shallowRef } from "vue";

export type IconButtonTooltipRequest = {
  ownerId: string;
  label: string;
  anchor: HTMLElement;
};

const requests = new Map<string, IconButtonTooltipRequest>();
let ownerSequence = 0;

export const activeIconButtonTooltip = shallowRef<IconButtonTooltipRequest | null>(null);

function activateLatestRequest() {
  const values = [...requests.values()];
  activeIconButtonTooltip.value = values[values.length - 1] ?? null;
}

export function createIconButtonTooltipOwnerId(): string {
  ownerSequence += 1;
  return `icon-button-tooltip-owner-${ownerSequence}`;
}

export function requestIconButtonTooltip(request: IconButtonTooltipRequest) {
  requests.delete(request.ownerId);
  requests.set(request.ownerId, request);
  activeIconButtonTooltip.value = request;
}

export function releaseIconButtonTooltip(ownerId: string) {
  const wasActive = activeIconButtonTooltip.value?.ownerId === ownerId;
  requests.delete(ownerId);
  if (wasActive) activateLatestRequest();
}

export function resetIconButtonTooltipRegistry() {
  requests.clear();
  activeIconButtonTooltip.value = null;
}
