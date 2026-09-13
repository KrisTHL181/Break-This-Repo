package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
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
import org.jspecify.annotations.Nullable;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {
   private static final int MAX_LEVELS = 4;
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
   List<BeaconBeamOwner.Section> beamSections = new ArrayList<>();
   private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<>();
   int levels;
   private int lastCheckY;
   @Nullable Holder<MobEffect> primaryPower;
   @Nullable Holder<MobEffect> secondaryPower;
   private @Nullable Component name;
   private LockCode lockKey = LockCode.NO_LOCK;
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int p_58711_) {
         return switch (p_58711_) {
            case 0 -> BeaconBlockEntity.this.levels;
            case 1 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.primaryPower);
            case 2 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.secondaryPower);
            default -> 0;
         };
      }

      @Override
      public void set(int p_58713_, int p_58714_) {
         switch (p_58713_) {
            case 0:
               BeaconBlockEntity.this.levels = p_58714_;
               break;
            case 1:
               if (!BeaconBlockEntity.this.level.isClientSide() && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                  BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
               }

               BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
               break;
            case 2:
               BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
         }
      }

      @Override
      public int getCount() {
         return 3;
      }
   };

   static @Nullable Holder<MobEffect> filterEffect(@Nullable Holder<MobEffect> p_330198_) {
      return VALID_EFFECTS.contains(p_330198_) ? p_330198_ : null;
   }

   public BeaconBlockEntity(BlockPos p_155088_, BlockState p_155089_) {
      super(BlockEntityType.BEACON, p_155088_, p_155089_);
   }

   public static void tick(Level p_155108_, BlockPos p_155109_, BlockState p_155110_, BeaconBlockEntity p_155111_) {
      int i = p_155109_.getX();
      int j = p_155109_.getY();
      int k = p_155109_.getZ();
      BlockPos blockpos;
      if (p_155111_.lastCheckY < j) {
         blockpos = p_155109_;
         p_155111_.checkingBeamSections = Lists.newArrayList();
         p_155111_.lastCheckY = p_155109_.getY() - 1;
      } else {
         blockpos = new BlockPos(i, p_155111_.lastCheckY + 1, k);
      }

      BeaconBeamOwner.Section beaconbeamowner$section = p_155111_.checkingBeamSections.isEmpty()
         ? null
         : p_155111_.checkingBeamSections.get(p_155111_.checkingBeamSections.size() - 1);
      int l = p_155108_.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

      for (int i1 = 0; i1 < 10 && blockpos.getY() <= l; i1++) {
         BlockState blockstate = p_155108_.getBlockState(blockpos);
         if (blockstate.getBlock() instanceof BeaconBeamBlock beaconbeamblock) {
            int j1 = beaconbeamblock.getColor().getTextureDiffuseColor();
            if (p_155111_.checkingBeamSections.size() <= 1) {
               beaconbeamowner$section = new BeaconBeamOwner.Section(j1);
               p_155111_.checkingBeamSections.add(beaconbeamowner$section);
            } else if (beaconbeamowner$section != null) {
               if (j1 == beaconbeamowner$section.getColor()) {
                  beaconbeamowner$section.increaseHeight();
               } else {
                  beaconbeamowner$section = new BeaconBeamOwner.Section(ARGB.average(beaconbeamowner$section.getColor(), j1));
                  p_155111_.checkingBeamSections.add(beaconbeamowner$section);
               }
            }
         } else {
            if (beaconbeamowner$section == null || blockstate.getLightBlock() >= 15 && !blockstate.is(Blocks.BEDROCK)) {
               p_155111_.checkingBeamSections.clear();
               p_155111_.lastCheckY = l;
               break;
            }

            beaconbeamowner$section.increaseHeight();
         }

         blockpos = blockpos.above();
         p_155111_.lastCheckY++;
      }

      int k1 = p_155111_.levels;
      if (p_155108_.getGameTime() % 80L == 0L) {
         if (!p_155111_.beamSections.isEmpty()) {
            p_155111_.levels = updateBase(p_155108_, i, j, k);
         }

         if (p_155111_.levels > 0 && !p_155111_.beamSections.isEmpty()) {
            applyEffects(p_155108_, p_155109_, p_155111_.levels, p_155111_.primaryPower, p_155111_.secondaryPower);
            playSound(p_155108_, p_155109_, SoundEvents.BEACON_AMBIENT);
         }
      }

      if (p_155111_.lastCheckY >= l) {
         p_155111_.lastCheckY = p_155108_.getMinY() - 1;
         boolean flag = k1 > 0;
         p_155111_.beamSections = p_155111_.checkingBeamSections;
         if (!p_155108_.isClientSide()) {
            boolean flag1 = p_155111_.levels > 0;
            if (!flag && flag1) {
               playSound(p_155108_, p_155109_, SoundEvents.BEACON_ACTIVATE);

               for (ServerPlayer serverplayer : p_155108_.getEntitiesOfClass(ServerPlayer.class, new AABB(i, j, k, i, j - 4, k).inflate(10.0, 5.0, 10.0))) {
                  CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverplayer, p_155111_.levels);
               }
            } else if (flag && !flag1) {
               playSound(p_155108_, p_155109_, SoundEvents.BEACON_DEACTIVATE);
            }
         }
      }
   }

   private static int updateBase(Level p_155093_, int p_155094_, int p_155095_, int p_155096_) {
      int i = 0;

      for (int j = 1; j <= 4; i = j++) {
         int k = p_155095_ - j;
         if (k < p_155093_.getMinY()) {
            break;
         }

         boolean flag = true;

         for (int l = p_155094_ - j; l <= p_155094_ + j && flag; l++) {
            for (int i1 = p_155096_ - j; i1 <= p_155096_ + j; i1++) {
               if (!p_155093_.getBlockState(new BlockPos(l, k, i1)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                  flag = false;
                  break;
               }
            }
         }

         if (!flag) {
            break;
         }
      }

      return i;
   }

   @Override
   public void setRemoved() {
      playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
      super.setRemoved();
   }

   private static void applyEffects(
      Level p_155098_, BlockPos p_155099_, int p_155100_, @Nullable Holder<MobEffect> p_329363_, @Nullable Holder<MobEffect> p_332048_
   ) {
      if (!p_155098_.isClientSide() && p_329363_ != null) {
         double d0 = p_155100_ * 10 + 10;
         int i = 0;
         if (p_155100_ >= 4 && Objects.equals(p_329363_, p_332048_)) {
            i = 1;
         }

         int j = (9 + p_155100_ * 2) * 20;
         AABB aabb = new AABB(p_155099_).inflate(d0).expandTowards(0.0, p_155098_.getHeight(), 0.0);
         List<Player> list = p_155098_.getEntitiesOfClass(Player.class, aabb);

         for (Player player : list) {
            player.addEffect(new MobEffectInstance(p_329363_, j, i, true, true));
         }

         if (p_155100_ >= 4 && !Objects.equals(p_329363_, p_332048_) && p_332048_ != null) {
            for (Player player1 : list) {
               player1.addEffect(new MobEffectInstance(p_332048_, j, 0, true, true));
            }
         }
      }
   }

   public static void playSound(Level p_155104_, BlockPos p_155105_, SoundEvent p_155106_) {
      p_155104_.playSound(null, p_155105_, p_155106_, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   public List<BeaconBeamOwner.Section> getBeamSections() {
      return (List<BeaconBeamOwner.Section>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_333588_) {
      return this.saveCustomOnly(p_333588_);
   }

   private static void storeEffect(ValueOutput p_407172_, String p_297212_, @Nullable Holder<MobEffect> p_329692_) {
      if (p_329692_ != null) {
         p_329692_.unwrapKey().ifPresent(p_449910_ -> p_407172_.putString(p_297212_, p_449910_.identifier().toString()));
      }
   }

   private static @Nullable Holder<MobEffect> loadEffect(ValueInput p_406136_, String p_301201_) {
      return p_406136_.<Holder<MobEffect>>read(p_301201_, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
   }

   @Override
   protected void loadAdditional(ValueInput p_410687_) {
      super.loadAdditional(p_410687_);
      this.primaryPower = loadEffect(p_410687_, "primary_effect");
      this.secondaryPower = loadEffect(p_410687_, "secondary_effect");
      this.name = parseCustomNameSafe(p_410687_, "CustomName");
      this.lockKey = LockCode.fromTag(p_410687_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_410479_) {
      super.saveAdditional(p_410479_);
      storeEffect(p_410479_, "primary_effect", this.primaryPower);
      storeEffect(p_410479_, "secondary_effect", this.secondaryPower);
      p_410479_.putInt("Levels", this.levels);
      p_410479_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
      this.lockKey.addToTag(p_410479_);
   }

   public void setCustomName(@Nullable Component p_58682_) {
      this.name = p_58682_;
   }

   @Override
   public @Nullable Component getCustomName() {
      return this.name;
   }

   @Override
   public @Nullable AbstractContainerMenu createMenu(int p_58696_, Inventory p_58697_, Player p_58698_) {
      if (this.lockKey.canUnlock(p_58698_)) {
         return new BeaconMenu(p_58696_, p_58697_, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
      }

      BaseContainerBlockEntity.sendChestLockedNotifications(this.getBlockPos().getCenter(), p_58698_, this.getDisplayName());
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
   protected void applyImplicitComponents(DataComponentGetter p_396481_) {
      super.applyImplicitComponents(p_396481_);
      this.name = p_396481_.get(DataComponents.CUSTOM_NAME);
      this.lockKey = p_396481_.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_329382_) {
      super.collectImplicitComponents(p_329382_);
      p_329382_.set(DataComponents.CUSTOM_NAME, this.name);
      if (!this.lockKey.equals(LockCode.NO_LOCK)) {
         p_329382_.set(DataComponents.LOCK, this.lockKey);
      }
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_408927_) {
      p_408927_.discard("CustomName");
      p_408927_.discard("lock");
   }

   @Override
   public void setLevel(Level p_155091_) {
      super.setLevel(p_155091_);
      this.lastCheckY = p_155091_.getMinY() - 1;
   }
}
