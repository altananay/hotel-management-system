export function normalizeDecimalInput(value: string): string {
  return value.trim().replace(",", ".");
}

function groupInteger(value: string): string {
  if (value === "") {
    return "0";
  }
  try {
    return new Intl.NumberFormat("tr-TR").format(BigInt(value));
  } catch {
    return value;
  }
}

function decimalParts(value: string, fractionDigits: number): [string, string] {
  const [integerPart, fractionPart = ""] = value.split(".");
  return [
    groupInteger(integerPart),
    fractionPart.padEnd(fractionDigits, "0").slice(0, fractionDigits),
  ];
}

/**
 * Formats a decimal string without converting it to a JS number. Amounts arrive as strings per
 * docs/conventions.md §3; arithmetic stays in the backend.
 */
export function formatMoney(amount: string, currency: string): string {
  try {
    const [integerPart, fractionPart] = decimalParts(amount, 2);
    const symbol = new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency,
    })
      .formatToParts(0)
      .find((part) => part.type === "currency")?.value;
    return symbol
      ? `${symbol}${integerPart},${fractionPart}`
      : `${integerPart},${fractionPart} ${currency}`;
  } catch {
    return `${amount} ${currency}`;
  }
}

export function formatRate(rate: string): string {
  const [integerPart, fractionPart] = decimalParts(rate, 6);
  return `${integerPart},${fractionPart}`;
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
