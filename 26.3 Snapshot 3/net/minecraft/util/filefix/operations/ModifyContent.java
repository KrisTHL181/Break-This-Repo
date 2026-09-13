package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.minecraft.util.filefix.access.FileAccessProvider;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class ModifyContent implements FileFixOperation {
   private final FileAccessProvider fileAccessProvider;
   private final ModifyContent.FixFunction fixFunction;

   public ModifyContent(final FileAccessProvider fileAccessProvider, final ModifyContent.FixFunction fixFunction) {
      this.fileAccessProvider = fileAccessProvider;
      this.fixFunction = fixFunction;
   }

   @Override
   public void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException {
      try {
         ScopedValue.where(this.fileAccessProvider.baseDirectory(), baseDirectory).run(() -> {
            try {
               this.fixFunction.run(upgradeProgress);
            } catch (IOException e) {
               throw new UncheckedIOException(e);
            }
         });
      } catch (UncheckedIOException e) {
         throw e.getCause();
      } finally {
         this.fileAccessProvider.close();
      }
   }

   @FunctionalInterface
   public interface FileAccessFunction {
      ModifyContent.FixFunction make(final FileAccessProvider fileAccessProvider);
   }

   @FunctionalInterface
   public interface FixFunction {
      void run(final UpgradeProgress upgradeProgress) throws IOException;
   }
}
