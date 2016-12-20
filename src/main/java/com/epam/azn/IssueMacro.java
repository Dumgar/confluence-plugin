package com.epam.azn;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import java.util.Map;

@Scanned
public class IssueMacro implements Macro{



    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {
        return null;
    }

    public BodyType getBodyType() {
        return null;
    }

    public OutputType getOutputType() {
        return null;
    }
}
