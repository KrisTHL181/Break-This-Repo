package net.minecraft.server.dedicated;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

public class DedicatedServerSettings {
   private final Path source;
   private DedicatedServerProperties properties;

   public DedicatedServerSettings(final Path source) {
      this.source = source;
      this.properties = DedicatedServerProperties.fromFile(source);
   }

   public DedicatedServerProperties getProperties() {
      return this.properties;
   }

   public void forceSave() {
      this.properties.store(this.source);
   }

   public DedicatedServerSettings update(final UnaryOperator<DedicatedServerProperties> mutator) {
      (this.properties = mutator.apply(this.properties)).store(this.source);
      return this;
   }
}
