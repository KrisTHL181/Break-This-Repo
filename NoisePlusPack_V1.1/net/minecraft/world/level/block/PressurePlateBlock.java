package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
   public static final MapCodec<PressurePlateBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422120_ -> p_422120_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_313030_ -> p_313030_.type), propertiesCodec())
         .apply(p_422120_, PressurePlateBlock::new)
   );
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   @Override
   public MapCodec<PressurePlateBlock> codec() {
      return CODEC;
   }

   protected PressurePlateBlock(BlockSetType p_273284_, BlockBehaviour.Properties p_273571_) {
      super(p_273571_, p_273284_);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
   }

   @Override
   protected int getSignalForState(BlockState p_55270_) {
      return p_55270_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected BlockState setSignalForState(BlockState p_55259_, int p_55260_) {
      return p_55259_.setValue(POWERED, p_55260_ > 0);
   }

   @Override
   protected int getSignalStrength(Level p_55264_, BlockPos p_55265_) {
      Class<? extends Entity> oclass = switch (this.type.pressurePlateSensitivity()) {
         case EVERYTHING -> Entity.class;
         case MOBS -> LivingEntity.class;
      };
      return getEntityCount(p_55264_, TOUCH_AABB.move(p_55265_), oclass) > 0 ? 15 : 0;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55262_) {
      p_55262_.add(POWERED);
   }
}
