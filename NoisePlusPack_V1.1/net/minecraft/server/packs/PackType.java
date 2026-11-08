package net.minecraft.server.packs;

public enum PackType {
   CLIENT_RESOURCES("assets"),
   SERVER_DATA("data");

   private final String directory;

   PackType(final String p_265479_) {
      this.directory = p_265479_;
   }

   public String getDirectory() {
      return this.directory;
   }
}
