package com.functionscout.backend.parser;

import com.functionscout.backend.client.GithubClient;
import com.functionscout.backend.dto.ClassDTO;
import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.dto.FunctionDTO;
import com.functionscout.backend.dto.GithubPomFileContentResponse;
import com.functionscout.backend.dto.UsedFunctionDependency;
import com.functionscout.backend.dto.UsedFunctionDependencyFromDB;
import com.functionscout.backend.dto.WebServiceClassData;
import com.functionscout.backend.enums.DependencyType;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.model.Class;
import com.functionscout.backend.model.Function;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.model.WebServiceFunctionDependency;
import com.functionscout.backend.repository.ClassRepository;
import com.functionscout.backend.repository.FunctionRepository;
import com.functionscout.backend.repository.JdbcClassRepository;
import com.functionscout.backend.repository.JdbcFunctionRepository;
import com.functionscout.backend.repository.WebServiceFunctionDependencyRepository;
import com.functionscout.backend.repository.WebServiceRepository;
import com.functionscout.backend.service.DependencyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private JdbcClassRepository jdbcClassRepository;

    @Autowired
    private FunctionRepository functionRepository;

    @Autowired
    private JdbcFunctionRepository jdbcFunctionRepository;

    @Autowired
    private WebServiceFunctionDependencyRepository webServiceFunctionDependencyRepository;

    @Value("${functionscout.github.pat-token}")
    private String githubPatToken;

    @Value("${functionscout.github.api-version}")
    private String githubApiVersion;

    @Value("${functionscout.github.accept-header}")
    private String githubAcceptHeader;

    @Async
    public void processGithubUrl(final WebService webService, final String owner, final String repository) {
        try {
            final String tempDirectory = System.getProperty("java.io.tmpdir");
            final String folderName = tempDirectory.charAt(tempDirectory.length() - 1) == '/'
                    ? tempDirectory + UUID.randomUUID()
                    : tempDirectory + "/" + UUID.randomUUID();

            log.info("Folder name: " + folderName);

            final File directory = new File(folderName);

            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new Exception("Unable to create directory");
                }
            }

            extractDependencies(webService, owner, repository);

            // Clone repository
            log.info("Cloning repository...");
            String githubUrl = webService.getGithubUrl();
            githubUrl = "https://oauth2:" + githubPatToken + "@" + githubUrl.substring(githubUrl.indexOf("https://") + 8);

            try (final Git result = Git.cloneRepository()
                    .setURI(githubUrl)
                    .setDirectory(directory)
                    .setProgressMonitor(new SimpleGitProgressMonitor())
                    .call()) {
                log.info("Repository Details: " + result.getRepository().getDirectory());
            } catch (Exception ex) {
                throw new Exception("Unable to clone github repository: " + webService.getGithubUrl());
            }

            final List<UsedFunctionDependency> usedFunctionDependencies = new ArrayList<>();
            final List<ClassDTO> classDTOS = new ArrayList<>();

            scanRepository(folderName, usedFunctionDependencies, classDTOS);

            saveWebServiceFunctionDependencies(usedFunctionDependencies, webService);
            saveFunctions(classDTOS, webService);

            FileUtils.deleteDirectory(directory);
        } catch (Exception ex) {
            ex.printStackTrace();
            updateWebServiceStatusToFailed(webService);
        }
    }

    private void scanRepository(final String folderName,
                                final List<UsedFunctionDependency> usedFunctionDependencies,
                                final List<ClassDTO> classDTOS) {
        // TODO: This fetches all classes. We might not need classes from the same service
        final Map<String, Integer> classMap = this.jdbcClassRepository.findAll()
                .stream()
                .collect(Collectors.toMap(WebServiceClassData::getClassName, WebServiceClassData::getServiceId));

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
                        final ClassDTO classDTO = new ClassDTO();

                        classParser.parse(
                                fileName.substring(0, fileName.indexOf(".java")),
                                filePath,
                                classMap,
                                usedFunctionDependencies,
                                classDTO
                        );

                        classDTOS.add(classDTO);
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
    }

    private void extractDependencies(final WebService webService,
                                     final String owner,
                                     final String repository) {
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
            DocumentBuilder builder;

            builder = factory.newDocumentBuilder();
            final Document document = builder.parse(new InputSource(new StringReader(pomContent)));
            final Element rootElement = document.getDocumentElement();

            final NodeList groupIdNodes = rootElement.getElementsByTagName("groupId");
            final NodeList artifactIdNodes = rootElement.getElementsByTagName("artifactId");
            String projectGroupId = "";
            String projectArtifactId = "";

            for (int index = 0; index < groupIdNodes.getLength(); index++) {
                Element node = (Element) groupIdNodes.item(index);

                if (node.getParentNode() != null && node.getParentNode().getNodeName().equals("project")) {
                    projectGroupId = node.getTextContent();
                    break;
                }
            }

            for (int index = 0; index < artifactIdNodes.getLength(); index++) {
                Element node = (Element) artifactIdNodes.item(index);

                if (node.getParentNode() != null && node.getParentNode().getNodeName().equals("project")) {
                    projectArtifactId = node.getTextContent();
                    break;
                }
            }

            webService.setName(projectGroupId + "." + projectArtifactId);

            final Element dependenciesElement = (Element) rootElement.getElementsByTagName("dependencies").item(0);
            final NodeList dependencyElements = dependenciesElement.getElementsByTagName("dependency");
            final List<DependencyDTO> webServiceDependencies = new ArrayList<>();

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
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void updateWebServiceStatusToFailed(final WebService webService) {
        webService.setStatus(Status.FAILED.getCode());
        webService.setUniqueHash(webService.getUuid());
        webService.setDependencies(new HashSet<>());
        webServiceRepository.save(webService);
    }

    private void saveWebServiceFunctionDependencies(final List<UsedFunctionDependency> usedFunctionDependencies,
                                                    final WebService webService) {
        final List<UsedFunctionDependencyFromDB> usedFunctionDependencyFromDBS =
                jdbcFunctionRepository.findAllFunctionsByServiceIdAndFunctionId(usedFunctionDependencies);
        final List<WebServiceFunctionDependency> webServiceFunctionDependencies = new ArrayList<>();

        for (UsedFunctionDependencyFromDB usedFunctionDependencyFromDB : usedFunctionDependencyFromDBS) {
            webServiceFunctionDependencies.add(
                    new WebServiceFunctionDependency(
                            webService.getId(),
                            usedFunctionDependencyFromDB.getWebServiceDependencyId(),
                            usedFunctionDependencyFromDB.getFunctionId()
                    )
            );
        }

        webServiceFunctionDependencyRepository.saveAll(webServiceFunctionDependencies);
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
                        .map(functionDTO -> new Function(
                                functionDTO.getName(),
                                functionDTO.getSignature(),
                                functionDTO.getReturnType(),
                                clazz))
                )
                .collect(Collectors.toList());

        functionRepository.saveAll(functions);

        webService.setStatus(Status.SUCCESS.getCode());
        webServiceRepository.save(webService);
    }
}