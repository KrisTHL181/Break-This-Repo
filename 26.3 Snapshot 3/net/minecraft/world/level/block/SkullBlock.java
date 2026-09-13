package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SkullBlock extends AbstractSkullBlock {
   public static final int MAX = RotationSegment.getMaxSegmentIndex();
   private static final int ROTATIONS = MAX + 1;
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 8.0);
   private static final VoxelShape SHAPE_PIGLIN = Block.column(10.0, 0.0, 8.0);

   protected SkullBlock(final SkullBlock.Type type, final BlockBehaviour.Properties properties) {
      super(type, properties);
      this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, 0));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return this.getType() == SkullBlock.Types.PIGLIN ? SHAPE_PIGLIN : SHAPE;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      return super.getStateForPlacement(context).setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation()));
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), ROTATIONS));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATIONS));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(ROTATION);
   }

   public interface Type extends StringRepresentable {
      Map<String, SkullBlock.Type> TYPES = new Object2ObjectArrayMap();
      Codec<SkullBlock.Type> CODEC = Codec.stringResolver(StringRepresentable::getSerializedName, TYPES::get);
   }

   public enum Types implements SkullBlock.Type {
      SKELETON("skeleton"),
      WITHER_SKELETON("wither_skeleton"),
      PLAYER("player"),
      ZOMBIE("zombie"),
      CREEPER("creeper"),
      PIGLIN("piglin"),
      DRAGON("dragon");

      private final String name;

      Types(final String name) {
         this.name = name;
         TYPES.put(name, this);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
