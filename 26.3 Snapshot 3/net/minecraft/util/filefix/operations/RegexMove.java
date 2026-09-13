package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.util.filefix.FileFixUtil;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record RegexMove(Pattern fromPattern, String toReplacement) implements FileFixOperation {
   public RegexMove(final String fromPattern, final String toPattern) {
      this(Pattern.compile(fromPattern), toPattern);
   }

   @Override
   public void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException {
      if (Files.exists(baseDirectory) && Files.isDirectory(baseDirectory)) {
         try (Stream<Path> files = Files.list(baseDirectory)) {
            for (Path file : files.toList()) {
               String fileName = file.getFileName().toString();
               Matcher matcher = this.fromPattern.matcher(fileName);
               if (matcher.matches()) {
                  String newName = matcher.replaceAll(this.toReplacement);
                  FileFixUtil.moveFile(baseDirectory, fileName, newName);
               }
            }
         }
      }
   }
}
