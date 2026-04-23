package com.derwin.prepforge.coding.service;

import com.derwin.prepforge.coding.dto.RunCodeRequest;
import com.derwin.prepforge.coding.entity.CodingQuestion;
import com.derwin.prepforge.execution.ExecutionRequest;
import com.derwin.prepforge.execution.ExecutionTestCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CodingExecutionRequestFactory {

    /**
     * This factory owns question-specific execution inputs so the coding service can stay focused on
     * session orchestration while the execution boundary stays stable.
     */
    public ExecutionRequest build(CodingQuestion question, RunCodeRequest request) {
        return ExecutionRequest.builder()
                .language(request.getLanguage())
                .sourceCode(request.getSolutionCode())
                .harnessSource(buildHarnessSource(question))
                .testCases(buildExecutionTestCases(question))
                .build();
    }

    private List<ExecutionTestCase> buildExecutionTestCases(CodingQuestion question) {
        String normalizedTitle = question.getTitle() == null ? "" : question.getTitle().trim().toLowerCase();

        if ("two sum".equals(normalizedTitle)) {
            return List.of(
                    ExecutionTestCase.builder()
                            .displayInput("[2,7,11,15], 9")
                            .programInput("2 7 11 15\n9\n")
                            .expectedOutput("[0,1]")
                            .build(),
                    ExecutionTestCase.builder()
                            .displayInput("[3,2,4], 6")
                            .programInput("3 2 4\n6\n")
                            .expectedOutput("[1,2]")
                            .build()
            );
        }

        if ("valid palindrome".equals(normalizedTitle)) {
            return List.of(
                    ExecutionTestCase.builder()
                            .displayInput("\"A man, a plan, a canal: Panama\"")
                            .programInput("A man, a plan, a canal: Panama\n")
                            .expectedOutput("true")
                            .build(),
                    ExecutionTestCase.builder()
                            .displayInput("\"race a car\"")
                            .programInput("race a car\n")
                            .expectedOutput("false")
                            .build()
            );
        }

        return List.of();
    }

    private String buildHarnessSource(CodingQuestion question) {
        String normalizedTitle = question.getTitle() == null ? "" : question.getTitle().trim().toLowerCase();

        if ("two sum".equals(normalizedTitle)) {
            return """
                    import java.io.BufferedReader;
                    import java.io.InputStreamReader;
                    import java.lang.reflect.Method;
                    import java.util.Arrays;
                    import java.util.stream.IntStream;

                    public class PrepForgeHarness {
                        public static void main(String[] args) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            String numbersLine = reader.readLine();
                            String targetLine = reader.readLine();

                            int[] nums = Arrays.stream(numbersLine.trim().split("\\\\s+"))
                                    .filter(part -> !part.isBlank())
                                    .mapToInt(Integer::parseInt)
                                    .toArray();
                            int target = Integer.parseInt(targetLine.trim());

                            Class<?> solutionClass = Class.forName("%s");
                            Object solution = solutionClass.getDeclaredConstructor().newInstance();
                            Method method = solutionClass.getMethod("twoSum", int[].class, int.class);
                            int[] result = (int[]) method.invoke(solution, nums, target);

                            String output = IntStream.of(result)
                                    .mapToObj(String::valueOf)
                                    .reduce((left, right) -> left + "," + right)
                                    .map(value -> "[" + value + "]")
                                    .orElse("[]");

                            System.out.print(output);
                        }
                    }
                    """;
        }

        if ("valid palindrome".equals(normalizedTitle)) {
            return """
                    import java.io.BufferedReader;
                    import java.io.InputStreamReader;
                    import java.lang.reflect.Method;

                    public class PrepForgeHarness {
                        public static void main(String[] args) throws Exception {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                            String value = reader.readLine();

                            Class<?> solutionClass = Class.forName("%s");
                            Object solution = solutionClass.getDeclaredConstructor().newInstance();
                            Method method = solutionClass.getMethod("isPalindrome", String.class);
                            boolean result = (boolean) method.invoke(solution, value);

                            System.out.print(result);
                        }
                    }
                    """;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported coding question for execution");
    }
}
