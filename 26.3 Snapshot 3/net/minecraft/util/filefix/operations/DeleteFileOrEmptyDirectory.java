package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.filefix.FileFixUtil;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record DeleteFileOrEmptyDirectory(String target) implements FileFixOperation {
   @Override
   public void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException {
      FileFixUtil.deleteFileOrEmptyDirectory(baseDirectory, this.target);
   }
}
