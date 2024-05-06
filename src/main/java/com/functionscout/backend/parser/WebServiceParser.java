package com.functionscout.backend.parser;

import com.functionscout.backend.client.GithubClient;
import com.functionscout.backend.dto.ClassDTO;
import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.dto.FunctionDTO;
import com.functionscout.backend.dto.GithubPomFileContentResponse;
import com.functionscout.backend.enums.DependencyType;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.model.Class;
import com.functionscout.backend.model.Function;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.repository.ClassRepository;
import com.functionscout.backend.repository.FunctionRepository;
import com.functionscout.backend.repository.WebServiceRepository;
import com.functionscout.backend.service.DependencyService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class WebServiceParser {

    @Autowired
    private WebServiceRepository webServiceRepository;

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private ClassParser classParser;

    @Autowired
    private DependencyService dependencyService;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Value("${functionscout.github.pat-token}")
    private String githubPatToken;

    @Value("${functionscout.github.api-version}")
    private String githubApiVersion;

    @Value("${functionscout.github.accept-header}")
    private String githubAcceptHeader;

    @Async
    public void processGithubUrl(final WebService webService, final String owner, final String repository) {
        try {
            final String folderName = System.getProperty("java.io.tmpdir") + UUID.randomUUID();

            log.info("Folder name: " + folderName);

            final File directory = new File(folderName);

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new Exception("Unable to create directory");
                }
            }

            if (!extractDependencies(webService, owner, repository)) {
                updateWebServiceStatusToFailed(webService);
                return;
            }

            // Clone repository
            final ProcessBuilder builder = new ProcessBuilder()
                    .inheritIO()
                    .directory(directory)
                    .command("git", "clone", webService.getGithubUrl());
            final Process process = builder.start();

            int exitVal = process.waitFor();

            if (exitVal != 0) {
                throw new Exception("Unable to clone github repository: " + webService.getGithubUrl());
            }

            final List<ClassDTO> classDTOS = scanRepository(folderName);
            saveFunctions(classDTOS, webService);
            deleteRepository(folderName);
        } catch (Exception ex) {
            ex.printStackTrace();
            updateWebServiceStatusToFailed(webService);
        }
    }

    private List<ClassDTO> scanRepository(final String folderName) {
        final List<ClassDTO> classDTOS = new ArrayList<>();
        final SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path directoryPath, BasicFileAttributes attrs) {
                final String directoryName = directoryPath.getFileName().toString();

                // Skip hidden and test directories
                if (directoryName.startsWith(".") || directoryName.startsWith("test")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                if (Files.isRegularFile(filePath)) {
                    final String fileName = filePath.getFileName().toString();

                    if (fileName.endsWith(".java")) {
                        // TODO: A project can have multiple classes with same name. Use 'package + class' as a unique identifier
                        classDTOS.add(new ClassDTO(fileName.substring(0, fileName.indexOf(".java")), classParser.extractFunctions(filePath)));
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(Paths.get(folderName), fileVisitor);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return classDTOS;
    }

    private void deleteRepository(final String folderName) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(folderName))) {
            pathStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            System.out.println(("Unable to delete file: " + file.getPath()));
                        }
                    });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean extractDependencies(final WebService webService, final String owner, final String repository) {
        try {
            final GithubPomFileContentResponse githubPomFileContentResponse = githubClient.getRepositoryContentForFile(
                    "Bearer " + githubPatToken,
                    githubApiVersion,
                    githubAcceptHeader,
                    owner,
                    repository,
                    "pom.xml"
            );
            final Base64.Decoder decoder = Base64.getMimeDecoder();
            final String pomContent = new String(
                    decoder.decode(githubPomFileContentResponse.getContent()),
                    StandardCharsets.UTF_8
            );

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final List<DependencyDTO> webServiceDependencies = new ArrayList<>();
            DocumentBuilder builder;

            builder = factory.newDocumentBuilder();
            final Document document = builder.parse(new InputSource(new StringReader(pomContent)));
            final Element rootElement = document.getDocumentElement();
            final Element dependenciesElement = (Element) rootElement.getElementsByTagName("dependencies").item(0);
            final NodeList dependencyElements = dependenciesElement.getElementsByTagName("dependency");

            for (int index = 0; index < dependencyElements.getLength(); index++) {
                final Element dependencyElement = (Element) dependencyElements.item(index);

                // Get the groupId element
                final Element groupIdElement = (Element) dependencyElement.getElementsByTagName("groupId").item(0);
                final String groupId = groupIdElement.getTextContent();

                // Get the artifactId element
                final Element artifactIdElement = (Element) dependencyElement.getElementsByTagName("artifactId").item(0);
                final String artifactId = artifactIdElement.getTextContent();

                // Get the version element
                final Element versionElement = (Element) dependencyElement.getElementsByTagName("version").item(0);
                String version = "V0";

                if (versionElement != null) {
                    version = versionElement.getTextContent();
                }

                webServiceDependencies.add(new DependencyDTO(
                        groupId + "." + artifactId,
                        version,
                        DependencyType.EXTERNAL.getType()
                ));
            }

            dependencyService.createDependencies(webService, webServiceDependencies);
        } catch (FeignException fex) {
            log.error("Exception while fetching pom content from Github");
            fex.printStackTrace();

            return false;
        } catch (Exception ex) {
            log.error("Exception while parsing pom content");
            ex.printStackTrace();

            return false;
        }

        return true;
    }

    private void updateWebServiceStatusToFailed(final WebService webService) {
        webService.setStatus(Status.FAILED.getCode());
        webService.setUniqueHash(webService.getUuid());
        webServiceRepository.save(webService);
    }

    private void saveFunctions(final List<ClassDTO> classDTOS, final WebService webService) {
        List<Class> classes = classDTOS.stream()
                .map(classDTO -> new Class(classDTO.getClassName(), webService))
                .toList();
        classes = classRepository.saveAll(classes);
        classRepository.flush();

        final Map<String, List<FunctionDTO>> classFunctionMap = classDTOS.stream()
                .collect(Collectors.toMap(ClassDTO::getClassName, ClassDTO::getFunctionDTOList));
        final List<Function> functions = classes.stream()
                .flatMap(clazz -> classFunctionMap.get(clazz.getName())
                        .stream()
                        .map(functionDTO -> new Function(functionDTO.getName(), functionDTO.getSignature(), clazz)))
                .collect(Collectors.toList());

        functionRepository.saveAll(functions);

        webService.setStatus(Status.SUCCESS.getCode());
        webServiceRepository.save(webService);
    }
}