package com.functionscout.backend.service;

import com.functionscout.backend.client.GithubClient;
import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.dto.GithubPomFileContentResponse;
import com.functionscout.backend.enums.DependencyType;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.model.WebService;
import com.functionscout.backend.repository.WebServiceRepository;
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
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebServiceProcessor {

    @Autowired
    private WebServiceRepository webServiceRepository;

    @Autowired
    private GithubClient githubClient;

    @Autowired
    private DependencyService dependencyService;

    @Value("${functionscout.github.pat-token}")
    private String githubPatToken;

    @Value("${functionscout.github.api-version}")
    private String githubApiVersion;

    @Value("${functionscout.github.accept-header}")
    private String githubAcceptHeader;

    @Async
    public void processGithubUrl(final WebService webService) {
        // Use Github API to get the repository information
        // After everything is done, insert the record to webservice table and update the status in webservicestatus to success

        try {
            Pattern pattern = Pattern.compile("^(https://github.com/)([\\w_-]+)/([\\w.-]+)\\.(git)?$");
            Matcher matcher = pattern.matcher(webService.getGithubUrl());

            //TODO: Validity of a github url should be checked in webservice and not here
            if (!matcher.matches()) {
                System.out.println("Not matching");
            }

            final String owner = matcher.group(2);
            final String repository = matcher.group(3);

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

            extractDependenciesFromPom(webService, pomContent);
        } catch (Exception ex) {
            ex.printStackTrace();
            webService.setStatus(Status.FAILED.getCode());
            webService.setUniqueHash(webService.getUuid());
            webServiceRepository.save(webService);
        }
    }

    private void extractDependenciesFromPom(final WebService webService, final String pomContent) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        final List<DependencyDTO> webServiceDependencies = new ArrayList<>();

        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(pomContent)));
            Element rootElement = document.getDocumentElement();
            Element dependenciesElement = (Element) rootElement.getElementsByTagName("dependencies").item(0);
            NodeList dependencyElements = dependenciesElement.getElementsByTagName("dependency");

            for (int index = 0; index < dependencyElements.getLength(); index++) {
                Element dependencyElement = (Element) dependencyElements.item(index);

                // Get the groupId element
                Element groupIdElement = (Element) dependencyElement.getElementsByTagName("groupId").item(0);
                String groupId = groupIdElement.getTextContent();

                // Get the artifactId element
                Element artifactIdElement = (Element) dependencyElement.getElementsByTagName("artifactId").item(0);
                String artifactId = artifactIdElement.getTextContent();

                // Get the version element
                Element versionElement = (Element) dependencyElement.getElementsByTagName("version").item(0);
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
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        dependencyService.createDependencies(webService, webServiceDependencies);
    }
}