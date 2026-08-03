export function roundFoodQuoteMoney(value: number) {
  const scaled = Math.max(0, Number(value || 0)) * 100;
  const floatingPointNoise = Number.EPSILON * Math.max(1, Math.abs(scaled)) * 4;
  return Math.ceil(scaled - floatingPointNoise) / 100;
}

export function foodQuotedUnitPrice(costUnitPrice: number, markupPercent: number) {
  const rate = Math.max(0, Number(markupPercent || 0)) / 100;
  return roundFoodQuoteMoney(Number(costUnitPrice || 0) * (1 + rate));
}

export function foodQuotedSubtotal(quantity: number, costUnitPrice: number, markupPercent: number) {
  return roundFoodQuoteMoney(Math.max(0, Number(quantity || 0)) * foodQuotedUnitPrice(costUnitPrice, markupPercent));
}
