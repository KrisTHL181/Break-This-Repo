package com.mojang.blaze3d.opengl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.logging.LogUtils;
import java.util.HexFormat;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlDebug {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CIRCULAR_LOG_SIZE = 10;
   private final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
   private volatile GlDebug.@Nullable LogEntry lastEntry;
   private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
   private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);

   private static String printUnknownToken(int p_396556_) {
      return "Unknown (0x" + HexFormat.of().withUpperCase().toHexDigits(p_396556_) + ")";
   }

   public static String sourceToString(int p_392197_) {
      switch (p_392197_) {
         case 33350:
            return "API";
         case 33351:
            return "WINDOW SYSTEM";
         case 33352:
            return "SHADER COMPILER";
         case 33353:
            return "THIRD PARTY";
         case 33354:
            return "APPLICATION";
         case 33355:
            return "OTHER";
         default:
            return printUnknownToken(p_392197_);
      }
   }

   public static String typeToString(int p_393670_) {
      switch (p_393670_) {
         case 33356:
            return "ERROR";
         case 33357:
            return "DEPRECATED BEHAVIOR";
         case 33358:
            return "UNDEFINED BEHAVIOR";
         case 33359:
            return "PORTABILITY";
         case 33360:
            return "PERFORMANCE";
         case 33361:
            return "OTHER";
         case 33384:
            return "MARKER";
         default:
            return printUnknownToken(p_393670_);
      }
   }

   public static String severityToString(int p_395913_) {
      switch (p_395913_) {
         case 33387:
            return "NOTIFICATION";
         case 37190:
            return "HIGH";
         case 37191:
            return "MEDIUM";
         case 37192:
            return "LOW";
         default:
            return printUnknownToken(p_395913_);
      }
   }

   private void printDebugLog(int p_391432_, int p_393126_, int p_395489_, int p_393407_, int p_397884_, long p_395821_, long p_396708_) {
      String s = GLDebugMessageCallback.getMessage(p_397884_, p_395821_);
      GlDebug.LogEntry gldebug$logentry;
      synchronized (this.MESSAGE_BUFFER) {
         gldebug$logentry = this.lastEntry;
         if (gldebug$logentry != null && gldebug$logentry.isSame(p_391432_, p_393126_, p_395489_, p_393407_, s)) {
            gldebug$logentry.count++;
         } else {
            gldebug$logentry = new GlDebug.LogEntry(p_391432_, p_393126_, p_395489_, p_393407_, s);
            this.MESSAGE_BUFFER.add(gldebug$logentry);
            this.lastEntry = gldebug$logentry;
         }
      }

      LOGGER.info("OpenGL debug message: {}", gldebug$logentry);
   }

   public List<String> getLastOpenGlDebugMessages() {
      synchronized (this.MESSAGE_BUFFER) {
         List<String> list = Lists.newArrayListWithCapacity(this.MESSAGE_BUFFER.size());

         for (GlDebug.LogEntry gldebug$logentry : this.MESSAGE_BUFFER) {
            list.add(gldebug$logentry + " x " + gldebug$logentry.count);
         }

         return list;
      }
   }

   public static @Nullable GlDebug enableDebugCallback(int p_394351_, boolean p_393026_, Set<String> p_393339_) {
      if (p_394351_ <= 0) {
         return null;
      }

      GLCapabilities glcapabilities = GL.getCapabilities();
      if (glcapabilities.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
         GlDebug gldebug1 = new GlDebug();
         p_393339_.add("GL_KHR_debug");
         GL11.glEnable(37600);
         if (p_393026_) {
            GL11.glEnable(33346);
         }

         for (int j = 0; j < DEBUG_LEVELS.size(); j++) {
            boolean flag1 = j < p_394351_;
            KHRDebug.glDebugMessageControl(4352, 4352, DEBUG_LEVELS.get(j), (int[])null, flag1);
         }

         KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(gldebug1::printDebugLog), DebugMemoryUntracker::untrack), 0L);
         return gldebug1;
      } else if (glcapabilities.GL_ARB_debug_output && GlDevice.USE_GL_ARB_debug_output) {
         GlDebug gldebug = new GlDebug();
         p_393339_.add("GL_ARB_debug_output");
         if (p_393026_) {
            GL11.glEnable(33346);
         }

         for (int i = 0; i < DEBUG_LEVELS_ARB.size(); i++) {
            boolean flag = i < p_394351_;
            ARBDebugOutput.glDebugMessageControlARB(4352, 4352, DEBUG_LEVELS_ARB.get(i), (int[])null, flag);
         }

         ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(gldebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
         return gldebug;
      } else {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LogEntry {
      private final int id;
      private final int source;
      private final int type;
      private final int severity;
      private final String message;
      int count = 1;

      LogEntry(int p_393196_, int p_394115_, int p_392842_, int p_391912_, String p_391249_) {
         this.id = p_392842_;
         this.source = p_393196_;
         this.type = p_394115_;
         this.severity = p_391912_;
         this.message = p_391249_;
      }

      boolean isSame(int p_397925_, int p_394526_, int p_397126_, int p_397660_, String p_391197_) {
         return p_394526_ == this.type && p_397925_ == this.source && p_397126_ == this.id && p_397660_ == this.severity && p_391197_.equals(this.message);
      }

      @Override
      public String toString() {
         return "id="
            + this.id
            + ", source="
            + GlDebug.sourceToString(this.source)
            + ", type="
            + GlDebug.typeToString(this.type)
            + ", severity="
            + GlDebug.severityToString(this.severity)
            + ", message='"
            + this.message
            + "'";
      }
   }
}
