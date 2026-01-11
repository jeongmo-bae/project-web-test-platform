package testauto.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SourceCodeService {

    private static final String[] SOURCE_ROOTS = {
            "src/test/java",
            "src/main/java"
    };

    // uniqueId 파싱용 정규식
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\[class:([^\\]]+)\\]");
    private static final Pattern NESTED_CLASS_PATTERN = Pattern.compile("\\[nested-class:([^\\]]+)\\]");
    private static final Pattern METHOD_PATTERN = Pattern.compile("\\[method:([^\\]]+)\\]");

    @Value("${testcode.project-path:}")
    private String testcodeProjectPath;

    /**
     * uniqueId를 기반으로 메서드 소스 코드를 추출.
     *
     * @param uniqueId JUnit Platform의 uniqueId
     *                 예: [engine:junit-jupiter]/[class:com.example.Test]/[nested-class:Inner]/[method:test1()]
     * @return 메서드 소스 코드
     */
    public String getMethodSourceCodeByUniqueId(String uniqueId) {
        log.info("Extracting method source code by uniqueId: {}", uniqueId);

        ParsedUniqueId parsed = parseUniqueId(uniqueId);
        log.info("Parsed uniqueId: className={}, nestedClasses={}, methodName={}",
                parsed.className, parsed.nestedClasses, parsed.methodName);

        try {
            File sourceFile = findSourceFile(parsed.className);
            if (sourceFile == null) {
                throw new IllegalArgumentException("Source file not found for class: " + parsed.className);
            }

            log.info("Found source file: {}", sourceFile.getAbsolutePath());

            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(sourceFile).getResult()
                    .orElseThrow(() -> new IllegalStateException("Failed to parse source file: " + sourceFile));

            // 메서드를 찾을 대상 클래스 결정
            List<MethodDeclaration> targetMethods = findTargetMethods(cu, parsed.nestedClasses);

            log.info("Found {} methods in target scope", targetMethods.size());

            Optional<MethodDeclaration> methodOpt = targetMethods.stream()
                    .filter(m -> matchesMethod(m, parsed.methodName))
                    .findFirst();

            if (methodOpt.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Method not found: '%s'. Available methods: %s",
                        parsed.methodName,
                        targetMethods.stream().map(MethodDeclaration::getNameAsString).toList()));
            }

            MethodDeclaration method = methodOpt.get();
            log.info("Successfully extracted method: {}", method.getNameAsString());
            return method.toString();

        } catch (FileNotFoundException e) {
            log.error("Source file not found", e);
            throw new IllegalArgumentException("Source file not found for class: " + parsed.className, e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract method source code for uniqueId={}", uniqueId, e);
            throw new RuntimeException("Failed to extract method source code: " + e.getMessage(), e);
        }
    }

    /**
     * uniqueId를 파싱하여 className, nestedClasses, methodName 추출.
     */
    private ParsedUniqueId parseUniqueId(String uniqueId) {
        // className 추출
        Matcher classMatcher = CLASS_PATTERN.matcher(uniqueId);
        if (!classMatcher.find()) {
            throw new IllegalArgumentException("Invalid uniqueId: class not found in " + uniqueId);
        }
        String className = classMatcher.group(1);

        // nested classes 추출 (여러 개일 수 있음)
        List<String> nestedClasses = new ArrayList<>();
        Matcher nestedMatcher = NESTED_CLASS_PATTERN.matcher(uniqueId);
        while (nestedMatcher.find()) {
            nestedClasses.add(nestedMatcher.group(1));
        }

        // methodName 추출
        Matcher methodMatcher = METHOD_PATTERN.matcher(uniqueId);
        if (!methodMatcher.find()) {
            throw new IllegalArgumentException("Invalid uniqueId: method not found in " + uniqueId);
        }
        String methodName = methodMatcher.group(1);
        // 괄호 제거: test1() -> test1
        methodName = methodName.replaceAll("\\(.*\\)$", "");

        return new ParsedUniqueId(className, nestedClasses, methodName);
    }

    /**
     * nested class 경로를 따라 메서드를 찾을 대상을 결정.
     */
    private List<MethodDeclaration> findTargetMethods(CompilationUnit cu, List<String> nestedClasses) {
        if (nestedClasses.isEmpty()) {
            // nested class가 없으면 최상위 클래스의 메서드만 반환
            return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(c -> !c.isNestedType())
                    .flatMap(c -> c.getMethods().stream())
                    .toList();
        }

        // nested class 경로를 따라 탐색
        ClassOrInterfaceDeclaration currentClass = null;
        List<ClassOrInterfaceDeclaration> searchScope = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (String nestedClassName : nestedClasses) {
            Optional<ClassOrInterfaceDeclaration> found = searchScope.stream()
                    .filter(c -> c.getNameAsString().equals(nestedClassName))
                    .findFirst();

            if (found.isEmpty()) {
                throw new IllegalArgumentException("Nested class not found: " + nestedClassName);
            }

            currentClass = found.get();
            // 다음 탐색 범위는 현재 클래스의 내부 클래스들
            searchScope = currentClass.findAll(ClassOrInterfaceDeclaration.class);
        }

        return currentClass != null ? currentClass.getMethods() : List.of();
    }

    /**
     * 클래스의 소스 파일을 찾음.
     */
    private File findSourceFile(String className) {
        String relativePath = className.replace('.', '/') + ".java";

        // testcode.project-path 설정 경로에서 찾기
        if (testcodeProjectPath != null && !testcodeProjectPath.isEmpty()) {
            for (String root : SOURCE_ROOTS) {
                File file = new File(testcodeProjectPath, root + "/" + relativePath);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }

        // 현재 디렉토리 기준
        for (String root : SOURCE_ROOTS) {
            File file = new File(root, relativePath);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        return null;
    }

    /**
     * 메서드가 주어진 이름과 일치하는지 확인.
     */
    private boolean matchesMethod(MethodDeclaration method, String methodName) {
        return method.getNameAsString().equals(methodName);
    }

    /**
     * uniqueId 파싱 결과를 담는 레코드.
     */
    private record ParsedUniqueId(String className, List<String> nestedClasses, String methodName) {}
}
