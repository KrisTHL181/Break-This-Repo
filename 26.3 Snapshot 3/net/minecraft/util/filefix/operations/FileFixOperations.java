package net.minecraft.util.filefix.operations;

import java.util.List;
import java.util.Map;
import net.minecraft.util.filefix.access.FileRelation;

public class FileFixOperations {
   public static Move moveSimple(final String file) {
      return new Move(file, file);
   }

   public static Move move(final String from, final String to) {
      return new Move(from, to);
   }

   public static RegexMove moveRegex(final String filePattern, final String replacePattern) {
      return new RegexMove(filePattern, replacePattern);
   }

   public static DeleteFileOrEmptyDirectory delete(final String target) {
      return new DeleteFileOrEmptyDirectory(target);
   }

   public static ApplyInFolders applyInFolders(final FileRelation applicableFolders, final List<FileFixOperation> operations) {
      return new ApplyInFolders(applicableFolders, operations);
   }

   public static GroupMove groupMove(final Map<String, String> data, final List<Move> move) {
      return new GroupMove(data, move);
   }
}
