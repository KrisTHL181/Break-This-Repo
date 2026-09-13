package net.minecraft.util.filefix.virtualfilesystem;

import org.jspecify.annotations.Nullable;

abstract sealed class Node permits FileNode, DirectoryNode {
   protected @Nullable DirectoryNode parent;
   protected CopyOnWriteFSPath path;

   protected Node(final CopyOnWriteFSPath cowPath) {
      this.setPath(cowPath);
   }

   public @Nullable String name() {
      CopyOnWriteFSPath fileName = this.path.getFileName();
      return fileName == null ? null : fileName.toString();
   }

   protected void setParent(final DirectoryNode parent) {
      this.parent = parent;
   }

   protected void setPath(final CopyOnWriteFSPath path) {
      this.path = path.normalize().toAbsolutePath();
   }

   public CopyOnWriteFSPath path() {
      return this.path;
   }
}
