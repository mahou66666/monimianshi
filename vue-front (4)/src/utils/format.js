const pad = (value) => String(value).padStart(2, '0');

const toDate = (value) => (value instanceof Date ? value : new Date(value));

export const formatFileSizeFromBytes = (sizeInBytes = 0) => {
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = Number(sizeInBytes);
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  const precision = unitIndex === 0 ? 0 : 1;
  return `${value.toFixed(precision)} ${units[unitIndex]}`;
};

export const formatFileSize = (sizeInMb, sizeInBytes = 0) => {
  const parsedBytes = Number(sizeInBytes);

  if (Number.isFinite(parsedBytes) && parsedBytes > 0) {
    return formatFileSizeFromBytes(parsedBytes);
  }

  const parsedMb = Number(sizeInMb);

  if (!Number.isFinite(parsedMb) || parsedMb <= 0) {
    return '0 B';
  }

  if (parsedMb < 1) {
    return `${Math.max(1, Math.round(parsedMb * 1024))} KB`;
  }

  return `${parsedMb.toFixed(1)} MB`;
};

export const formatDate = (value) => {
  const date = toDate(value);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

export const formatDateTime = (value) => {
  const date = toDate(value);
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export const joinMeta = (...parts) => parts.filter(Boolean).join(' · ');
