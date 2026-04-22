import { diffLines } from "diff";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";

function CodeDiffViewer({ originalCode, improvedCode, language }) {
  const rows = buildDiffRows(originalCode, improvedCode);

  return (
    <div className="overflow-hidden rounded-3xl border border-white/10 bg-forge-950">
      <div className="grid border-b border-white/10 bg-white/5 lg:grid-cols-2">
        <PaneHeader
          badge="Original"
          badgeClassName="bg-white/10 text-slate-300"
          description="Your latest submitted solution."
          title="Current implementation"
        />
        <PaneHeader
          badge="Improved"
          badgeClassName="bg-emerald-500/15 text-emerald-300"
          description="Refined for readability, performance, and best practices."
          title="AI-refined implementation"
        />
      </div>

      <div className="grid lg:grid-cols-2">
        <DiffPane
          emptyLabel="No original code available."
          language={language}
          rows={rows}
          side="left"
        />
        <DiffPane
          emptyLabel="No improved code available."
          language={language}
          rows={rows}
          side="right"
        />
      </div>
    </div>
  );
}

function PaneHeader({ badge, badgeClassName, description, title }) {
  return (
    <div className="border-b border-white/10 px-5 py-4 lg:border-b-0 lg:first:border-r lg:first:border-white/10">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-lg font-semibold text-white">{title}</p>
          <p className="mt-1 text-sm leading-6 text-slate-400">{description}</p>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.25em] ${badgeClassName}`}
        >
          {badge}
        </span>
      </div>
    </div>
  );
}

function DiffPane({ emptyLabel, language, rows, side }) {
  const hasVisibleCode = rows.some((row) => {
    const cell = side === "left" ? row.left : row.right;
    return Boolean(cell.text);
  });

  if (!hasVisibleCode) {
    return <div className="px-5 py-8 text-sm text-slate-400">{emptyLabel}</div>;
  }

  return (
    <div className="overflow-x-auto lg:first:border-r lg:first:border-white/10">
      <div className="min-w-[540px]">
        {rows.map((row, index) => {
          const cell = side === "left" ? row.left : row.right;

          return (
            <div
              key={`${side}-${index}-${cell.lineNumber ?? "blank"}`}
              className={`grid grid-cols-[72px,1fr] border-b border-white/5 ${getRowBackground(cell.type)}`}
            >
              <div className="border-r border-white/5 px-3 py-2 text-right text-xs text-slate-500">
                {cell.lineNumber ?? ""}
              </div>
              <div className="px-3 py-2">
                {cell.text ? (
                  <SyntaxHighlighter
                    CodeTag="span"
                    PreTag="div"
                    customStyle={{
                      margin: 0,
                      padding: 0,
                      background: "transparent",
                      fontSize: "0.9rem",
                      lineHeight: "1.6",
                    }}
                    language={getCodeLanguage(language)}
                    style={vscDarkPlus}
                    wrapLongLines
                  >
                    {cell.text}
                  </SyntaxHighlighter>
                ) : (
                  <span className="block min-h-6" />
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function buildDiffRows(originalCode = "", improvedCode = "") {
  const parts = diffLines(originalCode, improvedCode);
  const rows = [];
  let leftLineNumber = 1;
  let rightLineNumber = 1;

  for (let index = 0; index < parts.length; index += 1) {
    const part = parts[index];

    if (part.removed && parts[index + 1]?.added) {
      const removedLines = splitLines(part.value);
      const addedLines = splitLines(parts[index + 1].value);
      const maxLength = Math.max(removedLines.length, addedLines.length);

      for (let lineIndex = 0; lineIndex < maxLength; lineIndex += 1) {
        const removedLine = removedLines[lineIndex];
        const addedLine = addedLines[lineIndex];

        rows.push({
          left: createCell(removedLine, leftLineNumber, "removed"),
          right: createCell(addedLine, rightLineNumber, "added"),
        });

        if (removedLine !== undefined) {
          leftLineNumber += 1;
        }

        if (addedLine !== undefined) {
          rightLineNumber += 1;
        }
      }

      index += 1;
      continue;
    }

    if (part.removed) {
      splitLines(part.value).forEach((line) => {
        rows.push({
          left: createCell(line, leftLineNumber, "removed"),
          right: createCell(undefined, null, "empty"),
        });
        leftLineNumber += 1;
      });
      continue;
    }

    if (part.added) {
      splitLines(part.value).forEach((line) => {
        rows.push({
          left: createCell(undefined, null, "empty"),
          right: createCell(line, rightLineNumber, "added"),
        });
        rightLineNumber += 1;
      });
      continue;
    }

    splitLines(part.value).forEach((line) => {
      rows.push({
        left: createCell(line, leftLineNumber, "unchanged"),
        right: createCell(line, rightLineNumber, "unchanged"),
      });
      leftLineNumber += 1;
      rightLineNumber += 1;
    });
  }

  return rows;
}

function createCell(text, lineNumber, type) {
  return {
    lineNumber,
    text: text ?? "",
    type: text === undefined ? "empty" : type,
  };
}

function splitLines(value) {
  const normalized = value.replace(/\r\n/g, "\n");

  if (!normalized) {
    return [];
  }

  const lines = normalized.split("\n");
  if (lines.at(-1) === "") {
    lines.pop();
  }

  return lines;
}

function getRowBackground(type) {
  if (type === "added") {
    return "bg-emerald-500/10";
  }

  if (type === "removed") {
    return "bg-red-500/10";
  }

  return "bg-transparent";
}

function getCodeLanguage(language) {
  const normalized = language?.toLowerCase();

  if (normalized === "java") return "java";
  if (normalized === "javascript") return "javascript";
  if (normalized === "python") return "python";
  if (normalized === "cpp" || normalized === "c++") return "cpp";

  return "java";
}

export default CodeDiffViewer;
