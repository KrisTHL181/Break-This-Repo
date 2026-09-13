package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class JigsawRotationFix extends AbstractBlockPropertyFix {
   private static final Map<String, String> RENAMES = ImmutableMap.builder()
      .put("down", "down_south")
      .put("up", "up_north")
      .put("north", "north_up")
      .put("south", "south_up")
      .put("west", "west_up")
      .put("east", "east_up")
      .build();

   public JigsawRotationFix(final Schema outputSchema) {
      super(outputSchema, "jigsaw_rotation_fix");
   }

   @Override
   protected boolean shouldFix(final String blockId) {
      return blockId.equals("minecraft:jigsaw");
   }

   @Override
   protected <T> Dynamic<T> fixProperties(final String blockId, final Dynamic<T> properties) {
      String facing = properties.get("facing").asString("north");
      return properties.remove("facing").set("orientation", properties.createString(RENAMES.getOrDefault(facing, facing)));
   }
}
