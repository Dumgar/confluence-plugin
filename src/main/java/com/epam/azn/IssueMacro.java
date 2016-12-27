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
import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
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
    public IssueMacro(@ComponentImport final ApplicationLinkService applicationLinkService, @ComponentImport final PageManager pageManager) {
        this.applicationLinkService = applicationLinkService;
        this.pageManager = pageManager;
    }

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {

        String template;

        try {
            template = getTemplate(map);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "ID must be a number";
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "Page with this ID does not exist";
        }

//        String template = "<DIV class=\"contentLayout2\">\n" +
//                "    <DIV class=\"columnLayout single\" data-layout=\"single\">\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <H1 id=\"TestReportingPage-Project:%project%\">Project:&nbsp;%project%</H1>\n" +
//                "                <P>Key:&nbsp;%Key%&nbsp;</P>\n" +
//                "                <P>Reporter:&nbsp;%reporter%</P>\n" +
//                "                <P>Summary: %summary%&nbsp;</P>\n" +
//                "                <P>&nbsp;</P></DIV>\n" +
//                "        </DIV>\n" +
//                "    </DIV>\n" +
//                "    <DIV class=\"columnLayout two-equal\" data-layout=\"two-equal\">\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\" colspan=\"2\">\n" +
//                "                                <H2 id=\"TestReportingPage-KEYSTAKEHOLDERS\"><STRONG>KEY\n" +
//                "                                    STAKEHOLDERS</STRONG></H2></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Checkbox</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%customfield_10000%&nbsp;</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Resolution</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P style=\"text-align: left;\">%resolution%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Last Viewed</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%lastViewed%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Watches</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%watches%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "                <P>&nbsp;</P>\n" +
//                "                <P>&nbsp;</P></DIV>\n" +
//                "        </DIV>\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <H2 id=\"TestReportingPage-ASSESSMENT\"><STRONG>ASSESSMENT</STRONG></H2></TH>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <H2 id=\"TestReportingPage-PREVIOUS\"><STRONG>PREVIOUS</STRONG></H2></TH>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <H2 id=\"TestReportingPage-CURRENT\"><STRONG>CURRENT</STRONG></H2></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Overall schedule</P></TD>\n" +
//                "                            <TD class=\"highlight-red confluenceTd\" data-highlight-colour=\"red\">\n" +
//                "                                <P>%colorFiled1prev%</P></TD>\n" +
//                "                            <TD class=\"highlight-green confluenceTd\" data-highlight-colour=\"green\">\n" +
//                "                                <P>%colorFiled1%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Budget</P></TD>\n" +
//                "                            <TD class=\"highlight-yellow confluenceTd\" data-highlight-colour=\"yellow\">\n" +
//                "                                <P>%colorFiled2prev%</P></TD>\n" +
//                "                            <TD class=\"highlight-yellow confluenceTd\" data-highlight-colour=\"yellow\">\n" +
//                "                                <P>%colorFiled2%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Resources</P></TD>\n" +
//                "                            <TD class=\"highlight-green confluenceTd\" data-highlight-colour=\"green\">\n" +
//                "                                <P>%colorFiled3prev%</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%colorFiled3%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Scope</P></TD>\n" +
//                "                            <TD class=\"highlight-red confluenceTd\" data-highlight-colour=\"red\">\n" +
//                "                                <P>%colorFiled4prev%</P></TD>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%colorFiled4%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "                <P>&nbsp;</P>\n" +
//                "                <P>&nbsp;</P></DIV>\n" +
//                "        </DIV>\n" +
//                "    </DIV>\n" +
//                "    <DIV class=\"columnLayout two-equal\" data-layout=\"two-equal\">\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <H2 id=\"TestReportingPage-ACTIVITIES\"><STRONG>ACTIVITIES</STRONG></H2>\n" +
//                "                                <P>&nbsp;</P></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\" colspan=\"1\">\n" +
//                "                                <P>Accomplishments since last update:</P>\n" +
//                "                                <P>%field5%</P>&nbsp;</TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "            </DIV>\n" +
//                "        </DIV>\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <H2 id=\"TestReportingPage-ISSUE/RISKMANAGEMENT\"><STRONG>&nbsp;ISSUE / RISK\n" +
//                "                                    MANAGEMENT</STRONG></H2></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%field6%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "            </DIV>\n" +
//                "        </DIV>\n" +
//                "    </DIV>\n" +
//                "    <DIV class=\"columnLayout two-equal\" data-layout=\"two-equal\">\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <P>&nbsp;DECISIONS / CLARIFICATIONS</P></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>Decisions since last update:&nbsp;&nbsp;<BR>\n" +
//                "                                    %field7%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "            </DIV>\n" +
//                "        </DIV>\n" +
//                "        <DIV class=\"cell normal\" data-type=\"normal\">\n" +
//                "            <DIV class=\"innerCell\">\n" +
//                "                <DIV class=\"table-wrap\">\n" +
//                "                    <TABLE class=\"confluenceTable\">\n" +
//                "                        <TBODY>\n" +
//                "                        <TR>\n" +
//                "                            <TH class=\"confluenceTh\">\n" +
//                "                                <P>UPCOMING MILESTONES</P></TH>\n" +
//                "                        </TR>\n" +
//                "                        <TR>\n" +
//                "                            <TD class=\"confluenceTd\">\n" +
//                "                                <P>%field8%</P></TD>\n" +
//                "                        </TR>\n" +
//                "                        </TBODY>\n" +
//                "                    </TABLE>\n" +
//                "                </DIV>\n" +
//                "            </DIV>\n" +
//                "        </DIV>\n" +
//                "    </DIV>\n" +
//                "</DIV>";


        String jql = map.get(JQL_KEY);
        if (jql == null || jql.length() < 1) {
            return "";
        }

        ApplicationLink applicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);

        if (applicationLink == null) {
            return APPLICATION_LINK_MSG;
        }

        try {

            JqlResult jqlResult = getJqlResult(applicationLink, jql);

            return getCompleteString(jqlResult, template);

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

    private JqlResult getJqlResult(ApplicationLink applicationLink, String jql) throws UnsupportedEncodingException, CredentialsRequiredException, ResponseException {
        ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        String jiraRestQuery = applicationLink.getRpcUrl()
                + ISSUES_BY_JQL_REST_API_URL + URLEncoder.encode(jql, StandardCharsets.UTF_8.name());
        ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, jiraRestQuery);
        String jiraResponseContent = request.execute();
        Gson gson = new Gson();
        return gson.fromJson(jiraResponseContent, JqlResult.class);
    }

    private String getCompleteString(JqlResult jqlResult, String template) {
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
            for (Map.Entry<String, Object> entry : issueFields.entrySet()) {
                String fieldKey = entry.getKey();
                Object fieldValue = entry.getValue();
                String valueFromJson = getValueFromJson(fieldValue);
                String color = valueFromJson.toLowerCase();
                if (color.equals("red") || color.equals("green") || color.equals("amber")){
                    if (color.equals("amber")){
                        color = "#FFBF00";
                    }
                    replacement = replacement.replaceAll("%" + fieldKey + "color%", color);
                }
                replacement = replacement.replaceAll("%" + fieldKey + "%", valueFromJson);
            }
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
}
