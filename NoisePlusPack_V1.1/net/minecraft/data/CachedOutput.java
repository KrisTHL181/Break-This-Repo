package net.minecraft.data;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.FileUtil;

public interface CachedOutput {
   CachedOutput NO_CACHE = (p_448624_, p_448625_, p_448626_) -> {
      FileUtil.createDirectoriesSafe(p_448624_.getParent());
      Files.write(p_448624_, p_448625_);
   };

   void writeIfNeeded(Path var1, byte[] var2, HashCode var3) throws IOException;
}
