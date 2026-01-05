package testauto.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SourceCodeService {

    private static final String[] SOURCE_ROOTS = {
            "src/test/java",
            "src/main/java"
    };

    /**
     * 특정 메서드의 소스 코드를 추출.
     *
     * @param className 전체 클래스 이름 (예: testauto.testcode.SomeTest)
     * @param methodName 메서드 이름
     * @return 메서드 소스 코드
     */
    public String getMethodSourceCode(String className, String methodName) {
        log.info("Extracting method source code: class={}, method={}", className, methodName);

        try {
            File sourceFile = findSourceFile(className);
            if (sourceFile == null) {
                String error = "Source file not found for class: " + className;
                log.error(error);
                throw new IllegalArgumentException(error);
            }

            log.info("Found source file: {}", sourceFile.getAbsolutePath());

            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(sourceFile).getResult()
                    .orElseThrow(() -> new IllegalStateException("Failed to parse source file: " + sourceFile));

            log.info("Successfully parsed source file");

            // 메서드 찾기
            List<MethodDeclaration> allMethods = cu.findAll(MethodDeclaration.class);
            log.info("Found {} methods in class", allMethods.size());
            allMethods.forEach(m -> log.debug("  Method: {}", m.getNameAsString()));

            Optional<MethodDeclaration> methodOpt = allMethods.stream()
                    .filter(m -> matchesMethod(m, methodName))
                    .findFirst();

            if (methodOpt.isEmpty()) {
                String error = String.format("Method not found: '%s' in class: %s. Available methods: %s",
                    methodName, className, allMethods.stream().map(MethodDeclaration::getNameAsString).toList());
                log.error(error);
                throw new IllegalArgumentException(error);
            }

            MethodDeclaration method = methodOpt.get();
            log.info("Successfully extracted method: {}", method.getNameAsString());
            return method.toString();

        } catch (FileNotFoundException e) {
            log.error("Source file not found for class: {}", className, e);
            throw new IllegalArgumentException("Source file not found for class: " + className, e);
        } catch (Exception e) {
            log.error("Failed to extract method source code for class={}, method={}", className, methodName, e);
            throw new RuntimeException("Failed to extract method source code: " + e.getMessage(), e);
        }
    }

    /**
     * 클래스의 소스 파일을 찾음.
     */
    private File findSourceFile(String className) {
        String relativePath = className.replace('.', '/') + ".java";

        for (String root : SOURCE_ROOTS) {
            File file = new File(root, relativePath);
            if (file.exists() && file.isFile()) {
                log.debug("Found source file: {}", file.getAbsolutePath());
                return file;
            }
        }

        log.warn("Source file not found for class: {}", className);
        return null;
    }

    /**
     * 메서드가 주어진 이름과 일치하는지 확인.
     * JUnit의 displayName은 메서드명() 형태이거나 @DisplayName으로 커스터마이즈될 수 있음.
     */
    private boolean matchesMethod(MethodDeclaration method, String methodName) {
        // 메서드명이 정확히 일치하는 경우
        if (method.getNameAsString().equals(methodName)) {
            return true;
        }

        // 메서드명() 형태로 전달된 경우
        String nameWithParens = methodName.replaceAll("\\(\\)$", "");
        return method.getNameAsString().equals(nameWithParens);
    }
}
