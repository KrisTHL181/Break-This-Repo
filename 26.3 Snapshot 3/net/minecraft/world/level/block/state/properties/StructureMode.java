package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public enum StructureMode implements StringRepresentable {
   SAVE("save"),
   LOAD("load"),
   CORNER("corner"),
   DATA("data");

   @Deprecated
   public static final Codec<StructureMode> LEGACY_CODEC = ExtraCodecs.legacyEnum(StructureMode::valueOf);
   private final String name;
   private final Component displayName;

   StructureMode(final String name) {
      this.name = name;
      this.displayName = Component.translatable("structure_block.mode_info." + name);
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public Component getDisplayName() {
      return this.displayName;
   }
}
