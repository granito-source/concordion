/*
 * Copyright 2025 Alexei Yashkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * A Concordion extension that writes the result of an expression
 * evaluation to a file and links the file to the specification.
 */
public class WriteExtension extends AbstractCommand
    implements ConcordionExtension, ConcordionBuildListener {
    private static final String NAMESPACE = "urn:concordion-extensions:2010";

    private static final String COMMAND = "write";

    private static final String HREF = "href";

    private Target target;

    /**
     * Registers the extension to the {@link ConcordionExtender}.
     *
     * @param extender the Concordion extender for the registration
     */
    @Override
    public void addTo(ConcordionExtender extender)
    {
        extender.withBuildListener(this);
        extender.withCommand(NAMESPACE, COMMAND, this);
    }

    /**
     * Captures the Concordion target from the {@link ConcordionBuildEvent}.
     *
     * @param event the Concordion build event
     */
    @Override
    public void concordionBuilt(ConcordionBuildEvent event)
    {
        target = event.getTarget();
    }

    /**
     * Executes the {@code write} command that writes the evaluated
     * expression to a file and links the file as {@code href} attribute
     * of the element (normally {@code <a>}).
     *
     * @param call the command call
     * @param evaluator the evaluator for expression evaluation
     * @param recorder the result recorder
     * @param fixture the fixture
     */
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
