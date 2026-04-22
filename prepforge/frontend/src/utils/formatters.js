export const formatDateTime = (value) => {
  if (!value) return "Not available";

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
};

export const formatScore = (value) => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "N/A";
  }

  return typeof value === "number" ? value.toFixed(1) : value;
};
