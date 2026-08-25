import type { CompanyMenuOption } from "@/services/companyMemberService";

const collectSubtreeCodes = (node: CompanyMenuOption, result: string[] = []) => {
  result.push(node.code);
  node.children.forEach((child) => collectSubtreeCodes(child, result));
  return result;
};

const reconcileNode = (node: CompanyMenuOption, selected: Set<string>) => {
  node.children.forEach((child) => reconcileNode(child, selected));
  if (!node.children.length) return;

  const descendantCodes = node.children.flatMap((child) => collectSubtreeCodes(child));
  if (descendantCodes.every((code) => selected.has(code))) selected.add(node.code);
  else selected.delete(node.code);
};

export const reconcileMenuPermissionKeys = (nodes: CompanyMenuOption[], values: string[]) => {
  const selected = new Set(values);
  nodes.forEach((node) => reconcileNode(node, selected));
  return Array.from(selected);
};

export const toggleMenuPermissionKeys = (
  nodes: CompanyMenuOption[],
  values: string[],
  target: CompanyMenuOption,
  checked: boolean
) => {
  const selected = new Set(values);
  collectSubtreeCodes(target).forEach((code) => {
    if (checked) selected.add(code);
    else selected.delete(code);
  });
  return reconcileMenuPermissionKeys(nodes, Array.from(selected));
};

export const getMenuPermissionState = (node: CompanyMenuOption, values: string[]) => {
  const selected = new Set(values);
  const subtreeCodes = collectSubtreeCodes(node);
  const selectedCount = subtreeCodes.filter((code) => selected.has(code)).length;
  return {
    checked: selectedCount === subtreeCodes.length,
    indeterminate: selectedCount > 0 && selectedCount < subtreeCodes.length
  };
};

export const normalizeMenuPermissionKeys = (nodes: CompanyMenuOption[], values: string[]) => {
  const selected = new Set(values);
  const expandSelectedParents = (node: CompanyMenuOption) => {
    if (selected.has(node.code)) {
      collectSubtreeCodes(node).forEach((code) => selected.add(code));
    }
    node.children.forEach(expandSelectedParents);
  };
  nodes.forEach(expandSelectedParents);
  return reconcileMenuPermissionKeys(nodes, Array.from(selected));
};
