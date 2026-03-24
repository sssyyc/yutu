export function isValidPhone(phone) {
  return /^\d{11}$/.test(String(phone ?? "").trim());
}
