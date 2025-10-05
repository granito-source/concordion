package io.granito.concordion.ext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import io.granito.concordion.test.TestOutputStream;
import org.concordion.api.CommandCall;
import org.concordion.api.Element;
import org.concordion.api.Evaluator;
import org.concordion.api.Fixture;
import org.concordion.api.Resource;
import org.concordion.api.ResultRecorder;
import org.concordion.api.Target;
import org.concordion.api.extension.ConcordionExtender;
import org.concordion.api.listener.ConcordionBuildEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WriteExtensionTest {
    private static final String HREF = "file.out";

    private static final String EXPR = "#var";

    private final WriteExtension extension = new WriteExtension();

    private final TestOutputStream out = new TestOutputStream();

    private final Resource spec = new Resource("/target/test/spec.html");

    @Mock
    private ConcordionExtender extender;

    @Mock
    private Evaluator evaluator;

    @Mock
    private ResultRecorder recorder;

    @Mock
    private Fixture fixture;

    @Mock
    private Target target;

    @BeforeEach
    void setUp()
    {
        doReturn(extender).when(extender).withBuildListener(any());
        doReturn(extender).when(extender).withCommand(any(), any(), any());
    }

    @Test
    void registersItselfWhenAddedToExtender()
    {
        extension.addTo(extender);

        verify(extender).withBuildListener(extension);
        verify(extender).withCommand("urn:concordion-extensions:2010",
            "write", extension);
    }

    @Nested
    class WhenRegistered {
        @BeforeEach
        void setUp() throws Exception
        {
            extension.addTo(extender);
            extension.concordionBuilt(new ConcordionBuildEvent(target));

            doReturn(out).when(target).getOutputStream(any());
        }

        @Test
        void writesFileWhenExecuteWithByteExpression() throws Exception
        {
            var content = "content".getBytes(StandardCharsets.UTF_8);

            doReturn(content).when(evaluator).evaluate(EXPR);

            extension.execute(call(HREF, EXPR), evaluator, recorder,
                fixture);

            verify(target)
                .getOutputStream(new Resource("/target/test/file.out"));

            assertThat(out.toByteArray()).isEqualTo(content);
            assertThat(out.isClosed()).isTrue();
        }

        @Test
        void writesFileWhenExecuteWithStringExpression() throws Exception
        {
            var call = call(HREF, EXPR);
            var content = "content";

            doReturn(content).when(evaluator).evaluate(EXPR);

            extension.execute(call, evaluator, recorder, fixture);

            verify(target)
                .getOutputStream(new Resource("/target/test/file.out"));

            assertThat(out).hasToString(content);
            assertThat(out.isClosed()).isTrue();
        }

        @Test
        void writesFileWhenExecuteWithObjectExpression() throws Exception
        {
            var call = call(HREF, EXPR);
            var content = Integer.valueOf(42);

            doReturn(content).when(evaluator).evaluate(EXPR);

            extension.execute(call, evaluator, recorder, fixture);

            verify(target)
                .getOutputStream(new Resource("/target/test/file.out"));

            assertThat(out).hasToString("42");
            assertThat(out.isClosed()).isTrue();
        }

        @Test
        void throwsExceptionWhenNoHref()
        {
            var call = call(null, EXPR);

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("href");
        }

        @Test
        void throwsExceptionWhenEmptyHref()
        {
            var call = call("", EXPR);

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("href");
        }

        @Test
        void throwsExceptionWhenUrlHref()
        {
            var call = call("http://localhost/test.out", EXPR);

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
        }

        @Test
        void throwsExceptionWhenAbsolutePathHref()
        {
            var call = call("/test.out", EXPR);

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Relative");
        }

        @Test
        void throwsExceptionWhenNoExpression()
        {
            var call = call(HREF, null);

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expression");
        }

        @Test
        void throwsExceptionWhenEmptyExpression()
        {
            var call = call(HREF, "");

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expression");
        }

        @Test
        void wrapsExceptionWhenGettingOutputStreamFails() throws Exception
        {
            var call = call(HREF, EXPR);
            var t = new IOException("get");

            doReturn("content").when(evaluator).evaluate(any());
            doThrow(t).when(target).getOutputStream(any());

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(UncheckedIOException.class)
                .cause().isSameAs(t);
        }

        @Test
        void wrapsExceptionWhenWritingFails()
        {
            var call = call(HREF, EXPR);
            var t = new IOException("write");

            out.setWriteException(t);

            doReturn("content").when(evaluator).evaluate(any());

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(UncheckedIOException.class)
                .cause().isSameAs(t);

            assertThat(out.isClosed()).isTrue();
        }

        @Test
        void wrapsExceptionWhenClosingOutputStreamFails()
        {
            var call = call(HREF, EXPR);
            var t = new IOException("close");

            out.setCloseException(t);

            doReturn("content").when(evaluator).evaluate(any());

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                    recorder, fixture))
                .isInstanceOf(UncheckedIOException.class)
                .cause().isSameAs(t);
        }

        @Test
        void wrapsFirstExceptionWhenBothWritingAndClosingFail()
        {
            var call = call(HREF, EXPR);
            var t1 = new IOException("write");
            var t2 = new IOException("close");

            out.setWriteException(t1);
            out.setCloseException(t2);

            doReturn("content").when(evaluator).evaluate(any());

            assertThatThrownBy(() -> extension.execute(call, evaluator,
                recorder, fixture))
                .isInstanceOf(UncheckedIOException.class)
                .cause().isSameAs(t1);

            assertThat(t1).hasSuppressedException(t2);
        }

        private CommandCall call(String href, String expr)
        {
            var element = new Element("a");

            if (href != null)
                element.addAttribute("href", href);

            return new CommandCall(null, extension, element, expr, spec);
        }
    }
}
