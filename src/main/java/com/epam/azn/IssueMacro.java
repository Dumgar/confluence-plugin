package com.epam.azn;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Scanned
public class IssueMacro implements Macro {

    private final ApplicationLinkService applicationLinkService;
    private final PageManager pageManager;

    private static final String JIRA_AUTH_MSG_START = "In order to proceed <a href=\"";
    private static final String JIRA_AUTH_MSG_END = "\">click here to authorize in JIRA.</a>";

    private final static String ISSUES_BY_JQL_REST_API_URL = "/rest/api/2/search?jql=";

    private static final String JQL_KEY = "jqlString";
    private static final String PAGE_ID = "pageID";
    private static final String APPLICATION_LINK_MSG = "In order to proceed configure Application Link to JIRA or contact your administrator to do it.";

    @Autowired
    public IssueMacro(@ComponentImport final ApplicationLinkService applicationLinkService,@ComponentImport final PageManager pageManager) {
        this.applicationLinkService = applicationLinkService;
        this.pageManager = pageManager;
    }

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {

        List<String> listOfKeys = new ArrayList<>();

        long pageID;
        String templateString;
        try {
            pageID = Long.parseLong(map.get(PAGE_ID));
            templateString = pageManager.getPage(pageID).getBodyAsString();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "ID must be a number";
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "Page with this ID does not exist";
        }


        String jql = map.get(JQL_KEY);
        if (jql == null || jql.length() < 1) {
            return "";
        }
        ApplicationLink applicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);
        if (applicationLink == null) {
            return APPLICATION_LINK_MSG;
        }
        ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();

        String jiraRestQuery = null;
        try {
            jiraRestQuery = applicationLink.getRpcUrl()
                    + ISSUES_BY_JQL_REST_API_URL + URLEncoder.encode(jql, StandardCharsets.UTF_8.name());
            ApplicationLinkRequest request = null;

            request = requestFactory.createRequest(Request.MethodType.GET, jiraRestQuery);
            String jiraResponseContent = null;

            jiraResponseContent = request.execute();

            Gson gson = new Gson();
            JqlResult jqlResult = gson.fromJson(jiraResponseContent, JqlResult.class);
            for (JiraIssue s1 : jqlResult.getIssues()) {
                listOfKeys.add(s1.getKey());
                System.out.println(s1);
            }
            return "";
        } catch (UnsupportedEncodingException | ResponseException e) {
            e.printStackTrace();
        } catch (CredentialsRequiredException e) {
            e.printStackTrace();
            return JIRA_AUTH_MSG_START + e.getAuthorisationURI() + JIRA_AUTH_MSG_END;
        }
        return templateString;
    }

    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }
}
