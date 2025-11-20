# Write Extension for Concordion

This [Concordion](https://www.concordion.org/) extension provides the
capability to write an expression evaluation result to a file and
create a link to the file in the output specification.

## Introduction

This extension supports the following use cases:

* downloading a resource from the system under test and making it
  available in the specification for manual inspection, for example,
  an image, a PDF document or an Excel spreadsheet;
* receiving an e-mail or a message from the system under test and making
  the content available in the specification for manual inspection.

The manual inspection can be necessary to verify the formatting of the
content, its layout, aesthetic characteristics or other visual aspects
that are difficult to automate.

## Installation

The extension is available from
[Maven Central](https://central.sonatype.com/artifact/io.granito.concordion/concordion-ext-write).

## Configuration

To install the extension, either annotate the fixture class with:

```java
@ConcordionFixture
@Extensions(WriteExtension.class)
public class MyFixture {
    // ...
}
```

or set the system property `concordion.extensions` to

`io.granito.concordion.ext.WriteExtension`

## Usage

This extension provides a Concordion `write` command that
writes the results of the expression evaluation to a file and links it to
the output HTML.

To use the command, add an attribute named `write` using
the namespace `"urn:concordion-extensions:2010"` to an `<a>`
element in your Concordion HTML. For example:

```html
<html xmlns:concordion="http://www.concordion.org/2007/concordion"
      xmlns:ext="urn:concordion-extensions:2010">

...
<a ext:write="#document" href="document.pdf">Document</a>
...
```

In a Markdown specification, HTML `<span>` tag can be used to wrap the
link.

```markdown
<span><a ext:write="#document" href="document.pdf">Document</a></span>
```

You will also need to apply the following annotation to the corresponding
fixture class:

```java
@ConcordionFixture
@Extensions(WriteExtension.class)
@ConcordionOptions(declareNamespaces={"ext", "urn:concordion-extensions:2010"})
public class MyFixture {
    // ...
}
```

It is expected that the expression evaluates to a byte array. If the
expression does not evaluate to a byte array, `toString()` method
will be used on it and the resulting string will be written to the
file using UTF-8 encoding.
