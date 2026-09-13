package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BedBlock extends AbstractBedBlock {
   private final DyeColor color;
   private static final Map<Direction, VoxelShape> SHAPES = Util.make(() -> {
      VoxelShape northWestLeg = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
      VoxelShape northEastLeg = Shapes.rotate(northWestLeg, OctahedralGroup.BLOCK_ROT_Y_90);
      return Shapes.rotateHorizontal(Shapes.or(Block.column(16.0, 3.0, 9.0), northWestLeg, northEastLeg));
   });

   public BedBlock(final DyeColor color, final BlockBehaviour.Properties properties) {
      super(properties);
      this.color = color;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES.get(getConnectedDirection(state).getOpposite());
   }

   @Override
   protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
      return EnvironmentAttributes.BED_RULE;
   }

   @Override
   protected InteractionResult destroyOnUse(final BlockState state, final Level level, BlockPos pos, final Player player) {
      level.removeBlock(pos, false);
      BlockPos blockPos = pos.relative(state.getValue(FACING).getOpposite());
      if (level.getBlockState(blockPos).is(this)) {
         level.removeBlock(blockPos, false);
      }

      Vec3 boomPos = Vec3.atCenterOf(pos);
      level.explode(null, level.damageSources().badRespawnPointExplosion(boomPos), null, boomPos, 5.0F, true, Level.ExplosionInteraction.BLOCK);
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   protected void destroyOnLeave(final Level level, final BlockPos pos) {
   }

   public DyeColor getColor() {
      return this.color;
   }
}
