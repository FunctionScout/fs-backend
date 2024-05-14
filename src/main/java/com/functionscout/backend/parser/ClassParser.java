package com.functionscout.backend.parser;

import com.functionscout.backend.dto.ClassDTO;
import com.functionscout.backend.dto.FunctionDTO;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsibilities of this class:
 * <p>
 * Extract all public functions
 * Extract all imports
 * Filter only saved imports
 * Find the variables
 * Find their usages
 * Build a map of service and list of functions used from it
 * Save it in the database
 */
@Component
public class ClassParser {
    int a;

    public ClassDTO extractFunctions(final String className,
                                     final Path filePath,
                                     final Set<String> classNames) {
        try {
            final CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(filePath));
            final Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = "";

            if (packageDeclaration.isPresent()) {
                packageName = packageDeclaration.get().getNameAsString();
            }

            processImports(cu, classNames);

            final List<FunctionDTO> functionDTOS = getFunctions(cu);

            return new ClassDTO(packageName + "." + className, functionDTOS);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void processImports(final CompilationUnit cu, final Set<String> classNames) {
        final Map<String, String> usedImportsMap = cu.getImports()
                .stream()
                .map(NodeWithName::getNameAsString)
                .filter(importName -> !importName.startsWith("java.lang"))
                .filter(classNames::contains)
                .collect(Collectors.toMap(importName ->
                                importName.substring(importName.lastIndexOf(".") + 1),
                        Function.identity()
                ));

        final Map<String, VariableDeclarator> fieldDeclarationMap = cu.findAll(FieldDeclaration.class)
                .stream()
                .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                .filter(variableDeclarator -> variableDeclarator.getType().isReferenceType())
                .collect(Collectors.toMap(NodeWithSimpleName::getNameAsString, Function.identity()));

        cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
            final Optional<Expression> optionalExpression = methodCallExpr.getScope();

            if (optionalExpression.isPresent()) {
                final Expression expression = optionalExpression.get();

                if (expression.isNameExpr() || expression.isFieldAccessExpr()) {
                    final String variableName = expression.isNameExpr()
                            ? expression.asNameExpr().getNameAsString()
                            : expression.asFieldAccessExpr().getNameAsString();

                    // TODO: Look for static functions as well
                    if (fieldDeclarationMap.containsKey(variableName)) {
                        final String functionName = methodCallExpr.getNameAsString();

                        // TODO: Change this to a list
                        // TODO: Remove isNameExpr filter. Accept all expression types and later resolve type only for NameExpr
                        final Map<String, Expression> argumentMap = new LinkedHashMap<>(methodCallExpr.getArguments()
                                .stream()
                                .filter(Expression::isNameExpr)
                                .collect(Collectors.toMap(
                                        expr -> expr.asNameExpr().getNameAsString(), Function.identity())
                                )
                        );
                        final Map<String, String> argumentTypeMap = new HashMap<>();

                        argumentMap.forEach((argument, expr) -> {
                            final String argumentType = resolveArgumentType(argument, expr, fieldDeclarationMap);
                            System.out.println("Variable: " + argument + " Type: " + argumentType);
                            argumentTypeMap.put(argument, argumentType);
                        });
                    }
                }
            }
        });
    }

    private String resolveArgumentType(final String argument,
                                       final Expression expression,
                                       final Map<String, VariableDeclarator> fieldDeclarationMap) {
        Optional<Node> optionalNode = expression.getParentNode();
        Optional<Node> previousCheckpointNode = Optional.empty();

        // 1. It is defined within the scope of the function
        while (optionalNode.isPresent()) {
            final Node node = optionalNode.get();

            // Checkpoint
            if (node instanceof BlockStmt) {
                previousCheckpointNode = optionalNode;

                for (Node blockChildNode : node.getChildNodes()) {
                    if (blockChildNode instanceof ExpressionStmt) {
                        blockChildNode = ((ExpressionStmt) blockChildNode).asExpressionStmt();
                        Optional<VariableDeclarator> matchedVariableDeclarator = blockChildNode.findAll(VariableDeclarator.class)
                                .stream()
                                .filter(variableDeclarator -> variableDeclarator.getNameAsString().equals(argument))
                                .findFirst();

                        if (matchedVariableDeclarator.isPresent()) {
                            return matchedVariableDeclarator.get().getType().asString();
                        }
                    }
                }

                break;
            }

            optionalNode = node.getParentNode();
        }

        if (previousCheckpointNode.isPresent()) {
            optionalNode = previousCheckpointNode.get().getParentNode();
        }

        // 2. It is a function argument
        while (optionalNode.isPresent()) {
            final Node node = optionalNode.get();

            // Checkpoint
            if (node instanceof MethodDeclaration) {
                final NodeList<Parameter> parameters = ((MethodDeclaration) node).getParameters();

                for (Parameter parameter : parameters) {
                    if (parameter.getNameAsString().equals(argument)) {
                        return parameter.getType().asString();
                    }
                }

                break;
            }

            optionalNode = node.getParentNode();
        }

        if (fieldDeclarationMap.containsKey(argument)) {
            return fieldDeclarationMap.get(argument).getType().asString();
        }

        return "";
    }

    private List<FunctionDTO> getFunctions(final CompilationUnit cu) {
        final VoidVisitor<List<FunctionDTO>> methodCollector = new MethodNameCollector();
        final List<FunctionDTO> functionDTOS = new ArrayList<>();
        methodCollector.visit(cu, functionDTOS);

        return functionDTOS;
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
