package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.ProvidesTrimMaterial;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike {
   public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
      .holderByNameCodec()
      .validate(
         p_361655_ -> p_361655_.is(Items.AIR.builtInRegistryHolder())
            ? DataResult.error(() -> "Item must not be minecraft:air")
            : DataResult.success(p_361655_)
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
   public static final Identifier BASE_ATTACK_DAMAGE_ID = Identifier.withDefaultNamespace("base_attack_damage");
   public static final Identifier BASE_ATTACK_SPEED_ID = Identifier.withDefaultNamespace("base_attack_speed");
   public static final int DEFAULT_MAX_STACK_SIZE = 64;
   public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
   public static final int MAX_BAR_WIDTH = 13;
   protected static final int APPROXIMATELY_INFINITE_USE_DURATION = 72000;
   private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
   private final DataComponentMap components;
   private final @Nullable Item craftingRemainingItem;
   protected final String descriptionId;
   private final FeatureFlagSet requiredFeatures;

   public static int getId(Item p_41394_) {
      return p_41394_ == null ? 0 : BuiltInRegistries.ITEM.getId(p_41394_);
   }

   public static Item byId(int p_41446_) {
      return BuiltInRegistries.ITEM.byId(p_41446_);
   }

   @Deprecated
   public static Item byBlock(Block p_41440_) {
      return BY_BLOCK.getOrDefault(p_41440_, Items.AIR);
   }

   public Item(Item.Properties p_41383_) {
      this.descriptionId = p_41383_.effectiveDescriptionId();
      this.components = p_41383_.buildAndValidateComponents(Component.translatable(this.descriptionId), p_41383_.effectiveModel());
      this.craftingRemainingItem = p_41383_.craftingRemainingItem;
      this.requiredFeatures = p_41383_.requiredFeatures;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         String s = this.getClass().getSimpleName();
         if (!s.endsWith("Item")) {
            LOGGER.error("Item classes should end with Item and {} doesn't.", s);
         }
      }
   }

   @Deprecated
   public Holder.Reference<Item> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   public DataComponentMap components() {
      return this.components;
   }

   public int getDefaultMaxStackSize() {
      return this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   public void onUseTick(Level p_41428_, LivingEntity p_41429_, ItemStack p_41430_, int p_41431_) {
   }

   public void onDestroyed(ItemEntity p_150887_) {
   }

   public boolean canDestroyBlock(ItemStack p_393250_, BlockState p_391392_, Level p_391788_, BlockPos p_395670_, LivingEntity p_394644_) {
      Tool tool = p_393250_.get(DataComponents.TOOL);
      return tool != null && !tool.canDestroyBlocksInCreative() ? !(p_394644_ instanceof Player player && player.getAbilities().instabuild) : true;
   }

   @Override
   public Item asItem() {
      return this;
   }

   public InteractionResult useOn(UseOnContext p_41427_) {
      return InteractionResult.PASS;
   }

   public float getDestroySpeed(ItemStack p_41425_, BlockState p_41426_) {
      Tool tool = p_41425_.get(DataComponents.TOOL);
      return tool != null ? tool.getMiningSpeed(p_41426_) : 1.0F;
   }

   public InteractionResult use(Level p_41432_, Player p_41433_, InteractionHand p_41434_) {
      ItemStack itemstack = p_41433_.getItemInHand(p_41434_);
      Consumable consumable = itemstack.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.startConsuming(p_41433_, itemstack, p_41434_);
      } else {
         Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.swappable()) {
            return equippable.swapWithEquipmentSlot(itemstack, p_41433_);
         } else if (itemstack.has(DataComponents.BLOCKS_ATTACKS)) {
            p_41433_.startUsingItem(p_41434_);
            return InteractionResult.CONSUME;
         } else {
            KineticWeapon kineticweapon = itemstack.get(DataComponents.KINETIC_WEAPON);
            if (kineticweapon != null) {
               p_41433_.startUsingItem(p_41434_);
               kineticweapon.makeSound(p_41433_);
               return InteractionResult.CONSUME;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack p_41409_, Level p_41410_, LivingEntity p_41411_) {
      Consumable consumable = p_41409_.get(DataComponents.CONSUMABLE);
      return consumable != null ? consumable.onConsume(p_41410_, p_41411_, p_41409_) : p_41409_;
   }

   public boolean isBarVisible(ItemStack p_150899_) {
      return p_150899_.isDamaged();
   }

   public int getBarWidth(ItemStack p_150900_) {
      return Mth.clamp(Math.round(13.0F - p_150900_.getDamageValue() * 13.0F / p_150900_.getMaxDamage()), 0, 13);
   }

   public int getBarColor(ItemStack p_150901_) {
      int i = p_150901_.getMaxDamage();
      float f = Math.max(0.0F, ((float)i - p_150901_.getDamageValue()) / i);
      return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
   }

   public boolean overrideStackedOnOther(ItemStack p_150888_, Slot p_150889_, ClickAction p_150890_, Player p_150891_) {
      return false;
   }

   public boolean overrideOtherStackedOnMe(
      ItemStack p_150892_, ItemStack p_150893_, Slot p_150894_, ClickAction p_150895_, Player p_150896_, SlotAccess p_150897_
   ) {
      return false;
   }

   public float getAttackDamageBonus(Entity p_345227_, float p_327880_, DamageSource p_342960_) {
      return 0.0F;
   }

   @Deprecated
   public @Nullable DamageSource getItemDamageSource(LivingEntity p_363041_) {
      return null;
   }

   public void hurtEnemy(ItemStack p_41395_, LivingEntity p_41396_, LivingEntity p_41397_) {
   }

   public void postHurtEnemy(ItemStack p_343373_, LivingEntity p_342300_, LivingEntity p_344220_) {
   }

   public boolean mineBlock(ItemStack p_41416_, Level p_41417_, BlockState p_41418_, BlockPos p_41419_, LivingEntity p_41420_) {
      Tool tool = p_41416_.get(DataComponents.TOOL);
      if (tool == null) {
         return false;
      }

      if (!p_41417_.isClientSide() && p_41418_.getDestroySpeed(p_41417_, p_41419_) != 0.0F && tool.damagePerBlock() > 0) {
         p_41416_.hurtAndBreak(tool.damagePerBlock(), p_41420_, EquipmentSlot.MAINHAND);
      }

      return true;
   }

   public boolean isCorrectToolForDrops(ItemStack p_332232_, BlockState p_41450_) {
      Tool tool = p_332232_.get(DataComponents.TOOL);
      return tool != null && tool.isCorrectForDrops(p_41450_);
   }

   public InteractionResult interactLivingEntity(ItemStack p_41398_, Player p_41399_, LivingEntity p_41400_, InteractionHand p_41401_) {
      return InteractionResult.PASS;
   }

   @Override
   public String toString() {
      return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
   }

   public final ItemStack getCraftingRemainder() {
      return this.craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(this.craftingRemainingItem);
   }

   public void inventoryTick(ItemStack p_41404_, ServerLevel p_393153_, Entity p_41406_, @Nullable EquipmentSlot p_396194_) {
   }

   public void onCraftedBy(ItemStack p_41447_, Player p_41449_) {
      this.onCraftedPostProcess(p_41447_, p_41449_.level());
   }

   public void onCraftedPostProcess(ItemStack p_312780_, Level p_312645_) {
   }

   public ItemUseAnimation getUseAnimation(ItemStack p_41452_) {
      Consumable consumable = p_41452_.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.animation();
      } else if (p_41452_.has(DataComponents.BLOCKS_ATTACKS)) {
         return ItemUseAnimation.BLOCK;
      } else {
         return p_41452_.has(DataComponents.KINETIC_WEAPON) ? ItemUseAnimation.SPEAR : ItemUseAnimation.NONE;
      }
   }

   public int getUseDuration(ItemStack p_41454_, LivingEntity p_342054_) {
      Consumable consumable = p_41454_.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.consumeTicks();
      } else {
         return !p_41454_.has(DataComponents.BLOCKS_ATTACKS) && !p_41454_.has(DataComponents.KINETIC_WEAPON) ? 0 : 72000;
      }
   }

   public boolean releaseUsing(ItemStack p_41412_, Level p_41413_, LivingEntity p_41414_, int p_41415_) {
      return false;
   }

   @Deprecated
   public void appendHoverText(ItemStack p_41421_, Item.TooltipContext p_333372_, TooltipDisplay p_396484_, Consumer<Component> p_392123_, TooltipFlag p_41424_) {
   }

   public Optional<TooltipComponent> getTooltipImage(ItemStack p_150902_) {
      return Optional.empty();
   }

   @VisibleForTesting
   public final String getDescriptionId() {
      return this.descriptionId;
   }

   public final Component getName() {
      return this.components.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }

   public Component getName(ItemStack p_41458_) {
      return p_41458_.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }

   public boolean isFoil(ItemStack p_41453_) {
      return p_41453_.isEnchanted();
   }

   protected static BlockHitResult getPlayerPOVHitResult(Level p_41436_, Player p_41437_, ClipContext.Fluid p_41438_) {
      Vec3 vec3 = p_41437_.getEyePosition();
      Vec3 vec31 = vec3.add(p_41437_.calculateViewVector(p_41437_.getXRot(), p_41437_.getYRot()).scale(p_41437_.blockInteractionRange()));
      return p_41436_.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, p_41438_, p_41437_));
   }

   public boolean useOnRelease(ItemStack p_41464_) {
      return false;
   }

   public ItemStack getDefaultInstance() {
      return new ItemStack(this);
   }

   public boolean canFitInsideContainerItems() {
      return true;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   public boolean shouldPrintOpWarning(ItemStack p_377756_, @Nullable Player p_376409_) {
      return false;
   }

   public static class Properties {
      private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = p_449793_ -> Util.makeDescriptionId("block", p_449793_.identifier());
      private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = p_449792_ -> Util.makeDescriptionId("item", p_449792_.identifier());
      private final DataComponentMap.Builder components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
      @Nullable Item craftingRemainingItem;
      FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
      private @Nullable ResourceKey<Item> id;
      private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
      private final DependantName<Item, Identifier> model = ResourceKey::identifier;

      public Item.Properties food(FoodProperties p_41490_) {
         return this.food(p_41490_, Consumables.DEFAULT_FOOD);
      }

      public Item.Properties food(FoodProperties p_361365_, Consumable p_362417_) {
         return this.component(DataComponents.FOOD, p_361365_).component(DataComponents.CONSUMABLE, p_362417_);
      }

      public Item.Properties usingConvertsTo(Item p_369209_) {
         return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStack(p_369209_)));
      }

      public Item.Properties useCooldown(float p_365459_) {
         return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(p_365459_));
      }

      public Item.Properties stacksTo(int p_41488_) {
         return this.component(DataComponents.MAX_STACK_SIZE, p_41488_);
      }

      public Item.Properties durability(int p_41504_) {
         this.component(DataComponents.MAX_DAMAGE, p_41504_);
         this.component(DataComponents.MAX_STACK_SIZE, 1);
         this.component(DataComponents.DAMAGE, 0);
         return this;
      }

      public Item.Properties craftRemainder(Item p_41496_) {
         this.craftingRemainingItem = p_41496_;
         return this;
      }

      public Item.Properties rarity(Rarity p_41498_) {
         return this.component(DataComponents.RARITY, p_41498_);
      }

      public Item.Properties fireResistant() {
         return this.component(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
      }

      public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> p_342377_) {
         return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(p_342377_)));
      }

      public Item.Properties enchantable(int p_362160_) {
         return this.component(DataComponents.ENCHANTABLE, new Enchantable(p_362160_));
      }

      public Item.Properties repairable(Item p_367070_) {
         return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(p_367070_.builtInRegistryHolder())));
      }

      public Item.Properties repairable(TagKey<Item> p_367486_) {
         HolderGetter<Item> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
         return this.component(DataComponents.REPAIRABLE, new Repairable(holdergetter.getOrThrow(p_367486_)));
      }

      public Item.Properties equippable(EquipmentSlot p_367739_) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(p_367739_).build());
      }

      public Item.Properties equippableUnswappable(EquipmentSlot p_368340_) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(p_368340_).setSwappable(false).build());
      }

      public Item.Properties tool(ToolMaterial p_395599_, TagKey<Block> p_391394_, float p_394093_, float p_397101_, float p_392878_) {
         return p_395599_.applyToolProperties(this, p_391394_, p_394093_, p_397101_, p_392878_);
      }

      public Item.Properties pickaxe(ToolMaterial p_396558_, float p_394621_, float p_395313_) {
         return this.tool(p_396558_, BlockTags.MINEABLE_WITH_PICKAXE, p_394621_, p_395313_, 0.0F);
      }

      public Item.Properties axe(ToolMaterial p_395174_, float p_395904_, float p_392981_) {
         return this.tool(p_395174_, BlockTags.MINEABLE_WITH_AXE, p_395904_, p_392981_, 5.0F);
      }

      public Item.Properties hoe(ToolMaterial p_392061_, float p_397555_, float p_394584_) {
         return this.tool(p_392061_, BlockTags.MINEABLE_WITH_HOE, p_397555_, p_394584_, 0.0F);
      }

      public Item.Properties shovel(ToolMaterial p_391177_, float p_395706_, float p_394086_) {
         return this.tool(p_391177_, BlockTags.MINEABLE_WITH_SHOVEL, p_395706_, p_394086_, 0.0F);
      }

      public Item.Properties sword(ToolMaterial p_395594_, float p_395179_, float p_392972_) {
         return p_395594_.applySwordProperties(this, p_395179_, p_392972_);
      }

      public Item.Properties spear(
         ToolMaterial p_450684_,
         float p_460317_,
         float p_452091_,
         float p_453229_,
         float p_456711_,
         float p_456477_,
         float p_460664_,
         float p_450836_,
         float p_454185_,
         float p_458566_
      ) {
         return this.durability(p_450684_.durability())
            .repairable(p_450684_.repairItems())
            .enchantable(p_450684_.enchantmentValue())
            .component(DataComponents.DAMAGE_TYPE, new EitherHolder<>(DamageTypes.SPEAR))
            .component(
               DataComponents.KINETIC_WEAPON,
               new KineticWeapon(
                  10,
                  (int)(p_453229_ * 20.0F),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(p_456711_ * 20.0F), p_456477_),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(p_460664_ * 20.0F), p_450836_),
                  KineticWeapon.Condition.ofRelativeSpeed((int)(p_454185_ * 20.0F), p_458566_),
                  0.38F,
                  p_452091_,
                  Optional.of(p_450684_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                  Optional.of(p_450684_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(
               DataComponents.PIERCING_WEAPON,
               new PiercingWeapon(
                  true,
                  false,
                  Optional.of(p_450684_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_ATTACK : SoundEvents.SPEAR_ATTACK),
                  Optional.of(p_450684_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
            .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F)
            .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int)(p_460317_ * 20.0F)))
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0F + p_450684_.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 1.0F / p_460317_ - 4.0, AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
            .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F))
            .component(DataComponents.WEAPON, new Weapon(1));
      }

      public Item.Properties spawnEgg(EntityType<?> p_422939_) {
         return this.component(DataComponents.ENTITY_DATA, TypedEntityData.of(p_422939_, new CompoundTag()));
      }

      public Item.Properties humanoidArmor(ArmorMaterial p_392612_, ArmorType p_396266_) {
         return this.durability(p_396266_.getDurability(p_392612_.durability()))
            .attributes(p_392612_.createAttributes(p_396266_))
            .enchantable(p_392612_.enchantmentValue())
            .component(
               DataComponents.EQUIPPABLE, Equippable.builder(p_396266_.getSlot()).setEquipSound(p_392612_.equipSound()).setAsset(p_392612_.assetId()).build()
            )
            .repairable(p_392612_.repairIngredient());
      }

      public Item.Properties wolfArmor(ArmorMaterial p_397320_) {
         return this.durability(ArmorType.BODY.getDurability(p_397320_.durability()))
            .attributes(p_397320_.createAttributes(ArmorType.BODY))
            .repairable(p_397320_.repairIngredient())
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(p_397320_.equipSound())
                  .setAsset(p_397320_.assetId())
                  .setAllowedEntities(HolderSet.direct(EntityType.WOLF.builtInRegistryHolder()))
                  .setCanBeSheared(true)
                  .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_UNEQUIP_WOLF))
                  .build()
            )
            .component(DataComponents.BREAK_SOUND, SoundEvents.WOLF_ARMOR_BREAK)
            .stacksTo(1);
      }

      public Item.Properties horseArmor(ArmorMaterial p_392823_) {
         HolderGetter<EntityType<?>> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(p_392823_.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.HORSE_ARMOR)
                  .setAsset(p_392823_.assetId())
                  .setAllowedEntities(holdergetter.getOrThrow(EntityTypeTags.CAN_WEAR_HORSE_ARMOR))
                  .setDamageOnHurt(false)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties nautilusArmor(ArmorMaterial p_460572_) {
         HolderGetter<EntityType<?>> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(p_460572_.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.ARMOR_EQUIP_NAUTILUS)
                  .setAsset(p_460572_.assetId())
                  .setAllowedEntities(holdergetter.getOrThrow(EntityTypeTags.CAN_WEAR_NAUTILUS_ARMOR))
                  .setDamageOnHurt(false)
                  .setEquipOnInteract(true)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.ARMOR_UNEQUIP_NAUTILUS)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties trimMaterial(ResourceKey<TrimMaterial> p_394822_) {
         return this.component(DataComponents.PROVIDES_TRIM_MATERIAL, new ProvidesTrimMaterial(p_394822_));
      }

      public Item.Properties requiredFeatures(FeatureFlag... p_250948_) {
         this.requiredFeatures = FeatureFlags.REGISTRY.subset(p_250948_);
         return this;
      }

      public Item.Properties setId(ResourceKey<Item> p_365136_) {
         this.id = p_365136_;
         return this;
      }

      public Item.Properties overrideDescription(String p_363112_) {
         this.descriptionId = DependantName.fixed(p_363112_);
         return this;
      }

      public Item.Properties useBlockDescriptionPrefix() {
         this.descriptionId = BLOCK_DESCRIPTION_ID;
         return this;
      }

      public Item.Properties useItemDescriptionPrefix() {
         this.descriptionId = ITEM_DESCRIPTION_ID;
         return this;
      }

      protected String effectiveDescriptionId() {
         return this.descriptionId.get(Objects.requireNonNull(this.id, "Item id not set"));
      }

      public Identifier effectiveModel() {
         return this.model.get(Objects.requireNonNull(this.id, "Item id not set"));
      }

      public <T> Item.Properties component(DataComponentType<T> p_333852_, T p_330859_) {
         this.components.set(p_333852_, p_330859_);
         return this;
      }

      public Item.Properties attributes(ItemAttributeModifiers p_330293_) {
         return this.component(DataComponents.ATTRIBUTE_MODIFIERS, p_330293_);
      }

      DataComponentMap buildAndValidateComponents(Component p_361375_, Identifier p_454244_) {
         DataComponentMap datacomponentmap = this.components.set(DataComponents.ITEM_NAME, p_361375_).set(DataComponents.ITEM_MODEL, p_454244_).build();
         if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
            throw new IllegalStateException("Item cannot have both durability and be stackable");
         } else {
            return datacomponentmap;
         }
      }
   }

   public interface TooltipContext {
      Item.TooltipContext EMPTY = new Item.TooltipContext() {
         @Override
         public HolderLookup.@Nullable Provider registries() {
            return null;
         }

         @Override
         public float tickRate() {
            return 20.0F;
         }

         @Override
         public @Nullable MapItemSavedData mapData(MapId p_334227_) {
            return null;
         }

         @Override
         public boolean isPeaceful() {
            return false;
         }
      };

      HolderLookup.@Nullable Provider registries();

      float tickRate();

      @Nullable MapItemSavedData mapData(MapId var1);

      boolean isPeaceful();

      static Item.TooltipContext of(final @Nullable Level p_332083_) {
         return p_332083_ == null ? EMPTY : new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return p_332083_.registryAccess();
            }

            @Override
            public float tickRate() {
               return p_332083_.tickRateManager().tickrate();
            }

            @Override
            public MapItemSavedData mapData(MapId p_330171_) {
               return p_332083_.getMapData(p_330171_);
            }

            @Override
            public boolean isPeaceful() {
               return p_332083_.getDifficulty() == Difficulty.PEACEFUL;
            }
         };
      }

      static Item.TooltipContext of(final HolderLookup.Provider p_335652_) {
         return new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return p_335652_;
            }

            @Override
            public float tickRate() {
               return 20.0F;
            }

            @Override
            public @Nullable MapItemSavedData mapData(MapId p_332386_) {
               return null;
            }

            @Override
            public boolean isPeaceful() {
               return false;
            }
         };
      }
   }
}
