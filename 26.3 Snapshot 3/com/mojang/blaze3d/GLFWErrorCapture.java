package com.mojang.blaze3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

public class GLFWErrorCapture implements GLFWErrorCallbackI, Iterable<GLFWErrorCapture.Error> {
   private @Nullable List<GLFWErrorCapture.Error> errors;

   public void invoke(final int error, final long description) {
      if (this.errors == null) {
         this.errors = new ArrayList<>();
      }

      this.errors.add(new GLFWErrorCapture.Error(error, MemoryUtil.memUTF8(description)));
   }

   @Override
   public Iterator<GLFWErrorCapture.Error> iterator() {
      return this.errors == null ? Collections.emptyIterator() : this.errors.iterator();
   }

   public GLFWErrorCapture.@Nullable Error firstError() {
      return this.errors == null ? null : this.errors.getFirst();
   }

   public record Error(int error, String description) {
      @Override
      public String toString() {
         return String.format(Locale.ROOT, "[GLFW 0x%X] %s", this.error, this.description);
      }
   }
}
