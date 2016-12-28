package com.epam.azn;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.epam.azn.model.History;
import com.epam.azn.model.HistoryItem;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Scanned
public class IssueMacro implements Macro {

    private static final String[] ORDERED_MEANINGFUL_FIELD_KEYS = new String[]{"displayName", "name", "value", "votes", "watchCount"};

    private static final String SELF_KEY = "self";
    private static final String FIELD_VALUES_ASSIGNMENT = ": ";
    private static final String FIELD_VALUES_SEPARATOR = ", ";
    private final ApplicationLinkService applicationLinkService;
    private final PageManager pageManager;
    private final JiraFieldMetadataCache jiraFieldMetadataCache;

    private static final String JIRA_AUTH_MSG_START = "In order to proceed <a href=\"";
    private static final String JIRA_AUTH_MSG_END = "\">click here to authorize in JIRA.</a>";
    private static final String INCORRECT_JQL = "Incorrect JQL statement";
    private static final String WRONG_CHARACTER_ENCODING = "The Character Encoding is not supported.";
    private static final String WRONG_PAGE_ID = "PageID is incorrect or there is no template.";

    private final static String ISSUES_BY_JQL_REST_API_URL = "/rest/api/2/search?jql=";

    private static final String JQL_KEY = "jqlString";
    private static final String PAGE_ID = "pageID";
    private static final String APPLICATION_LINK_MSG = "In order to proceed configure Application Link to JIRA or contact your administrator to do it.";

    @Autowired
    public IssueMacro(@ComponentImport final ApplicationLinkService applicationLinkService, @ComponentImport final PageManager pageManager, final JiraFieldMetadataCache jiraFieldMetadataCache) {
        this.applicationLinkService = applicationLinkService;
        this.pageManager = pageManager;
        this.jiraFieldMetadataCache = jiraFieldMetadataCache;
    }

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {

        LocalDate date;

        String template;

        try {
            date = LocalDate.now().minusDays(Integer.parseInt(map.get("days")));
            template = getTemplate(map);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "ID and amount of Days must be a number";
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

        try {

            JqlResult jqlResult = getJqlResult(applicationLink, requestFactory, jql);

            return getCompleteString(jqlResult, template, date);

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return WRONG_PAGE_ID;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return WRONG_CHARACTER_ENCODING;
        } catch (ResponseException e) {
            e.printStackTrace();
            return INCORRECT_JQL;
        } catch (CredentialsRequiredException e) {
            e.printStackTrace();
            return JIRA_AUTH_MSG_START + e.getAuthorisationURI() + JIRA_AUTH_MSG_END;
        }
    }

    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    private String getValueFromJson(Object data) {
        if (data == null) {
            return "-";
        }

        if (data instanceof String) {
            return (String) data;
        }

        if (data instanceof ArrayList) {
            ArrayList arrayList = (ArrayList) data;
            if (arrayList.isEmpty()) {
                return "-";
            }

            StringBuilder sb = new StringBuilder();
            List<String> valuesList = new LinkedList<>();
            for (Object o : arrayList) {
                if (o instanceof StringMap) {
                    StringMap stringMap = (StringMap) o;
                    if (!stringMap.isEmpty()) {
                        valuesList.add(getMeaningfulData(stringMap));
                    }
                } else if (o != null && o.toString().length() > 0) {
                    valuesList.add(o.toString());
                }
            }

            for (String value : valuesList) {
                sb.append(value).append(", ");
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            return sb.toString();
        }

        if (data instanceof StringMap) {
            StringMap stringMap = (StringMap) data;
            if (stringMap.isEmpty()) {
                return "-";
            }
            return getMeaningfulData(stringMap);
        }
        return data.toString();
    }

    private String getMeaningfulData(StringMap stringMap) {
        for (String key : ORDERED_MEANINGFUL_FIELD_KEYS) {
            Object value = stringMap.get(key);
            if (value != null) {
                return value.toString();
            }
        }

        StringBuilder sb = new StringBuilder();

        for (Object key : stringMap.keySet()) {
            if (!SELF_KEY.equalsIgnoreCase(key.toString())) {
                sb.append(key).append(FIELD_VALUES_ASSIGNMENT).append(stringMap.get(key)).append(FIELD_VALUES_SEPARATOR);
            }
        }
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }

    private String getTemplate(Map<String, String> map) throws NumberFormatException, NullPointerException {
        long pageID = Long.parseLong(map.get(PAGE_ID));
        String template = pageManager.getPage(pageID).getBodyAsString();
        return template.substring(template.indexOf("CDATA[") + 6, template.indexOf("]]"));
    }

    private JqlResult getJqlResult(ApplicationLink applicationLink, ApplicationLinkRequestFactory requestFactory, String jql) throws UnsupportedEncodingException, CredentialsRequiredException, ResponseException {
        String jiraRestQuery = applicationLink.getRpcUrl()
                + ISSUES_BY_JQL_REST_API_URL + URLEncoder.encode(jql, StandardCharsets.UTF_8.name()) + "&expand=changelog";
        ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, jiraRestQuery);
        String jiraResponseContent = request.execute();
        Gson gson = new Gson();
        return gson.fromJson(jiraResponseContent, JqlResult.class);
    }

    private String getCompleteString(JqlResult jqlResult, String template, LocalDate date) {
        StringBuilder selectFormBuilder = new StringBuilder();
        StringBuilder divIssueBuilder = new StringBuilder();
        selectFormBuilder.append("<p><select id=\"hero\" onchange=\"show(oldValue); oldValue = this.value\">\n" +
                "    <option disabled>Choose Issue</option>\n" +
                "    <option value=\"\"></option>\n");

        for (JiraIssue issue : jqlResult.getIssues()) {
            String key = issue.getKey();
            HashMap<String, Object> issueFields = issue.getFields();

            selectFormBuilder.append("<option value=\"")
                    .append(key)
                    .append("\">")
                    .append(key)
                    .append("</option>\n");
            divIssueBuilder.append("<div id=\"")
                    .append(key)
                    .append("\" style=\"display: none\">");
            String replacement = template;
            replacement = replacement.replaceAll("%Key%", issue.getKey());
            replacement = replacement.replaceAll("%systemDate%", LocalDate.now().toString("yyyy-MM-dd"));
            for (Map.Entry<String, Object> entry : issueFields.entrySet()) {
                String fieldKey = entry.getKey();
                Object fieldValue = entry.getValue();
                String valueFromJson = getValueFromJson(fieldValue);
                String color = valueFromJson.toLowerCase();

                if (color.equals("red") || color.equals("green") || color.equals("amber")) {
                    if (color.equals("amber")) {
                        color = "#FFBF00";
                    }
                    replacement = replacement.replaceAll("%" + fieldKey + "color%", color);
                    replacement = changeOldColors(fieldKey, replacement, issue, date, valueFromJson);
                }
                replacement = replacement.replaceAll("%" + fieldKey + "%", valueFromJson);
            }
            replacement = replacement.replaceAll("%.*color%", "#FFF0F5");
            replacement = replacement.replaceAll("%.*colorprev%", "#FFF0F5");
            replacement = replacement.replaceAll("%.*%", "-");
            divIssueBuilder.append(replacement)
                    .append("</div>\n");
        }

        selectFormBuilder.append("</select></p>\n<br/><div id=\"\" style=\"display: none\"></div>\n")
                .append(divIssueBuilder)
                .append("<script>\n" +
                        "    var oldValue;\n" +
                        "    function show(previous) {\n" +
                        "        if (previous) {\n" +
                        "            document.getElementById(previous).style.display = 'none';\n" +
                        "        }\n" +
                        "        var x = document.getElementById(\"hero\").value;\n" +
                        "        document.getElementById(x).style.display = 'block';\n" +
                        "    }\n" +
                        "</script>");

        return selectFormBuilder.toString();
    }

    private String changeOldColors(String fieldKey, String template, JiraIssue issue, LocalDate date, String valueFromJson) {
        boolean flagEmpty = false;
        HistoryItem item = null;
        List<History> histories = issue.getChangelog().getHistories();
        for (History history : histories) {
            String created = history.getCreated();
            LocalDate creationDate = LocalDate.parse(created.substring(0, created.indexOf('T')));

            for (HistoryItem historyItem : history.getItems()) {
                if (historyItem.getField().equals(jiraFieldMetadataCache.getFieldNameByCustomFieldId(fieldKey))) {
                    flagEmpty = true;
                    if (creationDate.isBefore(date) || creationDate.isEqual(date)) {
                        item = historyItem;
                    }
                }
            }
        }

        if (!flagEmpty){
            String created = (String) issue.getFields().get("created");
            LocalDate issueCreated = LocalDate.parse(created.substring(0, created.indexOf('T')));
            String color = valueFromJson.toLowerCase();
            color = color.equals("amber") ? "#FFBF00" : color;
            color = color.equals("-") ? "#FFF0F5" : color;
            if (date.isAfter(issueCreated) || date.isEqual(issueCreated)){
                String replacement = template.replaceAll("%" + fieldKey + "prev%", valueFromJson);
                replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", color);
                return replacement;
            } else {
                String replacement = template.replaceAll("%" + fieldKey + "prev%", "-");
                replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", "#FFF0F5");
                return replacement;
            }
        }

        if (item == null) {
            return template.replaceAll("%" + fieldKey + "prev%", "-");
        }

        String previousValue = item.getToString();
        previousValue = previousValue == null ? "-" : previousValue;
        String color = previousValue.toLowerCase();
        color = color.equals("amber") ? "#FFBF00" : color;
        color = color.equals("-") ? "#FFF0F5" : color;
        String replacement = template.replaceAll("%" + fieldKey + "prev%", previousValue);
        replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", color);

        return replacement;
    }
}
