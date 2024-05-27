package com.functionscout.backend.parser;

import com.functionscout.backend.dto.ClassDTO;
import com.functionscout.backend.dto.FunctionDTO;
import com.functionscout.backend.dto.UsedFunctionDependency;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClassParser {

    public void parse(final String className,
                      final Path filePath,
                      final Map<String, Integer> classMap,
                      final List<UsedFunctionDependency> usedFunctionDependencies,
                      final ClassDTO classDTO,
                      final boolean isReverseScan) {
        try {
            final CompilationUnit cu = StaticJavaParser.parse(Files.newInputStream(filePath));
            final Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = "";

            if (packageDeclaration.isPresent()) {
                packageName = packageDeclaration.get().getNameAsString();
            }

            usedFunctionDependencies.addAll(getUsedFunctionDependencies(cu, classMap));

            if (!isReverseScan) {
                final List<FunctionDTO> functionDTOS = getFunctionDeclarations(cu);
                classDTO.setClassName(packageName + "." + className);
                classDTO.setFunctionDTOList(functionDTOS);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<UsedFunctionDependency> getUsedFunctionDependencies(final CompilationUnit cu,
                                                                     final Map<String, Integer> classMap) {
        if (classMap.isEmpty()) {
            return new ArrayList<>();
        }

        final Map<String, String> usedImportsMap = cu.getImports()
                .stream()
                .map(NodeWithName::getNameAsString)
                .filter(importName -> !importName.startsWith("java.lang"))
                .filter(classMap::containsKey)
                .collect(Collectors.toMap(importName ->
                                importName.substring(importName.lastIndexOf(".") + 1),
                        Function.identity()
                ));
        final Map<String, VariableDeclarator> fieldDeclarationMap = cu.findAll(FieldDeclaration.class)
                .stream()
                .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
                .filter(variableDeclarator -> variableDeclarator.getType().isReferenceType()
                        && usedImportsMap.containsKey(variableDeclarator.getTypeAsString()))
                .collect(Collectors.toMap(NodeWithSimpleName::getNameAsString, Function.identity()));
        final List<UsedFunctionDependency> usedFunctionDependencies = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
            final Optional<Expression> optionalExpression = methodCallExpr.getScope();

            if (optionalExpression.isPresent()) {
                final Expression expression = optionalExpression.get();

                if (expression.isNameExpr() || expression.isFieldAccessExpr()) {
                    final String variableName = expression.isNameExpr()
                            ? expression.asNameExpr().getNameAsString()
                            : expression.asFieldAccessExpr().getNameAsString();

                    if (fieldDeclarationMap.containsKey(variableName)) {
                        final String functionName = methodCallExpr.getNameAsString();
                        final NodeList<Expression> arguments = methodCallExpr.getArguments();
                        final List<String> argumentTypes = new LinkedList<>();

                        arguments.forEach(expr -> {
                            if (expr.isNameExpr()) {
                                final String argument = expr.asNameExpr().getNameAsString();
                                final String argumentType = resolveArgumentType(argument, expr, fieldDeclarationMap);
                                argumentTypes.add(argumentType);
                            } else if (expr.isClassExpr() || expr.isObjectCreationExpr()) {
                                final String type = expr.isClassExpr()
                                        ? expr.asClassExpr().getTypeAsString()
                                        : expr.asObjectCreationExpr().getTypeAsString();

                                if (usedImportsMap.containsKey(type)) {
                                    argumentTypes.add(type);
                                }
                            } else if (expr.isStringLiteralExpr()) {
                                argumentTypes.add("String");
                            } else if (expr.isIntegerLiteralExpr()) {
                                argumentTypes.add("int");
                            } else if (expr.isDoubleLiteralExpr()) {
                                argumentTypes.add("double");
                            } else if (expr.isBooleanLiteralExpr()) {
                                argumentTypes.add("boolean");
                            } else if (expr.isLongLiteralExpr()) {
                                argumentTypes.add("long");
                            } else if (expr.isCharLiteralExpr()) {
                                argumentTypes.add("char");
                            }
                        });

                        int argumentCount = argumentTypes.size();
                        final StringBuilder signatureBuilder = new StringBuilder();
                        signatureBuilder.append(functionName).append("(");

                        for (String argumentType : argumentTypes) {
                            signatureBuilder.append(argumentType);

                            if (argumentCount > 1) {
                                signatureBuilder.append(", ");
                            }

                            argumentCount--;
                        }

                        signatureBuilder.append(")");

                        final String variableType = fieldDeclarationMap.get(variableName).getTypeAsString();
                        final String className = usedImportsMap.get(variableType);
                        final Integer serviceId = classMap.get(className);

                        usedFunctionDependencies.add(new UsedFunctionDependency(
                                serviceId,
                                className,
                                signatureBuilder.toString())
                        );
                    } else {
                        // TODO: Look for static functions as well
                    }
                }
            }
        });

        return usedFunctionDependencies;
    }

    private String resolveArgumentType(final String argument,
                                       final Expression expression,
                                       final Map<String, VariableDeclarator> fieldDeclarationMap) {
        Optional<Node> node = expression.getParentNode();
        Optional<Node> previousNode = Optional.empty();

        // 1. Function body
        while (node.isPresent()) {
            final Node unWrappedNode = node.get();

            if (unWrappedNode instanceof BlockStmt) {
                previousNode = node;

                for (Node blockChildNode : unWrappedNode.getChildNodes()) {
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

            node = unWrappedNode.getParentNode();
        }

        if (previousNode.isPresent()) {
            node = previousNode.get().getParentNode();
        }

        // 2. Function arguments
        while (node.isPresent()) {
            final Node unWrappedNode = node.get();

            if (unWrappedNode instanceof MethodDeclaration) {
                final NodeList<Parameter> parameters = ((MethodDeclaration) unWrappedNode).getParameters();

                for (Parameter parameter : parameters) {
                    if (parameter.getNameAsString().equals(argument)) {
                        return parameter.getType().asString();
                    }
                }

                break;
            }

            node = unWrappedNode.getParentNode();
        }

        // 3. Class fields
        if (fieldDeclarationMap.containsKey(argument)) {
            return fieldDeclarationMap.get(argument).getType().asString();
        }

        return "";
    }

    private List<FunctionDTO> getFunctionDeclarations(final CompilationUnit cu) {
        final VoidVisitor<List<FunctionDTO>> methodCollector = new MethodNameCollector();
        final List<FunctionDTO> functionDTOS = new ArrayList<>();
        methodCollector.visit(cu, functionDTOS);

        return functionDTOS;
    }

    private static class MethodNameCollector extends VoidVisitorAdapter<List<FunctionDTO>> {

        @Override
        public void visit(MethodDeclaration md, List<FunctionDTO> collector) {
            super.visit(md, collector);

            if (md.getAccessSpecifier().equals(AccessSpecifier.PUBLIC)) {
                final String signature = md.getDeclarationAsString(
                        false, false, false
                );

                collector.add(new FunctionDTO(
                        md.getNameAsString(),
                        signature.substring(signature.indexOf(" ") + 1),
                        md.getTypeAsString()
                ));
            }
        }
    }
}
