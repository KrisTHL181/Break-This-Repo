package net.minecraft.server.packs;

public enum PackType {
   CLIENT_RESOURCES("assets"),
   SERVER_DATA("data");

   private final String directory;

   PackType(final String directory) {
      this.directory = directory;
   }

   public String getDirectory() {
      return this.directory;
   }
}
