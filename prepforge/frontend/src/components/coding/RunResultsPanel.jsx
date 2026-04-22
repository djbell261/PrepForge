function RunResultsPanel({ runResult }) {
  if (!runResult) {
    return null;
  }

  const testResults = Array.isArray(runResult.testResults)
    ? runResult.testResults
    : Array.isArray(runResult.results)
      ? runResult.results
      : [];
  const hasCompileError = Boolean(runResult.compileError);
  const hasRuntimeError = Boolean(runResult.runtimeError);
  const passedCount = runResult.passedTests ?? testResults.filter((result) => result.passed).length;
  const totalCount = runResult.totalTests ?? testResults.length;
  const failedCount = Math.max(totalCount - passedCount, 0);
  const executionSucceeded = !hasCompileError && !hasRuntimeError;

  return (
    <section className="panel overflow-hidden border border-ember-500/10">
      <div className="border-b border-white/10 bg-gradient-to-r from-ember-500/12 via-white/0 to-white/0 px-6 py-6 sm:px-8">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.35em] text-ember-300">Run Results</p>
            <h3 className="mt-3 text-3xl font-bold text-white">Sample test case execution</h3>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-400">
              {hasCompileError
                ? "Review the compiler output first. Test execution will begin once the code compiles."
                : hasRuntimeError
                  ? "The code compiled, but execution failed before the sample cases could finish."
                  : "Validate your code against the question&apos;s sample cases before final submission."}
            </p>
          </div>

          {executionSucceeded ? (
            <div className="grid gap-3 sm:grid-cols-3">
              <MetricCard
                label="Execution"
                value={runResult.success ? "Success" : "Needs attention"}
                valueClassName={runResult.success ? "text-emerald-300" : "text-red-300"}
              />
              <MetricCard
                label="Passing Tests"
                value={`${passedCount}/${totalCount}`}
                valueClassName={totalCount > 0 && passedCount === totalCount ? "text-emerald-300" : "text-slate-200"}
              />
              <MetricCard
                label="Failures"
                value={String(failedCount)}
                valueClassName={failedCount === 0 ? "text-slate-200" : "text-red-300"}
              />
            </div>
          ) : null}
        </div>
      </div>

      <div className="space-y-6 px-6 py-6 sm:px-8">
        {hasCompileError ? (
          <CompileErrorPanel
            compileError={runResult.compileError}
            friendlyMessage={runResult.friendlyMessage}
            rawOutput={runResult.rawOutput}
          />
        ) : null}

        {hasRuntimeError ? (
          <ErrorPanel
            description={
              runResult.timedOut
                ? "Compilation finished, but execution exceeded the time limit before the sample cases could complete."
                : "The code compiled, but execution failed while running the sample cases."
            }
            title={runResult.timedOut ? "Execution Timed Out" : "Runtime Error"}
            value={runResult.runtimeError}
            variant={runResult.timedOut ? "amber" : "red"}
          />
        ) : null}

        {executionSucceeded && testResults.length ? (
          <div className="grid gap-5">
            {testResults.map((result, index) => (
              <article
                key={`${result.input}-${index}`}
                className={`overflow-hidden rounded-3xl border p-0 ${
                  result.passed
                    ? "border-emerald-500/20 bg-emerald-500/5"
                    : "border-red-500/20 bg-red-500/5"
                }`}
              >
                <div className="flex flex-col gap-4 border-b border-white/10 px-5 py-5 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-4">
                    <div
                      className={`mt-1 h-3 w-3 rounded-full ${
                        result.passed ? "bg-emerald-400 shadow-[0_0_20px_rgba(52,211,153,0.45)]" : "bg-red-400 shadow-[0_0_20px_rgba(248,113,113,0.45)]"
                      }`}
                    />
                    <div>
                      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Sample Case {index + 1}</p>
                      <h4 className="mt-2 text-lg font-semibold text-white">
                        {result.passed ? "Output matched expected result" : "Output did not match expected result"}
                      </h4>
                      <p className="mt-2 text-sm leading-6 text-slate-400">
                        {result.passed
                          ? "This sample case passed with the current implementation."
                          : "This sample case failed. Compare the expected and actual outputs below."}
                      </p>
                    </div>
                  </div>
                  <div className="sm:pl-4">
                    <ResultBadge passed={result.passed} />
                  </div>
                </div>

                <div className="px-5 py-5">
                  <div className="grid gap-4 xl:grid-cols-[1fr,1fr,1fr]">
                    <OutputCard
                      accentClassName="border-white/10"
                      label="Input"
                      value={result.input}
                    />
                    <OutputCard
                      accentClassName="border-white/10"
                      label="Expected Output"
                      value={result.expectedOutput}
                    />
                    <OutputCard
                      accentClassName={result.passed ? "border-emerald-500/25 bg-emerald-500/5" : "border-red-500/25 bg-red-500/5"}
                      label="Actual Output"
                      value={result.actualOutput}
                    />
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : null}

        {executionSucceeded && !testResults.length ? (
          <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-8 text-sm text-slate-400">
            No test cases were returned for this run.
          </div>
        ) : null}
      </div>
    </section>
  );
}

function CompileErrorPanel({ compileError, friendlyMessage, rawOutput }) {
  const hasLocation = Boolean(compileError?.line || compileError?.column);
  const hasCodeContext = Boolean(compileError?.codeLine && compileError?.column);

  return (
    <div className="rounded-3xl border border-red-500/20 bg-red-500/10 p-5 shadow-[0_0_40px_rgba(239,68,68,0.08)]">
      <div className="flex flex-col gap-4 border-b border-red-500/15 pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-red-300">Compilation Failed</p>
          <h4 className="mt-2 text-2xl font-semibold text-white">
            {compileError?.message || "The Java compiler found a problem in this code."}
          </h4>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-red-100">
            {friendlyMessage || "Fix the compiler error below and run your code again."}
          </p>
        </div>

        {hasLocation ? (
          <div className="rounded-2xl border border-red-500/15 bg-black/20 px-4 py-3 text-sm text-red-100">
            <p>
              Line {compileError?.line || "?"}, Column {compileError?.column || "?"}
            </p>
          </div>
        ) : null}
      </div>

      {hasCodeContext ? (
        <div className="mt-5 rounded-2xl border border-red-500/15 bg-black/30 p-4">
          <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Code Context</p>
          <pre className="mt-3 overflow-x-auto font-mono text-sm leading-7 text-red-50">
            <span className="text-slate-500">
              {String(compileError.line).padStart(3, " ")} |{" "}
            </span>
            {compileError.codeLine}
            {"\n"}
            <span className="text-slate-500">    | </span>
            <span>{" ".repeat(Math.max((compileError.column || 1) - 1, 0))}</span>
            <span className="font-bold text-red-300">^</span>
          </pre>
        </div>
      ) : null}

      <div className="mt-5 grid gap-4 lg:grid-cols-[0.42fr,0.58fr]">
        <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
          <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Compiler Message</p>
          <p className="mt-3 text-sm leading-7 text-slate-200">
            {compileError?.message || "No structured compiler message was returned."}
          </p>
        </div>

        <div className="rounded-2xl border border-red-500/15 bg-black/20 p-4">
          <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Raw Compiler Output</p>
          <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-words font-mono text-sm leading-7 text-red-100">
            {rawOutput || compileError?.message || "No raw compiler output was returned."}
          </pre>
        </div>
      </div>
    </div>
  );
}

function MetricCard({ label, value, valueClassName }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4 backdrop-blur">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className={`mt-3 text-2xl font-semibold ${valueClassName}`}>{value}</p>
    </div>
  );
}

function ErrorPanel({ description, title, value, variant = "red" }) {
  const panelClassName =
    variant === "amber"
      ? "border-amber-500/20 bg-amber-500/10"
      : "border-red-500/20 bg-red-500/10";
  const titleClassName = variant === "amber" ? "text-amber-300" : "text-red-300";
  const bodyClassName = variant === "amber" ? "text-amber-100" : "text-red-100";
  const preClassName =
    variant === "amber"
      ? "border-amber-500/15 bg-black/20 text-amber-100"
      : "border-red-500/15 bg-black/20 text-red-100";

  return (
    <div className={`rounded-3xl border p-5 ${panelClassName}`}>
      <p className={`text-xs uppercase tracking-[0.3em] ${titleClassName}`}>{title}</p>
      <p className={`mt-2 text-sm leading-7 ${bodyClassName}`}>{description}</p>
      <pre className={`mt-4 overflow-x-auto rounded-2xl border p-4 text-sm leading-7 ${preClassName}`}>
        {value || "No additional error details were returned."}
      </pre>
    </div>
  );
}

function OutputCard({ accentClassName = "border-white/10", label, value }) {
  return (
    <div className={`rounded-2xl border p-4 ${accentClassName}`}>
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-words font-mono text-sm leading-7 text-slate-200">
        {value || "No value returned"}
      </pre>
    </div>
  );
}

function ResultBadge({ passed }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.3em] ${
        passed ? "bg-emerald-500/15 text-emerald-300" : "bg-red-500/15 text-red-300"
      }`}
    >
      <span
        className={`mr-2 h-2 w-2 rounded-full ${
          passed ? "bg-emerald-300" : "bg-red-300"
        }`}
      />
      {passed ? "Passed" : "Failed"}
    </span>
  );
}

export default RunResultsPanel;
