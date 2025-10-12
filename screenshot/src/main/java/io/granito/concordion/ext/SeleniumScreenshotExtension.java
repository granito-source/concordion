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

import static javax.imageio.ImageIO.getImageReadersBySuffix;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.concordion.ext.ScreenshotExtension;
import org.concordion.ext.ScreenshotTaker;
import org.concordion.ext.ScreenshotUnavailableException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class SeleniumScreenshotExtension extends ScreenshotExtension
    implements ScreenshotTaker {
    private WebDriver webDriver;

    public SeleniumScreenshotExtension()
    {
        setScreenshotTaker(this);
    }

    public void setWebDriver(WebDriver webDriver)
    {
        this.webDriver = webDriver;
    }

    @Override
    public Dimension writeScreenshotTo(OutputStream outputStream)
        throws IOException
    {
        try (var out = outputStream) {
            if (webDriver == null)
                throw new ScreenshotUnavailableException(
                    "WebDriver is not configured");

            var screenshot = ((TakesScreenshot)webDriver)
                .getScreenshotAs(OutputType.BYTES);

            out.write(screenshot);

            return extractDimensions(screenshot);
        } catch (ClassCastException ex) {
            throw new ScreenshotUnavailableException(
                "WebDriver does not support taking screenshots");
        }
    }

    @Override
    public String getFileExtension()
    {
        return "png";
    }

    private Dimension extractDimensions(byte[] screenshot)
        throws IOException
    {
        var ext = getFileExtension();
        var readers = getImageReadersBySuffix(ext);

        while (readers.hasNext()) {
            var reader = readers.next();

            try (var in = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(screenshot))) {
                reader.setInput(in);

                return new Dimension(reader.getWidth(reader.getMinIndex()),
                    reader.getHeight(reader.getMinIndex()));
            } catch (IOException ex) {
                // ignore and try the next reader
            } finally {
                reader.dispose();
            }
        }

        throw new IOException("unable to read ." + ext + " image");
    }
}
