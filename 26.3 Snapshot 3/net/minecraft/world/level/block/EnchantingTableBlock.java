package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EnchantingTableBlock extends BaseEntityBlock {
   public static final List<BlockPos> BOOKSHELF_OFFSETS = BlockPos.betweenClosedStream(-2, 0, -2, 2, 1, 2)
      .filter(pos -> Math.abs(pos.getX()) == 2 || Math.abs(pos.getZ()) == 2)
      .map(BlockPos::immutable)
      .toList();
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);

   protected EnchantingTableBlock(final BlockBehaviour.Properties properties) {
      super(properties);
   }

   public static boolean isValidBookShelf(final Level level, final BlockPos pos, final BlockPos offset) {
      return level.getBlockState(pos.offset(offset)).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)
         && level.getBlockState(pos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
   }

   @Override
   protected boolean useShapeForLightOcclusion(final BlockState state) {
      return true;
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      super.animateTick(state, level, pos, random);

      for (BlockPos offset : BOOKSHELF_OFFSETS) {
         if (random.nextInt(16) == 0 && isValidBookShelf(level, pos, offset)) {
            level.addParticle(
               ParticleTypes.ENCHANT,
               pos.getX() + 0.5,
               pos.getY() + 2.0,
               pos.getZ() + 0.5,
               offset.getX() + random.nextFloat() - 0.5,
               offset.getY() - random.nextFloat() - 1.0F,
               offset.getZ() + random.nextFloat() - 0.5
            );
         }
      }
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new EnchantingTableBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return level.isClientSide() ? createTickerHelper(type, BlockEntityTypes.ENCHANTING_TABLE, EnchantingTableBlockEntity::bookAnimationTick) : null;
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!level.isClientSide()) {
         player.openMenu(state.getMenuProvider(level, pos));
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   protected @Nullable MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity enchantingTable) {
         Component title = enchantingTable.getDisplayName();
         return new SimpleMenuProvider(
            (containerId, inventory, player) -> new EnchantmentMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), title
         );
      } else {
         return null;
      }
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }
}
