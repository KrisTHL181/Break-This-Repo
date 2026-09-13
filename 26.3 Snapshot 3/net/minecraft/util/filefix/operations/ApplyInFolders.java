package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record ApplyInFolders(FileRelation folders, List<FileFixOperation> fileFixOperations) implements FileFixOperation {
   @Override
   public void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException {
      for (Path path : this.folders.getPaths(baseDirectory)) {
         for (FileFixOperation operation : this.fileFixOperations) {
            operation.fix(path, upgradeProgress);
         }
      }
   }
}
