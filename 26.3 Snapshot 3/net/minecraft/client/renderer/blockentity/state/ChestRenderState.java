package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

public class ChestRenderState extends BlockEntityRenderState {
   public ChestType type = ChestType.SINGLE;
   public float open;
   public Direction facing = Direction.SOUTH;
   public ChestRenderState.ChestMaterialType material = ChestRenderState.ChestMaterialType.REGULAR;

   public enum ChestMaterialType {
      ENDER_CHEST,
      CHRISTMAS,
      TRAPPED,
      COPPER_UNAFFECTED,
      COPPER_EXPOSED,
      COPPER_WEATHERED,
      COPPER_OXIDIZED,
      REGULAR;
   }
}
