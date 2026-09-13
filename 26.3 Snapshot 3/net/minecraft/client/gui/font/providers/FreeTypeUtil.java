package net.minecraft.client.gui.font.providers;

import com.mojang.logging.LogUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

public class FreeTypeUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Object LIBRARY_LOCK = new Object();
   private static long library = 0L;

   public static long getLibrary() {
      synchronized (LIBRARY_LOCK) {
         if (library == 0L) {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               PointerBuffer libraryBuffer = stack.mallocPointer(1);
               assertError(FreeType.FT_Init_FreeType(libraryBuffer), "Initializing FreeType library");
               library = libraryBuffer.get();
            } catch (Throwable var6) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (stack != null) {
               stack.close();
            }
         }

         return library;
      }
   }

   public static void assertError(final int errorCode, final String type) {
      if (errorCode != 0) {
         throw new IllegalStateException("FreeType error: " + describeError(errorCode) + " (" + type + ")");
      }
   }

   public static boolean checkError(final int errorCode, final String type) {
      if (errorCode != 0) {
         LOGGER.error("FreeType error: {} ({})", describeError(errorCode), type);
         return true;
      } else {
         return false;
      }
   }

   private static String describeError(final int code) {
      String string = FreeType.FT_Error_String(code);
      return string != null ? string : "Unrecognized error: 0x" + Integer.toHexString(code);
   }

   public static FT_Vector setVector(final FT_Vector vector, final float x, final float y) {
      long fixedPointX = Math.round(x * 64.0F);
      long fixedPointY = Math.round(y * 64.0F);
      return vector.set(fixedPointX, fixedPointY);
   }

   public static float x(final FT_Vector vector) {
      return (float)vector.x() / 64.0F;
   }

   public static void destroy() {
      synchronized (LIBRARY_LOCK) {
         if (library != 0L) {
            FreeType.FT_Done_Library(library);
            library = 0L;
         }
      }
   }
}
