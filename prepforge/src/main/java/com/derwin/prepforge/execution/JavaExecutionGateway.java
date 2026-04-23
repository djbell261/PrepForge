package com.derwin.prepforge.execution;

import com.derwin.prepforge.coding.dto.CompileError;
import com.derwin.prepforge.infrastructure.observability.PrepForgeMetrics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JavaExecutionGateway implements ExecutionGateway {
    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern JAVAC_ERROR_PATTERN =
            Pattern.compile("^(?:.+\\.java:)?(\\d+):\\s+error:\\s+(.+)$");
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(3);
    private static final String GENERIC_COMPILE_HINT =
            "Java could not compile this code. Review the error details and check your syntax, names, and types.";

    private final PrepForgeMetrics prepForgeMetrics;

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (request == null || request.getLanguage() == null || !request.getLanguage().equalsIgnoreCase("java")) {
            throw new IllegalArgumentException("Only Java execution is currently supported");
        }

        Path tempDir = null;
        String language = request.getLanguage().toLowerCase();
        prepForgeMetrics.incrementExecutionRequest(language);
        long startedAtNanos = System.nanoTime();

        try {
            tempDir = Files.createTempDirectory("prepforge-java-run-" + UUID.randomUUID());

            PreparedJavaCode preparedCode = prepareJavaCode(request.getSourceCode());
            Files.writeString(
                    tempDir.resolve(preparedCode.className() + ".java"),
                    preparedCode.sourceCode(),
                    StandardCharsets.UTF_8);

            Files.writeString(
                    tempDir.resolve("PrepForgeHarness.java"),
                    request.getHarnessSource().formatted(preparedCode.className()),
                    StandardCharsets.UTF_8);

            ProcessResult compileResult = runProcess(
                    List.of("javac", preparedCode.className() + ".java", "PrepForgeHarness.java"),
                    tempDir,
                    null);

            if (!compileResult.completedInTime()) {
                prepForgeMetrics.incrementExecutionTimeout(language, "compile");
                return buildCompileErrorResponse("Compilation timed out.", true);
            }

            if (compileResult.exitCode() != 0) {
                prepForgeMetrics.incrementExecutionCompileFailure(language);
                return buildCompileErrorResponse(trimToNull(compileResult.stderr()), false);
            }

            List<TestCaseExecutionResult> results = new ArrayList<>();
            List<ExecutionTestCase> testCases = request.getTestCases() == null ? List.of() : request.getTestCases();

            for (ExecutionTestCase testCase : testCases) {
                ProcessResult runResult = runProcess(
                        List.of("java", "PrepForgeHarness"),
                        tempDir,
                        testCase.getProgramInput());

                if (!runResult.completedInTime()) {
                    prepForgeMetrics.incrementExecutionTimeout(language, "run");
                    return buildRuntimeErrorResponse("Execution timed out.", true);
                }

                if (runResult.exitCode() != 0) {
                    prepForgeMetrics.incrementExecutionRuntimeFailure(language);
                    return buildRuntimeErrorResponse(trimToNull(runResult.stderr()), false);
                }

                String actualOutput = normalizeOutput(runResult.stdout());
                boolean passed = actualOutput.equals(normalizeOutput(testCase.getExpectedOutput()));

                results.add(TestCaseExecutionResult.builder()
                        .input(testCase.getDisplayInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .actualOutput(actualOutput)
                        .passed(passed)
                        .build());
            }

            long passedTests = results.stream()
                    .filter(TestCaseExecutionResult::isPassed)
                    .count();

            prepForgeMetrics.incrementExecutionSuccess(language);
            return ExecutionResult.builder()
                    .success(passedTests == results.size())
                    .passedTests((int) passedTests)
                    .totalTests(results.size())
                    .error(null)
                    .compileError(null)
                    .friendlyMessage(null)
                    .rawOutput(null)
                    .runtimeError(null)
                    .timedOut(false)
                    .testResults(results)
                    .build();
        } catch (IOException exception) {
            prepForgeMetrics.incrementExecutionRuntimeFailure(language);
            return buildRuntimeErrorResponse("Failed to execute Java code.", false, exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            prepForgeMetrics.incrementExecutionRuntimeFailure(language);
            return buildRuntimeErrorResponse("Execution was interrupted.", false);
        } finally {
            prepForgeMetrics.recordExecutionDuration(
                    language,
                    Duration.ofNanos(System.nanoTime() - startedAtNanos));
            deleteRecursively(tempDir);
        }
    }

    private PreparedJavaCode prepareJavaCode(String userCode) {
        String sanitizedCode = userCode == null ? "" : userCode.trim();

        Matcher publicClassMatcher = PUBLIC_CLASS_PATTERN.matcher(sanitizedCode);
        if (publicClassMatcher.find()) {
            return new PreparedJavaCode(publicClassMatcher.group(1), sanitizedCode);
        }

        Matcher classMatcher = CLASS_PATTERN.matcher(sanitizedCode);
        if (classMatcher.find()) {
            return new PreparedJavaCode(classMatcher.group(1), sanitizedCode);
        }

        List<String> importLines = new ArrayList<>();
        List<String> bodyLines = new ArrayList<>();

        for (String line : sanitizedCode.split("\\R")) {
            if (line.trim().startsWith("import ")) {
                importLines.add(line);
            } else {
                bodyLines.add(line);
            }
        }

        String wrappedSource = String.join("\n", importLines)
                + (importLines.isEmpty() ? "" : "\n\n")
                + "class Solution {\n"
                + String.join("\n", bodyLines)
                + "\n}\n";

        return new PreparedJavaCode("Solution", wrappedSource);
    }

    private ProcessResult runProcess(List<String> command, Path workingDirectory, String stdin)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());

        Process process = processBuilder.start();

        if (stdin != null) {
            process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        process.getOutputStream().close();

        boolean completed = process.waitFor(EXECUTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            return new ProcessResult(-1, "", "Process timed out.", false);
        }

        return new ProcessResult(
                process.exitValue(),
                readStream(process.getInputStream()),
                readStream(process.getErrorStream()),
                true);
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String normalizeOutput(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExecutionResult buildRuntimeErrorResponse(String runtimeError, boolean timedOut) {
        return buildRuntimeErrorResponse(runtimeError, timedOut, runtimeError);
    }

    private ExecutionResult buildRuntimeErrorResponse(String runtimeError, boolean timedOut, String rawOutput) {
        return ExecutionResult.builder()
                .success(false)
                .passedTests(null)
                .totalTests(null)
                .error(runtimeError)
                .compileError(null)
                .friendlyMessage(null)
                .rawOutput(rawOutput)
                .runtimeError(runtimeError)
                .timedOut(timedOut)
                .testResults(List.of())
                .build();
    }

    private ExecutionResult buildCompileErrorResponse(String rawCompilerOutput, boolean timedOut) {
        String normalizedOutput = trimToNull(rawCompilerOutput);
        CompileError compileError = parseCompileError(normalizedOutput);

        return ExecutionResult.builder()
                .success(false)
                .passedTests(null)
                .totalTests(null)
                .error(normalizedOutput)
                .compileError(compileError)
                .friendlyMessage(buildFriendlyCompilerMessage(normalizedOutput, timedOut))
                .rawOutput(normalizedOutput)
                .runtimeError(null)
                .timedOut(timedOut)
                .testResults(List.of())
                .build();
    }

    private CompileError parseCompileError(String rawCompilerOutput) {
        if (rawCompilerOutput == null) {
            return CompileError.builder()
                    .message("Compilation failed.")
                    .build();
        }

        String[] lines = rawCompilerOutput.split("\\R");

        for (int index = 0; index < lines.length; index++) {
            String currentLine = lines[index].trim();
            Matcher matcher = JAVAC_ERROR_PATTERN.matcher(currentLine);

            if (matcher.matches()) {
                Integer lineNumber = parseInteger(matcher.group(1));
                String message = matcher.group(2).trim();
                String codeLine = index + 1 < lines.length ? trimToNull(lines[index + 1]) : null;
                Integer column = null;

                if (index + 2 < lines.length && lines[index + 2].contains("^")) {
                    column = lines[index + 2].indexOf('^') + 1;
                }

                return CompileError.builder()
                        .line(lineNumber)
                        .column(column)
                        .message(message)
                        .codeLine(codeLine)
                        .build();
            }
        }

        return CompileError.builder()
                .message(extractFallbackCompileMessage(lines))
                .build();
    }

    private String extractFallbackCompileMessage(String[] lines) {
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.contains("error:")) {
                return trimmedLine.substring(trimmedLine.indexOf("error:") + "error:".length()).trim();
            }
        }

        return "Compilation failed.";
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildFriendlyCompilerMessage(String rawCompilerOutput, boolean timedOut) {
        if (timedOut) {
            return "Compilation took too long. Check for very large code blocks or syntax issues that may be confusing the compiler.";
        }

        if (rawCompilerOutput == null) {
            return GENERIC_COMPILE_HINT;
        }

        String normalizedOutput = rawCompilerOutput.toLowerCase();

        if (normalizedOutput.contains("';' expected")) {
            return "You may have forgotten a semicolon at the end of a statement.";
        }

        if (normalizedOutput.contains("cannot find symbol")) {
            return "Java found a name it does not recognize. Check for typos in variable names, method names, or class names, and make sure everything is declared before you use it.";
        }

        if (normalizedOutput.contains("incompatible types")) {
            return "Two values are being used as if they were the same type, but Java sees them as different. Check your variable types, method return types, and assignments.";
        }

        return GENERIC_COMPILE_HINT;
    }

    private void deleteRecursively(Path directory) {
        if (directory == null) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private record PreparedJavaCode(String className, String sourceCode) {
    }

    private record ProcessResult(int exitCode, String stdout, String stderr, boolean completedInTime) {
    }
}
