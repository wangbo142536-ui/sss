const fakePathPattern = /(?:^|[\\/])fakepath[\\/]/i;
const windowsAbsolutePathPattern = /^[a-z]:[\\/].+/i;
const uncPathPattern = /^\\\\.+/;
const unixAbsolutePathPattern = /^\/.+/;

export function isSuspiciousContactPath(value?: string | null): boolean {
  const trimmed = value?.trim() || "";
  return Boolean(trimmed) && (
    fakePathPattern.test(trimmed)
    || windowsAbsolutePathPattern.test(trimmed)
    || uncPathPattern.test(trimmed)
    || unixAbsolutePathPattern.test(trimmed)
  );
}
