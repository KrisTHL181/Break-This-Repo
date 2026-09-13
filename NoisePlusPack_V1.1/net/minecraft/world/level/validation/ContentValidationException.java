package net.minecraft.world.level.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ContentValidationException extends Exception {
   private final Path directory;
   private final List<ForbiddenSymlinkInfo> entries;

   public ContentValidationException(Path p_289932_, List<ForbiddenSymlinkInfo> p_289984_) {
      this.directory = p_289932_;
      this.entries = p_289984_;
   }

   @Override
   public String getMessage() {
      return getMessage(this.directory, this.entries);
   }

   public static String getMessage(Path p_289929_, List<ForbiddenSymlinkInfo> p_289979_) {
      return "Failed to validate '"
         + p_289929_
         + "'. Found forbidden symlinks: "
         + p_289979_.stream().map(p_327657_ -> p_327657_.link() + "->" + p_327657_.target()).collect(Collectors.joining(", "));
   }
}
