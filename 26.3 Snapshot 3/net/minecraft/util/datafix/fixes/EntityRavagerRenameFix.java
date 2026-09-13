package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class EntityRavagerRenameFix extends SimplestEntityRenameFix {
   public static final Map<String, String> RENAMED_IDS = ImmutableMap.builder().put("minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg").build();

   public EntityRavagerRenameFix(final Schema outputSchema, final boolean changesType) {
      super("EntityRavagerRenameFix", outputSchema, changesType);
   }

   @Override
   protected String rename(final String name) {
      return Objects.equals("minecraft:illager_beast", name) ? "minecraft:ravager" : name;
   }
}
