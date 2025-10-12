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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.Base64;

import io.granito.concordion.test.TestOutputStream;
import org.concordion.api.Command;
import org.concordion.api.CommandCall;
import org.concordion.api.Element;
import org.concordion.api.Evaluator;
import org.concordion.api.Fixture;
import org.concordion.api.Resource;
import org.concordion.api.ResultRecorder;
import org.concordion.api.Target;
import org.concordion.api.extension.ConcordionExtender;
import org.concordion.api.listener.AssertEqualsListener;
import org.concordion.api.listener.AssertFailureEvent;
import org.concordion.api.listener.AssertFalseListener;
import org.concordion.api.listener.AssertSuccessEvent;
import org.concordion.api.listener.AssertTrueListener;
import org.concordion.api.listener.ConcordionBuildEvent;
import org.concordion.api.listener.ConcordionBuildListener;
import org.concordion.api.listener.SpecificationProcessingEvent;
import org.concordion.api.listener.SpecificationProcessingListener;
import org.concordion.api.listener.ThrowableCaughtEvent;
import org.concordion.api.listener.ThrowableCaughtListener;
import org.concordion.ext.screenshot.ScreenshotEmbedder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

@MockitoSettings(strictness = Strictness.LENIENT)
class SeleniumScreenshotExtensionTest {
    private static final String IMAGE = "spec_img1.png";

    private final SeleniumScreenshotExtension extension =
        new SeleniumScreenshotExtension();

    private final Resource spec = new Resource("/target/test/spec.html");

    private final Resource image = new Resource("/target/test/" + IMAGE);

    private final TestOutputStream out = new TestOutputStream();

    @Mock
    private WebDriver genericWebDriver;

    @Mock
    private ScreenshotWebDriver screenshotWebDriver;

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

    @Captor
    private ArgumentCaptor<ConcordionBuildListener> buildListener;

    @Captor
    private ArgumentCaptor<SpecificationProcessingListener> processingListener;

    @Captor
    private ArgumentCaptor<Command> command;

    @Captor
    private ArgumentCaptor<AssertEqualsListener> equalsListener;

    @Captor
    private ArgumentCaptor<AssertTrueListener> trueListener;

    @Captor
    private ArgumentCaptor<AssertFalseListener> falseListener;

    @Captor
    private ArgumentCaptor<ThrowableCaughtListener> throwableListener;

    private ConcordionBuildEvent buildEvent;

    private SpecificationProcessingEvent processingEvent;

    @BeforeEach
    void setUp()
    {
        buildEvent = new ConcordionBuildEvent(target);
        processingEvent = new SpecificationProcessingEvent(spec,
            new Element("html"));

        doReturn(extender).when(extender)
            .withBuildListener(buildListener.capture());
        doReturn(extender).when(extender)
            .withSpecificationProcessingListener(processingListener.capture());
        doReturn(extender).when(extender).withCommand(
            eq("urn:concordion-extensions:2010"), eq("screenshot"),
            command.capture());
        doReturn(extender).when(extender)
            .withAssertEqualsListener(equalsListener.capture());
        doReturn(extender).when(extender)
            .withAssertTrueListener(trueListener.capture());
        doReturn(extender).when(extender)
            .withAssertFalseListener(falseListener.capture());
        doReturn(extender).when(extender)
            .withThrowableListener(throwableListener.capture());
    }

    @Test
    void registersItselfWhenAddedToExtender()
    {
        extension.addTo(extender);

        verify(extender).withSpecificationProcessingListener(
            any(ScreenshotEmbedder.class));
        verify(extender).withBuildListener(any(ScreenshotEmbedder.class));
        verify(extender).withCommand(eq("urn:concordion-extensions:2010"),
            eq("screenshot"), any(ScreenshotEmbedder.class));
        verify(extender)
            .withAssertEqualsListener(any(ScreenshotEmbedder.class));
        verify(extender)
            .withAssertTrueListener(any(ScreenshotEmbedder.class));
        verify(extender)
            .withAssertFalseListener(any(ScreenshotEmbedder.class));
        verify(extender)
            .withThrowableListener(any(ScreenshotEmbedder.class));
    }

    @Nested
    class WhenRegistered {
        @BeforeEach
        void setUp() throws Exception
        {
            extension.addTo(extender);
            buildListener.getValue().concordionBuilt(buildEvent);
            processingListener.getValue()
                .beforeProcessingSpecification(processingEvent);
            doReturn(out).when(target).getOutputStream(any());
        }

        @Test
        void doesNotModifyElementWhenWebDriverIsNotSet() throws Exception
        {
            var call = call();

            command.getValue().execute(call, evaluator, recorder, fixture);

            verify(target).getOutputStream(image);

            assertThat(out.toByteArray()).isEmpty();
            assertThat(out.isClosed()).isTrue();
            assertThat(call.getElement().hasChildren()).isFalse();
        }

        @Test
        void doesNotModifyElementWhenScreenshotsAreNotSupported()
            throws Exception
        {
            extension.setWebDriver(genericWebDriver);

            var call = call();

            command.getValue().execute(call, evaluator, recorder, fixture);

            verifyNoInteractions(genericWebDriver);
            verify(target).getOutputStream(image);

            assertThat(out.toByteArray()).isEmpty();
            assertThat(out.isClosed()).isTrue();
            assertThat(call.getElement().hasChildren()).isFalse();
        }

        @Nested
        class WhenUsingScreenshotCapableWebDriver {
            @BeforeEach
            void setUp()
            {
                extension.setWebDriver(screenshotWebDriver);
            }

            @Test
            void writesFileAndAddsImageToElement() throws Exception
            {
                var call = call();
                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                command.getValue()
                    .execute(call, evaluator, recorder, fixture);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(call.getElement());
            }

            @Test
            void wrapsExceptionWhenGettingOutputStreamFails()
                throws Exception
            {
                var cmd = command.getValue();
                var call = call();
                var t = new IOException("get");

                doThrow(t).when(target).getOutputStream(any());

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isInstanceOf(RuntimeException.class)
                    .cause().isSameAs(t);

                verifyNoInteractions(screenshotWebDriver);
            }

            @Test
            void propagatesExceptionWhenTakingScreenshotFails()
                throws Exception
            {
                var cmd = command.getValue();
                var call = call();
                var t = new WebDriverException("screenshot");

                doThrow(t).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isSameAs(t);

                verify(target).getOutputStream(image);

                assertThat(out.isClosed()).isTrue();
            }

            @Test
            void throwsExceptionWhenScreenshotIsNotValidPng()
            {
                var cmd = command.getValue();
                var call = call();
                var content = "this is not a png".getBytes();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isInstanceOf(RuntimeException.class)
                    .rootCause()
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("unable to read .png");

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
            }

            @Test
            void wrapsExceptionWhenWritingFileFails() throws Exception
            {
                var cmd = command.getValue();
                var call = call();
                var t = new IOException("write");

                out.setWriteException(t);

                doReturn(data()).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isInstanceOf(RuntimeException.class)
                    .cause().isSameAs(t);

                verify(target).getOutputStream(image);

                assertThat(out.isClosed()).isTrue();
            }

            @Test
            void wrapsExceptionWhenClosingStreamFails()
            {
                var cmd = command.getValue();
                var call = call();
                var t = new IOException("close");

                out.setCloseException(t);

                doReturn(data()).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isInstanceOf(RuntimeException.class)
                    .cause().isSameAs(t);
            }

            @Test
            void wrapsFirstExceptionWhenBothWritingAndClosingFail()
            {
                var cmd = command.getValue();
                var call = call();
                var t1 = new IOException("write");
                var t2 = new IOException("close");

                out.setWriteException(t1);
                out.setCloseException(t2);

                doReturn(data()).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                assertThatThrownBy(() -> cmd.execute(call, evaluator,
                        recorder, fixture))
                    .isInstanceOf(RuntimeException.class)
                    .cause().isSameAs(t1);

                assertThat(t1).hasSuppressedException(t2);
            }

            @Test
            void takesScreenshotOnEqualsFailure() throws Exception
            {
                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertFailure();

                equalsListener.getValue().failureReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void doesNothingOnEqualsFailureWhenDisabled() throws Exception
            {
                extension.setScreenshotOnAssertionFailure(false);

                var event = assertFailure();

                equalsListener.getValue().failureReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void doesNothingOnEqualsSuccess() throws Exception
            {
                var event = assertSuccess();

                equalsListener.getValue().successReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void takesScreenshotOnEqualsSuccessWhenEnabled()
                throws Exception
            {
                extension.setScreenshotOnAssertionSuccess(true);

                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertSuccess();

                equalsListener.getValue().successReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void takesScreenshotOnTrueFailure() throws Exception
            {
                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertFailure();

                trueListener.getValue().failureReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void doesNothingOnTrueFailureWhenDisabled() throws Exception
            {
                extension.setScreenshotOnAssertionFailure(false);

                var event = assertFailure();

                trueListener.getValue().failureReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void doesNothingOnTruesSuccess() throws Exception
            {
                var event = assertSuccess();

                trueListener.getValue().successReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void takesScreenshotOnTrueSuccessWhenEnabled()
                throws Exception
            {
                extension.setScreenshotOnAssertionSuccess(true);

                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertSuccess();

                trueListener.getValue().successReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void takesScreenshotOnFalseFailure() throws Exception
            {
                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertFailure();

                falseListener.getValue().failureReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void doesNothingOnFalseFailureWhenDisabled() throws Exception
            {
                extension.setScreenshotOnAssertionFailure(false);

                var event = assertFailure();

                falseListener.getValue().failureReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void doesNothingOnFalseSuccess() throws Exception
            {
                var event = assertSuccess();

                falseListener.getValue().successReported(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            @Test
            void takesScreenshotOnFalseSuccessWhenEnabled()
                throws Exception
            {
                extension.setScreenshotOnAssertionSuccess(true);

                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = assertSuccess();

                falseListener.getValue().successReported(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void takesScreenshotOnThrowableCaught() throws Exception
            {
                var content = data();

                doReturn(content).when(screenshotWebDriver)
                    .getScreenshotAs(OutputType.BYTES);

                var event = throwableCaught();

                throwableListener.getValue().throwableCaught(event);

                verify(target).getOutputStream(image);

                assertThat(out.toByteArray()).isEqualTo(content);
                assertThat(out.isClosed()).isTrue();
                verifyScreenshotIsAdded(event.getElement());
            }

            @Test
            void doesNothingOnThrowableCaughtWhenDisabled()
                throws Exception
            {
                extension.setScreenshotOnThrowable(false);

                var event = throwableCaught();

                throwableListener.getValue().throwableCaught(event);

                verify(target, never()).getOutputStream(any());
                verifyNoInteractions(screenshotWebDriver);

                assertThat(event.getElement().hasChildren()).isFalse();
            }

            private byte[] data()
            {
                return Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAA" +
                    "DUlEQVR4AWJiYGD4DwAAAP//cGajQwAAAAZJREFUAwABDgEC" +
                    "81VxbAAAAABJRU5ErkJggg==");
            }

            private AssertFailureEvent assertFailure()
            {
                return new AssertFailureEvent(new Element("p"), "true",
                    false);
            }

            private AssertSuccessEvent assertSuccess()
            {
                return new AssertSuccessEvent(new Element("p"));
            }

            private ThrowableCaughtEvent throwableCaught()
            {
                return new ThrowableCaughtEvent(
                    new RuntimeException("test"), new Element("p"),
                    "call()");
            }

            private void verifyScreenshotIsAdded(Element element)
            {
                var children = element.getChildElements();

                assertThat(children).hasSize(1);

                var anchor = children[0];

                assertThat(anchor.getLocalName()).isEqualTo("a");

                var anchorChildren = anchor.getChildElements();

                assertThat(anchorChildren).hasSize(1);

                var img = anchorChildren[0];

                assertThat(img.getLocalName()).isEqualTo("img");
                assertThat(img.getAttributeValue("src")).isEqualTo(IMAGE);
                assertThat(img.getAttributeValue("width")).isEqualTo("1");
            }
        }

        private CommandCall call()
        {
            return new CommandCall(null, command.getValue(),
                new Element("p"), "", spec);
        }
    }

    private interface ScreenshotWebDriver extends WebDriver, TakesScreenshot {
    }
}
