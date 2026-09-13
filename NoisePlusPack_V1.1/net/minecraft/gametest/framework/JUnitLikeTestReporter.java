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

   public JUnitLikeTestReporter(File p_177664_) throws ParserConfigurationException {
      this.destination = p_177664_;
      this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      this.testSuite = this.document.createElement("testsuite");
      Element element = this.document.createElement("testsuite");
      element.appendChild(this.testSuite);
      this.document.appendChild(element);
      this.testSuite.setAttribute("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
      this.stopwatch = Stopwatch.createStarted();
   }

   private Element createTestCase(GameTestInfo p_177671_, String p_177672_) {
      Element element = this.document.createElement("testcase");
      element.setAttribute("name", p_177672_);
      element.setAttribute("classname", p_177671_.getStructure().toString());
      element.setAttribute("time", String.valueOf(p_177671_.getRunTime() / 1000.0));
      this.testSuite.appendChild(element);
      return element;
   }

   @Override
   public void onTestFailed(GameTestInfo p_177669_) {
      String s = p_177669_.id().toString();
      String s1 = p_177669_.getError().getMessage();
      Element element = this.document.createElement(p_177669_.isRequired() ? "failure" : "skipped");
      element.setAttribute("message", "(" + p_177669_.getTestBlockPos().toShortString() + ") " + s1);
      Element element1 = this.createTestCase(p_177669_, s);
      element1.appendChild(element);
   }

   @Override
   public void onTestSuccess(GameTestInfo p_177674_) {
      String s = p_177674_.id().toString();
      this.createTestCase(p_177674_, s);
   }

   @Override
   public void finish() {
      this.stopwatch.stop();
      this.testSuite.setAttribute("time", String.valueOf(this.stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0));

      try {
         this.save(this.destination);
      } catch (TransformerException transformerexception) {
         throw new Error("Couldn't save test report", transformerexception);
      }
   }

   public void save(File p_177667_) throws TransformerException {
      TransformerFactory transformerfactory = TransformerFactory.newInstance();
      Transformer transformer = transformerfactory.newTransformer();
      DOMSource domsource = new DOMSource(this.document);
      StreamResult streamresult = new StreamResult(p_177667_);
      transformer.transform(domsource, streamresult);
   }
}
