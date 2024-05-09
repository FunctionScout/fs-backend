package com.functionscout.backend.parser;

import com.functionscout.backend.dto.ClassDTO;
import com.functionscout.backend.dto.FunctionDTO;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ClassParser {
    public ClassDTO extractFunctions(final String className, final Path filePath) {
        try {
            final CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(filePath));
            final Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = "";

            if (packageDeclaration.isPresent()) {
                packageName = packageDeclaration.get().getNameAsString();
            }

            final VoidVisitor<List<FunctionDTO>> methodCollector = new MethodNameCollector();
            final List<FunctionDTO> functionDTOS = new ArrayList<>();
            methodCollector.visit(cu, functionDTOS);

            return new ClassDTO(packageName + "." + className, functionDTOS);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class MethodNameCollector extends VoidVisitorAdapter<List<FunctionDTO>> {

        @Override
        public void visit(MethodDeclaration md, List<FunctionDTO> collector) {
            super.visit(md, collector);
            collector.add(new FunctionDTO(
                    md.getNameAsString(),
                    md.getDeclarationAsString(true, false, false)
            ));
        }
    }
}
