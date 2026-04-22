/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        forge: {
          950: "#090909",
          900: "#111111",
          850: "#171717",
          800: "#1e1e1e",
          700: "#292929",
          600: "#3b3b3b",
        },
        ember: {
          300: "#ffb36a",
          400: "#ff9a3d",
          500: "#f97316",
          600: "#ea580c",
        },
      },
      boxShadow: {
        ember: "0 0 0 1px rgba(249,115,22,0.18), 0 18px 50px rgba(249,115,22,0.12)",
      },
      backgroundImage: {
        "forge-radial":
          "radial-gradient(circle at top, rgba(249,115,22,0.16), transparent 35%), radial-gradient(circle at bottom right, rgba(255,154,61,0.08), transparent 30%)",
      },
      fontFamily: {
        sans: ["Space Grotesk", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
    },
  },
  plugins: [],
};
