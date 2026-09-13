package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.filefix.FileFixUtil;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record Move(String from, String to) implements FileFixOperation {
   public Move relative(final String sourceFolder, final String targetFolder) {
      return new Move(sourceFolder + "/" + this.from, targetFolder + "/" + this.to);
   }

   @Override
   public void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException {
      FileFixUtil.moveFile(baseDirectory, this.from, this.to);
   }
}
