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
import com.epam.azn.model.JiraIssue;
import com.epam.azn.model.JqlResult;
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import org.joda.time.LocalDate;
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
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_RETURN_VALUE = "-";
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

    private static final String PARAMETER_DAYS = "days";
    private static final String NUMBER_FORMAT_EXCEPTION_MESSAGE = "ID and amount of Days must be a number";
    private static final String NULL_POINTER_EXCEPTION = "Page with this ID does not exist";

    private static final String NOFORMAT_BLOCK_START = "CDATA[";
    private static final String NOFORMAT_BLOCK_END = "]]";
    private static final String REST_ATTR_CHANGELOG = "&expand=changelog";
    private static final String SYSTEM_DATE_FORMAT = "yyyy-MM-dd";

    private static final String COLOR_RED = "red";
    private static final String COLOR_GREEN = "green";
    private static final String COLOR_AMBER = "amber";
    private static final String COLOR_AMBER_DIG = "FFBF00";
    private static final String COLOR_DEFAULT = "FFF0F5";

    private static final Character DATE_TIME_SEPARATOR = 'T';
    private static final String CREATED_FIELD_KEY = "created";

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
            date = LocalDate.now().minusDays(Integer.parseInt(map.get(PARAMETER_DAYS)));
            template = getTemplate(map);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return NUMBER_FORMAT_EXCEPTION_MESSAGE;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return NULL_POINTER_EXCEPTION;
        }

        String jql = map.get(JQL_KEY);
        if (jql == null || jql.length() < 1) {
            return EMPTY_STRING;
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
            return DEFAULT_RETURN_VALUE;
        }

        if (data instanceof String) {
            return (String) data;
        }

        if (data instanceof ArrayList) {
            ArrayList arrayList = (ArrayList) data;
            if (arrayList.isEmpty()) {
                return DEFAULT_RETURN_VALUE;
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
                sb.append(value).append(FIELD_VALUES_SEPARATOR);
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            return sb.toString();
        }

        if (data instanceof StringMap) {
            StringMap stringMap = (StringMap) data;
            if (stringMap.isEmpty()) {
                return DEFAULT_RETURN_VALUE;
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
        return template.substring(template.indexOf(NOFORMAT_BLOCK_START) + 6, template.indexOf(NOFORMAT_BLOCK_END));
    }

    private JqlResult getJqlResult(ApplicationLink applicationLink, ApplicationLinkRequestFactory requestFactory, String jql)
            throws UnsupportedEncodingException, CredentialsRequiredException, ResponseException {
        String jiraRestQuery = applicationLink.getRpcUrl()
                + ISSUES_BY_JQL_REST_API_URL + URLEncoder.encode(jql, StandardCharsets.UTF_8.name()) + REST_ATTR_CHANGELOG;
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
            replacement = replacement.replaceAll("%systemDate%", LocalDate.now().toString(SYSTEM_DATE_FORMAT));
            for (Map.Entry<String, Object> entry : issueFields.entrySet()) {
                String fieldKey = entry.getKey();
                Object fieldValue = entry.getValue();
                String valueFromJson = getValueFromJson(fieldValue);
                String color = valueFromJson.toLowerCase();

                if (color.equals(COLOR_RED) || color.equals(COLOR_GREEN) || color.equals(COLOR_AMBER)) {
                    if (color.equals(COLOR_AMBER)) {
                        color = COLOR_AMBER_DIG;
                    }
                    replacement = replacement.replaceAll("%" + fieldKey + "color%", color);
                    replacement = changeOldColors(fieldKey, replacement, issue, date, valueFromJson);
                }
                replacement = replacement.replaceAll("%" + fieldKey + "%", valueFromJson);
            }
            replacement = replacement.replaceAll("%.*color%", COLOR_DEFAULT);
            replacement = replacement.replaceAll("%.*colorprev%", COLOR_DEFAULT);
            replacement = replacement.replaceAll("%.*%", DEFAULT_RETURN_VALUE);
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
            LocalDate creationDate = LocalDate.parse(created.substring(0, created.indexOf(DATE_TIME_SEPARATOR)));

            for (HistoryItem historyItem : history.getItems()) {
                if (historyItem.getField().equals(jiraFieldMetadataCache.getFieldNameByCustomFieldId(fieldKey))) {
                    flagEmpty = true;
                    if (creationDate.isBefore(date) || creationDate.isEqual(date)) {
                        item = historyItem;
                    }
                }
            }
        }

        if (!flagEmpty) {
            String created = (String) issue.getFields().get(CREATED_FIELD_KEY);
            LocalDate issueCreated = LocalDate.parse(created.substring(0, created.indexOf(DATE_TIME_SEPARATOR)));
            String color = valueFromJson.toLowerCase();
            color = color.equals(COLOR_AMBER) ? COLOR_AMBER_DIG : color;
            color = color.equals(DEFAULT_RETURN_VALUE) ? COLOR_DEFAULT : color;
            if (date.isAfter(issueCreated) || date.isEqual(issueCreated)) {
                String replacement = template.replaceAll("%" + fieldKey + "prev%", valueFromJson);
                replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", color);
                return replacement;
            } else {
                String replacement = template.replaceAll("%" + fieldKey + "prev%", DEFAULT_RETURN_VALUE);
                replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", COLOR_DEFAULT);
                return replacement;
            }
        }

        if (item == null) {
            return template.replaceAll("%" + fieldKey + "prev%", DEFAULT_RETURN_VALUE);
        }

        String previousValue = item.getToString();
        previousValue = previousValue == null ? DEFAULT_RETURN_VALUE : previousValue;
        String color = previousValue.toLowerCase();
        color = color.equals(COLOR_AMBER) ? COLOR_AMBER_DIG : color;
        color = color.equals(DEFAULT_RETURN_VALUE) ? COLOR_DEFAULT : color;
        String replacement = template.replaceAll("%" + fieldKey + "prev%", previousValue);
        replacement = replacement.replaceAll("%" + fieldKey + "colorprev%", color);

        return replacement;
    }
}
