package com.epam.azn;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.epam.azn.pojo.JiraFieldMetadata;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class JiraFieldMetadataCache {
    private final static String FIELDS_REST_API_URL = "/rest/api/2/field";

    private final HashMap<String, JiraFieldMetadata> fieldMetadata = new HashMap<>();

    private final ApplicationLinkService applicationLinkService;

    @Autowired
    public JiraFieldMetadataCache(@ComponentImport final ApplicationLinkService applicationLinkService) {
        this.applicationLinkService = applicationLinkService;
    }

    private void init() {
        ApplicationLink applicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);
        ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        String jiraRestQuery = applicationLink.getRpcUrl() + FIELDS_REST_API_URL;
        try {
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, jiraRestQuery);
            String jiraResponseContent = request.execute();

            Gson gson = new Gson();
            List<JiraFieldMetadata> fields = gson.fromJson(jiraResponseContent, new TypeToken<List<JiraFieldMetadata>>() {
            }.getType());

            for (JiraFieldMetadata jiraFieldMetadata : fields) {
                fieldMetadata.put(jiraFieldMetadata.getId(), jiraFieldMetadata);
            }

        } catch (CredentialsRequiredException | ResponseException e) {
            e.printStackTrace();
        }
    }

    public synchronized String getFieldNameByCustomFieldId(final String customFieldId) {
        if (fieldMetadata.size() == 0 || !fieldMetadata.containsKey(customFieldId)) {
            init();
        }
        JiraFieldMetadata jiraFieldMetadata = fieldMetadata.get(customFieldId);
        return jiraFieldMetadata == null ? customFieldId : jiraFieldMetadata.getName();
    }
}
