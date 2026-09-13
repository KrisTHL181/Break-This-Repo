package net.minecraft.client.gui.font.providers;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FreeTypeUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Object LIBRARY_LOCK = new Object();
   private static long library = 0L;

   public static long getLibrary() {
      synchronized (LIBRARY_LOCK) {
         if (library == 0L) {
            MemoryStack memorystack = MemoryStack.stackPush();

            try {
               PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
               assertError(FreeType.FT_Init_FreeType(pointerbuffer), "Initializing FreeType library");
               library = pointerbuffer.get();
            } catch (Throwable var6) {
               if (memorystack != null) {
                  try {
                     memorystack.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (memorystack != null) {
               memorystack.close();
            }
         }

         return library;
      }
   }

   public static void assertError(int p_328560_, String p_336278_) {
      if (p_328560_ != 0) {
         throw new IllegalStateException("FreeType error: " + describeError(p_328560_) + " (" + p_336278_ + ")");
      }
   }

   public static boolean checkError(int p_333415_, String p_334613_) {
      if (p_333415_ != 0) {
         LOGGER.error("FreeType error: {} ({})", describeError(p_333415_), p_334613_);
         return true;
      } else {
         return false;
      }
   }

   private static String describeError(int p_328820_) {
      String s = FreeType.FT_Error_String(p_328820_);
      return s != null ? s : "Unrecognized error: 0x" + Integer.toHexString(p_328820_);
   }

   public static FT_Vector setVector(FT_Vector p_332923_, float p_329595_, float p_330314_) {
      long i = Math.round(p_329595_ * 64.0F);
      long j = Math.round(p_330314_ * 64.0F);
      return p_332923_.set(i, j);
   }

   public static float x(FT_Vector p_334185_) {
      return (float)p_334185_.x() / 64.0F;
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
