package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {
   private static final int MAX_LEVELS = 4;
   private static final int LEVELS_NEEDED_FOR_SECONDARY = 4;
   public static final List<List<Holder<MobEffect>>> BEACON_EFFECTS = List.of(
      List.of(MobEffects.SPEED, MobEffects.HASTE),
      List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
      List.of(MobEffects.STRENGTH),
      List.of(MobEffects.REGENERATION)
   );
   private static final Set<Holder<MobEffect>> VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toSet());
   public static final int DATA_LEVELS = 0;
   public static final int DATA_PRIMARY = 1;
   public static final int DATA_SECONDARY = 2;
   public static final int NUM_DATA_VALUES = 3;
   private static final int BLOCKS_CHECK_PER_TICK = 10;
   private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
   private static final String TAG_PRIMARY = "primary_effect";
   private static final String TAG_SECONDARY = "secondary_effect";
   private List<BeaconBeamOwner.Section> beamSections = new ArrayList<>();
   private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<>();
   private int levels;
   private int lastCheckY;
   private @Nullable Holder<MobEffect> primaryPower;
   private @Nullable Holder<MobEffect> secondaryPower;
   private @Nullable Component name;
   private LockCode lockKey = LockCode.NO_LOCK;
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(final int dataId) {
         return switch (dataId) {
            case 0 -> BeaconBlockEntity.this.levels;
            case 1 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.primaryPower);
            case 2 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.secondaryPower);
            default -> 0;
         };
      }

      @Override
      public void set(final int dataId, final int value) {
         switch (dataId) {
            case 0:
               BeaconBlockEntity.this.levels = value;
               break;
            case 1:
               if (!BeaconBlockEntity.this.level.isClientSide() && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                  BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
               }

               BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
               break;
            case 2:
               BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
         }
      }

      @Override
      public int getCount() {
         return 3;
      }
   };

   private static @Nullable Holder<MobEffect> filterEffect(final @Nullable Holder<MobEffect> effect) {
      return VALID_EFFECTS.contains(effect) ? effect : null;
   }

   public BeaconBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.BEACON, worldPosition, blockState);
   }

   public static void tick(final Level level, final BlockPos pos, final BlockState selfState, final BeaconBlockEntity entity) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      BlockPos checkPos;
      if (entity.lastCheckY < y) {
         checkPos = pos;
         entity.checkingBeamSections = Lists.newArrayList();
         entity.lastCheckY = checkPos.getY() - 1;
      } else {
         checkPos = new BlockPos(x, entity.lastCheckY + 1, z);
      }

      BeaconBeamOwner.Section lastBeamSection = entity.checkingBeamSections.isEmpty()
         ? null
         : entity.checkingBeamSections.get(entity.checkingBeamSections.size() - 1);
      int lastSetBlock = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

      for (int i = 0; i < 10 && checkPos.getY() <= lastSetBlock; i++) {
         BlockState state = level.getBlockState(checkPos);
         if (state.getBlock() instanceof BeaconBeamBlock beaconBeamBlock) {
            int color = beaconBeamBlock.getColor().getTextureDiffuseColor();
            if (entity.checkingBeamSections.size() <= 1) {
               lastBeamSection = new BeaconBeamOwner.Section(color);
               entity.checkingBeamSections.add(lastBeamSection);
            } else if (lastBeamSection != null) {
               if (color == lastBeamSection.getColor()) {
                  lastBeamSection.increaseHeight();
               } else {
                  lastBeamSection = new BeaconBeamOwner.Section(ARGB.average(lastBeamSection.getColor(), color));
                  entity.checkingBeamSections.add(lastBeamSection);
               }
            }
         } else {
            if (lastBeamSection == null || state.getLightDampening() >= 15 && !state.is(Blocks.BEDROCK)) {
               entity.checkingBeamSections.clear();
               entity.lastCheckY = lastSetBlock;
               break;
            }

            lastBeamSection.increaseHeight();
         }

         checkPos = checkPos.above();
         entity.lastCheckY++;
      }

      int previousLevels = entity.levels;
      if (level.getGameTime() % 80L == 0L) {
         if (!entity.beamSections.isEmpty()) {
            entity.levels = updateBase(level, x, y, z);
         }

         if (entity.levels > 0 && !entity.beamSections.isEmpty()) {
            applyEffects(level, pos, entity.levels, entity.primaryPower, entity.secondaryPower);
            playSound(level, pos, SoundEvents.BEACON_AMBIENT);
         }
      }

      if (entity.lastCheckY >= lastSetBlock) {
         entity.lastCheckY = level.getMinY() - 1;
         boolean wasActive = previousLevels > 0;
         entity.beamSections = entity.checkingBeamSections;
         if (!level.isClientSide()) {
            boolean isActive = entity.levels > 0;
            if (!wasActive && isActive) {
               playSound(level, pos, SoundEvents.BEACON_ACTIVATE);

               for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(x, y, z, x, y - 4, z).inflate(10.0, 5.0, 10.0))) {
                  CriteriaTriggers.CONSTRUCT_BEACON.trigger(player, entity.levels);
               }
            } else if (wasActive && !isActive) {
               playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
            }
         }
      }
   }

   private static int updateBase(final Level level, final int x, final int y, final int z) {
      int levels = 0;

      for (int step = 1; step <= 4; levels = step++) {
         int ly = y - step;
         if (ly < level.getMinY()) {
            break;
         }

         boolean isOk = true;

         for (int lx = x - step; lx <= x + step && isOk; lx++) {
            for (int lz = z - step; lz <= z + step; lz++) {
               if (!level.getBlockState(new BlockPos(lx, ly, lz)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                  isOk = false;
                  break;
               }
            }
         }

         if (!isOk) {
            break;
         }
      }

      return levels;
   }

   @Override
   public void setRemoved() {
      playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
      super.setRemoved();
   }

   public static boolean validateEffects(final @Nullable Holder<MobEffect> primary, final @Nullable Holder<MobEffect> secondary, final int levels) {
      if (secondary != null && levels < 4) {
         return false;
      } else {
         int primaryLevel = getRequiredLevelsFor(primary);
         int secondaryLevel = getRequiredLevelsFor(secondary);
         if (primaryLevel > levels || secondaryLevel > levels) {
            return false;
         } else {
            return primaryLevel >= 4 ? false : secondaryLevel == 0 || secondaryLevel >= 4 || primary.equals(secondary);
         }
      }
   }

   private static int getRequiredLevelsFor(final @Nullable Holder<MobEffect> effect) {
      if (effect == null) {
         return 0;
      }

      for (int i = 0; i < BEACON_EFFECTS.size(); i++) {
         List<Holder<MobEffect>> effectsForLevel = BEACON_EFFECTS.get(i);
         if (effectsForLevel.contains(effect)) {
            return i + 1;
         }
      }

      return Integer.MAX_VALUE;
   }

   private static void applyEffects(
      final Level level,
      final BlockPos worldPosition,
      final int levels,
      final @Nullable Holder<MobEffect> primaryPower,
      final @Nullable Holder<MobEffect> secondaryPower
   ) {
      if (!level.isClientSide() && primaryPower != null) {
         double range = levels * 10 + 10;
         int baseAmp = 0;
         if (levels >= 4 && Objects.equals(primaryPower, secondaryPower)) {
            baseAmp = 1;
         }

         int durationTicks = (9 + levels * 2) * 20;
         AABB bb = new AABB(worldPosition).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
         List<Player> players = level.getEntitiesOfClass(Player.class, bb);

         for (Player player : players) {
            player.addEffect(new MobEffectInstance(primaryPower, durationTicks, baseAmp, true, true));
         }

         if (levels >= 4 && !Objects.equals(primaryPower, secondaryPower) && secondaryPower != null) {
            for (Player player : players) {
               player.addEffect(new MobEffectInstance(secondaryPower, durationTicks, 0, true, true));
            }
         }
      }
   }

   public static void playSound(final Level level, final BlockPos worldPosition, final SoundEvent event) {
      level.playSound(null, worldPosition, event, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   public List<BeaconBeamOwner.Section> getBeamSections() {
      return (List<BeaconBeamOwner.Section>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   private static void storeEffect(final ValueOutput output, final String field, final @Nullable Holder<MobEffect> effect) {
      if (effect != null) {
         effect.unwrapKey().ifPresent(key -> output.putString(field, key.identifier().toString()));
      }
   }

   private static @Nullable Holder<MobEffect> loadEffect(final ValueInput input, final String field) {
      return input.<Holder<MobEffect>>read(field, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.primaryPower = loadEffect(input, "primary_effect");
      this.secondaryPower = loadEffect(input, "secondary_effect");
      this.name = parseCustomNameSafe(input, "CustomName");
      this.lockKey = LockCode.fromTag(input);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      storeEffect(output, "primary_effect", this.primaryPower);
      storeEffect(output, "secondary_effect", this.secondaryPower);
      output.putInt("Levels", this.levels);
      output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
      this.lockKey.addToTag(output);
   }

   public void setCustomName(final @Nullable Component name) {
      this.name = name;
   }

   @Override
   public @Nullable Component getCustomName() {
      return this.name;
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
      if (this.lockKey.canUnlock(player)) {
         return new BeaconMenu(containerId, inventory, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
      }

      BaseContainerBlockEntity.sendChestLockedNotifications(Vec3.atCenterOf(this.getBlockPos()), player, this.getDisplayName());
      return null;
   }

   @Override
   public Component getDisplayName() {
      return this.getName();
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      super.applyImplicitComponents(components);
      this.name = components.get(DataComponents.CUSTOM_NAME);
      this.lockKey = components.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
   }

   @Override
   protected void collectImplicitComponents(final DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.CUSTOM_NAME, this.name);
      if (!this.lockKey.equals(LockCode.NO_LOCK)) {
         components.set(DataComponents.LOCK, this.lockKey);
      }
   }

   @Override
   public void removeComponentsFromTag(final ValueOutput output) {
      output.discard("CustomName");
      output.discard("lock");
   }

   @Override
   public void setLevel(final Level level) {
      super.setLevel(level);
      this.lastCheckY = level.getMinY() - 1;
   }
}
