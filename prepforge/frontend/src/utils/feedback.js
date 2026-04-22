export const parseAiFeedback = (feedbackText) => {
  if (!feedbackText) {
    return null;
  }

  try {
    return JSON.parse(feedbackText);
  } catch (error) {
    return {
      summary: feedbackText,
      strengths: [],
      weaknesses: [],
      recommendations: [],
    };
  }
};
