package net.minecraft.server.packs.linkfs;

import java.nio.file.Path;
import java.util.Map;

interface PathContents {
   PathContents MISSING = new PathContents() {
      @Override
      public String toString() {
         return "empty";
      }
   };
   PathContents RELATIVE = new PathContents() {
      @Override
      public String toString() {
         return "relative";
      }
   };

   record DirectoryContents(Map<String, LinkFSPath> children) implements PathContents {
   }

   record FileContents(Path contents) implements PathContents {
   }
}
