package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import net.minecraft.util.FileUtil;
import net.minecraft.util.MemoryReserve;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CrashReport {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
   private final String title;
   private final Throwable exception;
   private final List<CrashReportCategory> details = Lists.newArrayList();
   private @Nullable Path saveFile;
   private boolean trackingStackTrace = true;
   private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];
   private final SystemReport systemReport = new SystemReport();

   public CrashReport(String p_127509_, Throwable p_127510_) {
      this.title = p_127509_;
      this.exception = p_127510_;
   }

   public String getTitle() {
      return this.title;
   }

   public Throwable getException() {
      return this.exception;
   }

   public String getDetails() {
      StringBuilder stringbuilder = new StringBuilder();
      this.getDetails(stringbuilder);
      return stringbuilder.toString();
   }

   public void getDetails(StringBuilder p_127520_) {
      if ((this.uncategorizedStackTrace == null || this.uncategorizedStackTrace.length <= 0) && !this.details.isEmpty()) {
         this.uncategorizedStackTrace = (StackTraceElement[])ArrayUtils.subarray(this.details.get(0).getStacktrace(), 0, 1);
      }

      if (this.uncategorizedStackTrace != null && this.uncategorizedStackTrace.length > 0) {
         p_127520_.append("-- Head --\n");
         p_127520_.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
         p_127520_.append("Stacktrace:\n");

         for (StackTraceElement stacktraceelement : this.uncategorizedStackTrace) {
            p_127520_.append("\t").append("at ").append(stacktraceelement);
            p_127520_.append("\n");
         }

         p_127520_.append("\n");
      }

      for (CrashReportCategory crashreportcategory : this.details) {
         crashreportcategory.getDetails(p_127520_);
         p_127520_.append("\n\n");
      }

      this.systemReport.appendToCrashReportString(p_127520_);
   }

   public String getExceptionMessage() {
      StringWriter stringwriter = null;
      PrintWriter printwriter = null;
      Throwable throwable = this.exception;
      if (throwable.getMessage() == null) {
         if (throwable instanceof NullPointerException) {
            throwable = new NullPointerException(this.title);
         } else if (throwable instanceof StackOverflowError) {
            throwable = new StackOverflowError(this.title);
         } else if (throwable instanceof OutOfMemoryError) {
            throwable = new OutOfMemoryError(this.title);
         }

         throwable.setStackTrace(this.exception.getStackTrace());
      }

      String s;
      try {
         stringwriter = new StringWriter();
         printwriter = new PrintWriter(stringwriter);
         throwable.printStackTrace(printwriter);
         s = stringwriter.toString();
      } finally {
         IOUtils.closeQuietly(stringwriter);
         IOUtils.closeQuietly(printwriter);
      }

      return s;
   }

   public String getFriendlyReport(ReportType p_343869_, List<String> p_342487_) {
      StringBuilder stringbuilder = new StringBuilder();
      p_343869_.appendHeader(stringbuilder, p_342487_);
      stringbuilder.append("Time: ");
      stringbuilder.append(DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
      stringbuilder.append("\n");
      stringbuilder.append("Description: ");
      stringbuilder.append(this.title);
      stringbuilder.append("\n\n");
      stringbuilder.append(this.getExceptionMessage());
      stringbuilder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

      for (int i = 0; i < 87; i++) {
         stringbuilder.append("-");
      }

      stringbuilder.append("\n\n");
      this.getDetails(stringbuilder);
      return stringbuilder.toString();
   }

   public String getFriendlyReport(ReportType p_343367_) {
      return this.getFriendlyReport(p_343367_, List.of());
   }

   public @Nullable Path getSaveFile() {
      return this.saveFile;
   }

   public boolean saveToFile(Path p_343023_, ReportType p_343502_, List<String> p_344584_) {
      if (this.saveFile != null) {
         return false;
      }

      try {
         if (p_343023_.getParent() != null) {
            FileUtil.createDirectoriesSafe(p_343023_.getParent());
         }

         try (Writer writer = Files.newBufferedWriter(p_343023_, StandardCharsets.UTF_8)) {
            writer.write(this.getFriendlyReport(p_343502_, p_344584_));
         }

         this.saveFile = p_343023_;
         return true;
      } catch (Throwable throwable1) {
         LOGGER.error("Could not save crash report to {}", p_343023_, throwable1);
         return false;
      }
   }

   public boolean saveToFile(Path p_342057_, ReportType p_344042_) {
      return this.saveToFile(p_342057_, p_344042_, List.of());
   }

   public SystemReport getSystemReport() {
      return this.systemReport;
   }

   public CrashReportCategory addCategory(String p_127515_) {
      return this.addCategory(p_127515_, 1);
   }

   public CrashReportCategory addCategory(String p_127517_, int p_127518_) {
      CrashReportCategory crashreportcategory = new CrashReportCategory(p_127517_);
      if (this.trackingStackTrace) {
         int i = crashreportcategory.fillInStackTrace(p_127518_);
         StackTraceElement[] astacktraceelement = this.exception.getStackTrace();
         StackTraceElement stacktraceelement = null;
         StackTraceElement stacktraceelement1 = null;
         int j = astacktraceelement.length - i;
         if (j < 0) {
            LOGGER.error("Negative index in crash report handler ({}/{})", astacktraceelement.length, i);
         }

         if (astacktraceelement != null && 0 <= j && j < astacktraceelement.length) {
            stacktraceelement = astacktraceelement[j];
            if (astacktraceelement.length + 1 - i < astacktraceelement.length) {
               stacktraceelement1 = astacktraceelement[astacktraceelement.length + 1 - i];
            }
         }

         this.trackingStackTrace = crashreportcategory.validateStackTrace(stacktraceelement, stacktraceelement1);
         if (astacktraceelement != null && astacktraceelement.length >= i && 0 <= j && j < astacktraceelement.length) {
            this.uncategorizedStackTrace = new StackTraceElement[j];
            System.arraycopy(astacktraceelement, 0, this.uncategorizedStackTrace, 0, this.uncategorizedStackTrace.length);
         } else {
            this.trackingStackTrace = false;
         }
      }

      this.details.add(crashreportcategory);
      return crashreportcategory;
   }

   public static CrashReport forThrowable(Throwable p_127522_, String p_127523_) {
      while (p_127522_ instanceof CompletionException && p_127522_.getCause() != null) {
         p_127522_ = p_127522_.getCause();
      }

      CrashReport crashreport;
      if (p_127522_ instanceof ReportedException reportedexception) {
         crashreport = reportedexception.getReport();
      } else {
         crashreport = new CrashReport(p_127523_, p_127522_);
      }

      return crashreport;
   }

   public static void preload() {
      MemoryReserve.allocate();
      new CrashReport("Don't panic!", new Throwable()).getFriendlyReport(ReportType.CRASH);
   }
}
