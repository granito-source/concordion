package io.granito.concordion.ext;

import static java.lang.String.format;
import static org.concordion.internal.util.Check.notEmpty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.concordion.api.AbstractCommand;
import org.concordion.api.CommandCall;
import org.concordion.api.Evaluator;
import org.concordion.api.Fixture;
import org.concordion.api.Resource;
import org.concordion.api.ResultRecorder;
import org.concordion.api.Target;
import org.concordion.api.extension.ConcordionExtender;
import org.concordion.api.extension.ConcordionExtension;
import org.concordion.api.listener.ConcordionBuildEvent;
import org.concordion.api.listener.ConcordionBuildListener;

public class WriteExtension extends AbstractCommand
    implements ConcordionExtension, ConcordionBuildListener {
    private static final String NAMESPACE = "urn:concordion-extensions:2010";

    private static final String COMMAND = "write";

    private static final String HREF = "href";

    private Target target;

    @Override
    public void addTo(ConcordionExtender extender)
    {
        extender.withBuildListener(this);
        extender.withCommand(NAMESPACE, COMMAND, this);
    }

    @Override
    public void concordionBuilt(ConcordionBuildEvent event)
    {
        target = event.getTarget();
    }

    @Override
    public void execute(CommandCall call, Evaluator evaluator,
        ResultRecorder recorder, Fixture fixture)
    {
        var element = call.getElement();
        var href = element.getAttributeValue(HREF);

        notEmpty(href, "'%s' attribute must be defined", HREF);

        if (URI.create(href).isAbsolute())
            throw new IllegalArgumentException(format(
                "'%s' may not be an absolute URL, was: %s", HREF, href));

        var file = call.getResource().getRelativeResource(href);
        var expr = call.getExpression();

        notEmpty(expr, "'%s' command must define an expression", COMMAND);

        write(file, toBytes(evaluator.evaluate(expr)));
    }

    private byte[] toBytes(Object value)
    {
        return value instanceof byte[] bytes ? bytes :
            value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void write(Resource file, byte[] bytes)
    {
        try (var out = target.getOutputStream(file)) {
            out.write(bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
