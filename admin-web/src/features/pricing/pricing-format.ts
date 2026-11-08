export function normalizeDecimalInput(value: string): string {
  return value.trim().replace(",", ".");
}

export function formatMoney(amount: string, currency: string): string {
  try {
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(amount));
  } catch {
    return amount;
  }
}

export function formatRate(rate: string): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 6,
    maximumFractionDigits: 6,
  }).format(Number(rate));
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}
