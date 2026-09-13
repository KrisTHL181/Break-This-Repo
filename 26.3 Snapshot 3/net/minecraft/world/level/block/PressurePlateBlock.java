package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PressurePlateBlock extends BasePressurePlateBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   protected PressurePlateBlock(final BlockSetType type, final BlockBehaviour.Properties properties) {
      super(properties, type);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
   }

   @Override
   protected int getSignalForState(final BlockState state) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected BlockState setSignalForState(final BlockState state, final int signal) {
      return state.setValue(POWERED, signal > 0);
   }

   @Override
   protected int getSignalStrength(final Level level, final BlockPos pos) {
      Class<? extends Entity> entityClass = switch (this.type.pressurePlateSensitivity()) {
         case EVERYTHING -> Entity.class;
         case MOBS -> LivingEntity.class;
      };
      return getEntityCount(level, TOUCH_AABB.move(pos), entityClass) > 0 ? 15 : 0;
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(POWERED);
   }
}
