# Screenshot Extension for Concordion

This [Concordion](https://www.concordion.org/) extension provides the
capability to embed screenshots in the output specification. It is based
on the original
[Screenshot Extension](https://github.com/concordion/concordion-screenshot-extension)
from the Concordion project. The main difference of this
implementation is the ability to take screenshots using
[Selenium WebDriver](https://www.selenium.dev/documentation/webdriver/),
making it suitable for end-to-end browser testing.

## Introduction

This extension has two main uses:

* taking screenshots to help diagnose test failures;
* explicitly adding screenshots to the output for documentation purposes.

## Installation

The extension is available from
[Maven Central](https://central.sonatype.com/artifact/io.granito.concordion/concordion-ext-screenshot).

## Configuration

Because this extension needs a `WebDriver` instance to function, use
the `@Extension` annotation on a `SeleniumScreenshotExtension`
instance field. This allows methods to be called to configure
the extension. In the example below, the `WebDriver` is provided
by dependency injection and configured in a method annotated with
`@PostConstruct`, ensuring that the `WebDriver` instance is available
at the configuration time.

```java
// use this class as a base for all UI test fixtures
public abstract class UiBase {
    @Extension
    public final SeleniumScreenshotExtension screenshot =
        new SeleniumScreenshotExtension();

    @Autowired
    protected WebDriver webDriver;

    @PostConstruct
    public void uiBaseInit()
    {
        screenshot.setWebDriver(webDriver);
        screenshot.setMaxWidth(400);
    }

    // ...
}
```

## Diagnosing failures

When running web UI tests it can be difficult to determine what was being
shown on the browser at the time of failure, especially if the tests are
running in the background or on a continuous integration server.

This extension adds screenshots to the Concordion output when failures
or exceptions occur. It can also be configured to add screenshots on
successful assertions.

The screenshot is displayed when you hover over the relevant element.
Clicking on the element will open the image in the current browser window.

## Explicitly taking screenshots

This extension also provides a Concordion `screenshot` command that
explicitly add screenshots to the output HTML for documentation purposes.

To use the command, add an attribute named `screenshot` using
the namespace `"urn:concordion-extensions:2010"` to an element in your
Concordion HTML. For example:

```html
<html xmlns:concordion="http://www.concordion.org/2007/concordion"
      xmlns:ext="urn:concordion-extensions:2010">

...
<div ext:screenshot=""/>
...
```

By default, the screenshot is embedded in the output HTML. If
you'd rather have it linked, set the attribute value to
'linked', for example:

```html
<p>See <span ext:screenshot="linked">this screen</span></p>
```

**NOTE:** If you want to use the extension only as a
command, and not to capture screenshots of test failures, you will need
to use a custom configuration that sets `setScreenshotOnAssertionFailure`
and `setScreenshotOnThrowable` to `false`. See below for
custom configuration details.

In a Markdown specification, HTML `<span>` tag can be used to wrap the
screenshot. To explicitly add a screenshot, add the above attribute to a
`<span>` tag within the Markdown specification, for example:

```markdown
<span ext:screenshot="linked">text</span>
```

You will also need to apply the following annotation to the corresponding
fixture class:

```java
@ConcordionFixture
@ConcordionOptions(declareNamespaces={"ext", "urn:concordion-extensions:2010"})
public class UiFixture extends UiBase {
    // ...
}
```

Or you can simply add it to the common base class for all your UI test
fixtures.
