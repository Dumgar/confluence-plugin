package com.epam.azn;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import com.atlassian.applinks.api.ApplicationLinkService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Scanned
public class IssueMacro implements Macro{

    private final ApplicationLinkService applicationLinkService;

    @Autowired
    public IssueMacro(@ComponentImport final ApplicationLinkService applicationLinkService) {
        this.applicationLinkService = applicationLinkService;
    }

    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {
        return null;
    }

    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }
}
