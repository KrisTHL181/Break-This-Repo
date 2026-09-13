package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BrushableBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String LOOT_TABLE_TAG = "LootTable";
   private static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
   private static final String HIT_DIRECTION_TAG = "hit_direction";
   private static final String ITEM_TAG = "item";
   private static final int BRUSH_COOLDOWN_TICKS = 10;
   private static final int BRUSH_RESET_TICKS = 40;
   private static final int REQUIRED_BRUSHES_TO_BREAK = 10;
   private int brushCount;
   private long brushCountResetsAtTick;
   private long coolDownEndsAtTick;
   private ItemStack item = ItemStack.EMPTY;
   private @Nullable Direction hitDirection;
   private @Nullable ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   public BrushableBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.BRUSHABLE_BLOCK, worldPosition, blockState);
   }

   public boolean brush(final long gameTime, final ServerLevel level, final LivingEntity user, final Direction direction, final ItemStack brush) {
      if (this.hitDirection == null) {
         this.hitDirection = direction;
      }

      this.brushCountResetsAtTick = gameTime + 40L;
      if (gameTime < this.coolDownEndsAtTick) {
         return false;
      }

      this.coolDownEndsAtTick = gameTime + 10L;
      this.unpackLootTable(level, user, brush);
      int previousCompletionState = this.getCompletionState();
      if (++this.brushCount >= 10) {
         this.brushingCompleted(level, user, brush);
         return true;
      }

      level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
      int completionState = this.getCompletionState();
      if (previousCompletionState != completionState) {
         BlockState previousState = this.getBlockState();
         BlockState state = previousState.setValue(BlockStateProperties.DUSTED, completionState);
         level.setBlockAndUpdate(this.getBlockPos(), state);
      }

      return false;
   }

   private void unpackLootTable(final ServerLevel level, final LivingEntity user, final ItemInstance brush) {
      if (this.lootTable != null) {
         LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(this.lootTable);
         if (user instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, this.lootTable);
         }

         LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
            .withLuck(user.getLuck())
            .withParameter(LootContextParams.THIS_ENTITY, user)
            .withParameter(LootContextParams.TOOL, brush)
            .create(LootContextParamSets.ARCHAEOLOGY);
         ObjectArrayList<ItemStack> loot = lootTable.getRandomItems(params, this.lootTableSeed);

         this.item = switch (loot.size()) {
            case 0 -> ItemStack.EMPTY;
            case 1 -> (ItemStack)loot.getFirst();
            default -> {
               LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", this.lootTable.identifier(), loot.size());
               yield (ItemStack)loot.getFirst();
            }
         };
         this.lootTable = null;
         this.setChanged();
      }
   }

   private void brushingCompleted(final ServerLevel level, final LivingEntity user, final ItemStack brush) {
      this.dropContent(level, user, brush);
      BlockState blockState = this.getBlockState();
      level.levelEvent(3008, this.getBlockPos(), Block.getId(blockState));
      Block turnsInto;
      if (this.getBlockState().getBlock() instanceof BrushableBlock brushableBlock) {
         turnsInto = brushableBlock.getTurnsInto();
      } else {
         turnsInto = Blocks.AIR;
      }

      level.setBlockAndUpdate(this.worldPosition, turnsInto.defaultBlockState());
   }

   private void dropContent(final ServerLevel level, final LivingEntity user, final ItemStack brush) {
      this.unpackLootTable(level, user, brush);
      if (!this.item.isEmpty()) {
         double size = EntityTypes.ITEM.getWidth();
         double centerRange = 1.0 - size;
         double halfSize = size / 2.0;
         Direction dropDirection = Objects.requireNonNullElse(this.hitDirection, Direction.UP);
         BlockPos dropPos = this.worldPosition.relative(dropDirection, 1);
         double xo = dropPos.getX() + 0.5 * centerRange + halfSize;
         double yo = dropPos.getY() + 0.5 + EntityTypes.ITEM.getHeight() / 2.0F;
         double zo = dropPos.getZ() + 0.5 * centerRange + halfSize;
         ItemEntity entity = new ItemEntity(level, xo, yo, zo, this.item.split(level.getRandom().nextInt(21) + 10));
         entity.setDeltaMovement(Vec3.ZERO);
         level.addFreshEntity(entity);
         this.item = ItemStack.EMPTY;
      }
   }

   public void checkReset(final ServerLevel level) {
      if (this.brushCount != 0 && level.getGameTime() >= this.brushCountResetsAtTick) {
         int previousCompletionState = this.getCompletionState();
         this.brushCount = Math.max(0, this.brushCount - 2);
         int completionState = this.getCompletionState();
         if (previousCompletionState != completionState) {
            level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.DUSTED, completionState));
         }

         int retractionSpeed = 4;
         this.brushCountResetsAtTick = level.getGameTime() + 4L;
      }

      if (this.brushCount == 0) {
         this.hitDirection = null;
         this.brushCountResetsAtTick = 0L;
         this.coolDownEndsAtTick = 0L;
      } else {
         level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
      }
   }

   private boolean tryLoadLootTable(final ValueInput input) {
      this.lootTable = input.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.lootTableSeed = input.getLongOr("LootTableSeed", 0L);
      return this.lootTable != null;
   }

   private boolean trySaveLootTable(final ValueOutput base) {
      if (this.lootTable == null) {
         return false;
      }

      base.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
      if (this.lootTableSeed != 0L) {
         base.putLong("LootTableSeed", this.lootTableSeed);
      }

      return true;
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      CompoundTag tag = super.getUpdateTag(registries);
      tag.storeNullable("hit_direction", Direction.LEGACY_ID_CODEC, this.hitDirection);
      if (!this.item.isEmpty()) {
         RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
         tag.store("item", ItemStack.CODEC, ops, this.item);
      }

      return tag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      if (!this.tryLoadLootTable(input)) {
         this.item = input.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      } else {
         this.item = ItemStack.EMPTY;
      }

      this.hitDirection = input.<Direction>read("hit_direction", Direction.LEGACY_ID_CODEC).orElse(null);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      if (!this.trySaveLootTable(output) && !this.item.isEmpty()) {
         output.store("item", ItemStack.CODEC, this.item);
      }
   }

   public void setLootTable(final ResourceKey<LootTable> lootTable, final long seed) {
      this.lootTable = lootTable;
      this.lootTableSeed = seed;
   }

   private int getCompletionState() {
      if (this.brushCount == 0) {
         return 0;
      } else if (this.brushCount < 3) {
         return 1;
      } else {
         return this.brushCount < 6 ? 2 : 3;
      }
   }

   public @Nullable Direction getHitDirection() {
      return this.hitDirection;
   }

   public ItemStack getItem() {
      return this.item;
   }
}
