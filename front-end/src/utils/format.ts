/**
 * Format a date string or number to a readable format
 * @param date - Date string (ISO format or other) or timestamp
 * @param format - Optional format type
 * @returns Formatted date string
 */
export function formatDate(date: string | number | Date | null | undefined): string {
  if (!date) return '-';
  
  try {
    const d = new Date(date);
    if (isNaN(d.getTime())) return String(date);
    
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  } catch (e) {
    return String(date);
  }
}

/**
 * Format currency amount
 * @param amount - Number
 * @returns Formatted currency string
 */
export function formatCurrency(amount: number | string): string {
  const val = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(val)) return '0.00';
  return val.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
