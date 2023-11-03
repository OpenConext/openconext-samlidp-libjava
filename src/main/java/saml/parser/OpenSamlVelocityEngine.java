package saml.parser;


import net.shibboleth.utilities.java.support.velocity.VelocityEngine;
import org.apache.velocity.VelocityContext;
import org.slf4j.helpers.NOPLogger;

import java.io.Writer;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OpenSamlVelocityEngine {

    public void process(String templateId,
                        Map<String, Object> model,
                        Writer out) {
        org.apache.velocity.app.VelocityEngine velocityEngine = VelocityEngine.newVelocityEngine();
        velocityEngine.setProperty("runtime.log.instance", NOPLogger.NOP_LOGGER);
        velocityEngine.init();
        VelocityContext context = new VelocityContext();
        model.forEach(context::put);
        velocityEngine.mergeTemplate(templateId, UTF_8.name(), context, out);
    }

}