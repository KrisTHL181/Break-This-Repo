package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JUnitLikeTestReporter implements TestReporter {
   private final Document document;
   private final Element testSuite;
   private final Stopwatch stopwatch;
   private final File destination;

   public JUnitLikeTestReporter(final File destination) throws ParserConfigurationException {
      this.destination = destination;
      this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      this.testSuite = this.document.createElement("testsuite");
      Element testSuites = this.document.createElement("testsuite");
      testSuites.appendChild(this.testSuite);
      this.document.appendChild(testSuites);
      this.testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
      this.stopwatch = Stopwatch.createStarted();
   }

   private Element createTestCase(final GameTestInfo testInfo, final String name) {
      Element testCase = this.document.createElement("testcase");
      testCase.setAttribute("name", name);
      testCase.setAttribute("classname", testInfo.getStructure().toString());
      testCase.setAttribute("time", String.valueOf(testInfo.getRunTime() / 1000.0));
      this.testSuite.appendChild(testCase);
      return testCase;
   }

   @Override
   public void onTestFailed(final GameTestInfo testInfo) {
      String name = testInfo.id().toString();
      String message = testInfo.getError().getMessage();
      Element result = this.document.createElement(testInfo.isRequired() ? "failure" : "skipped");
      result.setAttribute("message", "(" + testInfo.getTestBlockPos().toShortString() + ") " + message);
      Element testCase = this.createTestCase(testInfo, name);
      testCase.appendChild(result);
   }

   @Override
   public void onTestSuccess(final GameTestInfo testInfo) {
      String name = testInfo.id().toString();
      this.createTestCase(testInfo, name);
   }

   @Override
   public void finish() {
      this.stopwatch.stop();
      this.testSuite.setAttribute("time", String.valueOf(this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));

      try {
         this.save(this.destination);
      } catch (TransformerException exception) {
         throw new Error("Couldn't save test report", exception);
      }
   }

   public void save(final File file) throws TransformerException {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(this.document);
      StreamResult result = new StreamResult(file);
      transformer.transform(source, result);
   }
}
