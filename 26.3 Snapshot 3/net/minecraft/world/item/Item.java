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
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponentInitializers;
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
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.item.component.BlockTransformerMappings;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
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
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Item implements ItemLike, FeatureElement {
   public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
      .holderByNameCodec()
      .validate(item -> item.is(Items.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success(item));
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
   public static final Codec<Holder<Item>> CODEC_WITH_BOUND_COMPONENTS = CODEC.validate(
      item -> !item.areComponentsBound()
         ? DataResult.error(() -> "Item " + item.getRegisteredName() + " does not have components yet")
         : DataResult.success(item)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
   public static final Identifier BASE_ATTACK_DAMAGE_ID = Identifier.withDefaultNamespace("base_attack_damage");
   public static final Identifier BASE_ATTACK_SPEED_ID = Identifier.withDefaultNamespace("base_attack_speed");
   public static final int DEFAULT_MAX_STACK_SIZE = 64;
   public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
   public static final int MAX_BAR_WIDTH = 13;
   protected static final int APPROXIMATELY_INFINITE_USE_DURATION = 72000;
   private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
   private final @Nullable ItemStackTemplate craftingRemainingItem;
   protected final String descriptionId;
   private final FeatureFlagSet requiredFeatures;

   public static int getId(final Item item) {
      return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
   }

   public static Item byId(final int id) {
      return BuiltInRegistries.ITEM.byId(id);
   }

   @Deprecated
   public static Item byBlock(final Block block) {
      return BY_BLOCK.getOrDefault(block, Items.AIR);
   }

   public Item(final Item.Properties properties) {
      this.descriptionId = properties.effectiveDescriptionId();
      DataComponentInitializers.Initializer<Item> componentInitializer = properties.finalizeInitializer(
         Component.translatable(this.descriptionId), properties.effectiveModel()
      );
      BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.add(properties.itemIdOrThrow(), componentInitializer);
      this.craftingRemainingItem = properties.craftingRemainingItem;
      this.requiredFeatures = properties.requiredFeatures;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         String className = this.getClass().getSimpleName();
         if (!className.endsWith("Item")) {
            LOGGER.error("Item classes should end with Item and {} doesn't.", className);
         }
      }
   }

   @Deprecated
   public Holder.Reference<Item> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   public DataComponentMap components() {
      return this.builtInRegistryHolder.components();
   }

   public int getDefaultMaxStackSize() {
      return this.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack itemStack, final int ticksRemaining) {
   }

   public void onDestroyed(final ItemEntity itemEntity) {
   }

   public boolean canDestroyBlock(final ItemStack itemStack, final BlockState state, final Level level, final BlockPos pos, final LivingEntity user) {
      Tool tool = itemStack.get(DataComponents.TOOL);
      return tool != null && !tool.canDestroyBlocksInCreative() ? !(user instanceof Player player && player.getAbilities().instabuild) : true;
   }

   @Override
   public Item asItem() {
      return this;
   }

   public InteractionResult useOn(final UseOnContext context) {
      ItemStack stack = context.getItemInHand();
      BlockTransformer blockTransformer = stack.get(DataComponents.BLOCK_TRANSFORMER);
      return blockTransformer != null ? blockTransformer.transformBlock(context) : InteractionResult.PASS;
   }

   public float getDestroySpeed(final ItemStack itemStack, final BlockState state) {
      Tool tool = itemStack.get(DataComponents.TOOL);
      return tool != null ? tool.getMiningSpeed(state) : 1.0F;
   }

   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      Consumable consumable = stack.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.startConsuming(player, stack, hand);
      } else {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.swappable()) {
            return equippable.swapWithEquipmentSlot(stack, player);
         } else if (stack.has(DataComponents.BLOCKS_ATTACKS)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
         } else {
            KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
            if (kineticWeapon != null) {
               player.startUsingItem(hand);
               kineticWeapon.makeSound(player);
               return InteractionResult.CONSUME;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   public ItemStack finishUsingItem(final ItemStack itemStack, final Level level, final LivingEntity entity) {
      Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
      return consumable != null ? consumable.onConsume(level, entity, itemStack) : itemStack;
   }

   public boolean isBarVisible(final ItemStack stack) {
      return stack.isDamaged();
   }

   public int getBarWidth(final ItemStack stack) {
      return Mth.clamp(Math.round(13.0F - stack.getDamageValue() * 13.0F / stack.getMaxDamage()), 0, 13);
   }

   public int getBarColor(final ItemStack stack) {
      int maxDamage = stack.getMaxDamage();
      float healthPercentage = Math.max(0.0F, ((float)maxDamage - stack.getDamageValue()) / maxDamage);
      return Mth.hsvToRgb(healthPercentage / 3.0F, 1.0F, 1.0F);
   }

   public boolean overrideStackedOnOther(final ItemStack self, final Slot slot, final ClickAction clickAction, final Player player) {
      return false;
   }

   public boolean overrideOtherStackedOnMe(
      final ItemStack self, final ItemStack other, final Slot slot, final ClickAction clickAction, final Player player, final SlotAccess carriedItem
   ) {
      return false;
   }

   public float getAttackDamageBonus(final Entity victim, final float damage, final DamageSource damageSource) {
      return 0.0F;
   }

   @Deprecated
   public @Nullable DamageSource getItemDamageSource(final LivingEntity attacker) {
      return null;
   }

   public void hurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
   }

   public void postHurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
   }

   public boolean mineBlock(final ItemStack itemStack, final Level level, final BlockState state, final BlockPos pos, final LivingEntity owner) {
      Tool tool = itemStack.get(DataComponents.TOOL);
      if (tool == null) {
         return false;
      }

      if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && tool.damagePerBlock() > 0) {
         itemStack.hurtAndBreak(tool.damagePerBlock(), owner, EquipmentSlot.MAINHAND);
      }

      return true;
   }

   public boolean isCorrectToolForDrops(final ItemStack itemStack, final BlockState state) {
      Tool tool = itemStack.get(DataComponents.TOOL);
      return tool != null && tool.isCorrectForDrops(state);
   }

   public InteractionResult interactLivingEntity(final ItemStack itemStack, final Player player, final LivingEntity target, final InteractionHand type) {
      return InteractionResult.PASS;
   }

   @Override
   public String toString() {
      return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
   }

   public final @Nullable ItemStackTemplate getCraftingRemainder() {
      return this.craftingRemainingItem;
   }

   public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, final @Nullable EquipmentSlot slot) {
   }

   public void onCraftedBy(final ItemStack itemStack, final Player player) {
      this.onCraftedPostProcess(itemStack, player.level());
   }

   public void onCraftedPostProcess(final ItemStack itemStack, final Level level) {
   }

   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.animation();
      } else if (itemStack.has(DataComponents.BLOCKS_ATTACKS)) {
         return ItemUseAnimation.BLOCK;
      } else {
         return itemStack.has(DataComponents.KINETIC_WEAPON) ? ItemUseAnimation.SPEAR : ItemUseAnimation.NONE;
      }
   }

   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.consumeTicks();
      } else {
         return !itemStack.has(DataComponents.BLOCKS_ATTACKS) && !itemStack.has(DataComponents.KINETIC_WEAPON) ? 0 : 72000;
      }
   }

   public boolean releaseUsing(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime) {
      return false;
   }

   @Deprecated
   public void appendHoverText(
      final ItemStack itemStack,
      final Item.TooltipContext context,
      final TooltipDisplay display,
      final Consumer<Component> builder,
      final TooltipFlag tooltipFlag
   ) {
   }

   public Optional<TooltipComponent> getTooltipImage(final ItemStack itemStack) {
      return Optional.empty();
   }

   @VisibleForTesting
   public final String getDescriptionId() {
      return this.descriptionId;
   }

   public Component getName(final ItemStack itemStack) {
      return itemStack.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }

   public boolean isFoil(final ItemStack itemStack) {
      return itemStack.isEnchanted();
   }

   protected static BlockHitResult getPlayerPOVHitResult(final Level level, final Player player, final ClipContext.Fluid fluid) {
      Vec3 from = player.getEyePosition();
      Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
      return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, fluid, player));
   }

   public boolean useOnRelease(final ItemStack itemStack) {
      return false;
   }

   public ItemStack getDefaultInstance() {
      return new ItemStack(this);
   }

   public boolean canFitInsideContainerItems() {
      return true;
   }

   @Override
   public final FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   public boolean shouldPrintOpWarning(final ItemStack stack, final @Nullable Player player) {
      return false;
   }

   public static class Properties {
      private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = id -> Util.makeDescriptionId("block", id.identifier());
      private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = id -> Util.makeDescriptionId("item", id.identifier());
      private DataComponentInitializers.Initializer<Item> componentInitializer = (builder, var1, var2) -> builder.addAll(DataComponents.COMMON_ITEM_COMPONENTS);
      private @Nullable ItemStackTemplate craftingRemainingItem;
      private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
      private @Nullable ResourceKey<Item> id;
      private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
      private final DependantName<Item, Identifier> model = ResourceKey::identifier;

      public Item.Properties food(final FoodProperties foodProperties) {
         return this.food(foodProperties, Consumables.DEFAULT_FOOD);
      }

      public Item.Properties food(final FoodProperties foodProperties, final Consumable consumable) {
         return this.component(DataComponents.FOOD, foodProperties).component(DataComponents.CONSUMABLE, consumable);
      }

      public Item.Properties usingConvertsTo(final Item item) {
         return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(item)));
      }

      public Item.Properties useCooldown(final float seconds) {
         return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(seconds));
      }

      public Item.Properties stacksTo(final int max) {
         return this.component(DataComponents.MAX_STACK_SIZE, max);
      }

      public Item.Properties durability(final int maxDamage) {
         this.component(DataComponents.MAX_DAMAGE, maxDamage);
         this.component(DataComponents.MAX_STACK_SIZE, 1);
         this.component(DataComponents.DAMAGE, 0);
         return this;
      }

      public Item.Properties craftRemainder(final Item craftingRemainingItem) {
         return this.craftRemainder(new ItemStackTemplate(craftingRemainingItem));
      }

      public Item.Properties craftRemainder(final ItemStackTemplate craftingRemainingItem) {
         this.craftingRemainingItem = craftingRemainingItem;
         return this;
      }

      public Item.Properties rarity(final Rarity rarity) {
         return this.component(DataComponents.RARITY, rarity);
      }

      public Item.Properties fireResistant() {
         return this.delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(DamageTypeTags.IS_FIRE)));
      }

      public Item.Properties jukeboxPlayable(final ResourceKey<JukeboxSong> song) {
         return this.delayedComponent(DataComponents.JUKEBOX_PLAYABLE, context -> new JukeboxPlayable(context.getOrThrow(song)));
      }

      public Item.Properties enchantable(final int value) {
         return this.component(DataComponents.ENCHANTABLE, new Enchantable(value));
      }

      public Item.Properties potPattern(final ResourceKey<DecoratedPotPattern> pattern) {
         return this.delayedComponent(DataComponents.PROVIDES_POTTERY_PATTERN, context -> context.getOrThrow(pattern));
      }

      public Item.Properties repairable(final Item repairItem) {
         return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItem.builtInRegistryHolder())));
      }

      public Item.Properties repairable(final TagKey<Item> repairItems) {
         HolderGetter<Item> registrationLookup = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
         return this.component(DataComponents.REPAIRABLE, new Repairable(registrationLookup.getOrThrow(repairItems)));
      }

      public Item.Properties equippable(final EquipmentSlot slot) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).build());
      }

      public Item.Properties equippableUnswappable(final EquipmentSlot slot) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).setSwappable(false).build());
      }

      public Item.Properties tool(
         final ToolMaterial material,
         final TagKey<Block> minesEfficiently,
         final float attackDamageBaseline,
         final float attackSpeedBaseline,
         final float disableBlockingSeconds
      ) {
         return material.applyToolProperties(this, minesEfficiently, attackDamageBaseline, attackSpeedBaseline, disableBlockingSeconds);
      }

      public Item.Properties pickaxe(final ToolMaterial material, final float attackDamageBaseline, final float attackSpeedBaseline) {
         return this.tool(material, BlockTags.MINEABLE_WITH_PICKAXE, attackDamageBaseline, attackSpeedBaseline, 0.0F);
      }

      public Item.Properties axe(final ToolMaterial material, final float attackDamageBaseline, final float attackSpeedBaseline) {
         return this.tool(material, BlockTags.MINEABLE_WITH_AXE, attackDamageBaseline, attackSpeedBaseline, 5.0F)
            .delayedComponent(DataComponents.BLOCK_TRANSFORMER, var0 -> BlockTransformerMappings.AXE);
      }

      public Item.Properties hoe(final ToolMaterial material, final float attackDamageBaseline, final float attackSpeedBaseline) {
         return this.tool(material, BlockTags.MINEABLE_WITH_HOE, attackDamageBaseline, attackSpeedBaseline, 0.0F)
            .delayedComponent(DataComponents.BLOCK_TRANSFORMER, var0 -> BlockTransformerMappings.HOE);
      }

      public Item.Properties shovel(final ToolMaterial material, final float attackDamageBaseline, final float attackSpeedBaseline) {
         return this.tool(material, BlockTags.MINEABLE_WITH_SHOVEL, attackDamageBaseline, attackSpeedBaseline, 0.0F)
            .delayedComponent(DataComponents.BLOCK_TRANSFORMER, var0 -> BlockTransformerMappings.SHOVEL);
      }

      public Item.Properties sword(final ToolMaterial material, final float attackDamageBaseline, final float attackSpeedBaseline) {
         return material.applySwordProperties(this, attackDamageBaseline, attackSpeedBaseline);
      }

      public Item.Properties spear(
         final ToolMaterial material,
         final float attackDuration,
         final float damageMultiplier,
         final float delay,
         final float dismountTime,
         final float dismountThreshold,
         final float knockbackTime,
         final float knockbackThreshold,
         final float damageTime,
         final float damageThreshold
      ) {
         return this.durability(material.durability())
            .repairable(material.repairItems())
            .enchantable(material.enchantmentValue())
            .delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR)
            .component(
               DataComponents.KINETIC_WEAPON,
               new KineticWeapon(
                  10,
                  (int)(delay * 20.0F),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(dismountTime * 20.0F), dismountThreshold),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(knockbackTime * 20.0F), knockbackThreshold),
                  KineticWeapon.Condition.ofRelativeSpeed((int)(damageTime * 20.0F), damageThreshold),
                  0.38F,
                  damageMultiplier,
                  Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                  Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(
               DataComponents.PIERCING_WEAPON,
               new PiercingWeapon(
                  true,
                  false,
                  Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_ATTACK : SoundEvents.SPEAR_ATTACK),
                  Optional.of(material == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
            .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F)
            .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int)(attackDuration * 20.0F)))
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0F + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 1.0F / attackDuration - 4.0, AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
            .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F))
            .component(DataComponents.WEAPON, new Weapon(1));
      }

      public Item.Properties spawnEgg(final EntityType<?> type) {
         return this.component(DataComponents.ENTITY_DATA, TypedEntityData.of(type, new CompoundTag())).requiredFeatures(type.requiredFeatures());
      }

      public Item.Properties humanoidArmor(final ArmorMaterial material, final ArmorType type) {
         return this.durability(type.getDurability(material.durability()))
            .attributes(material.createAttributes(type))
            .enchantable(material.enchantmentValue())
            .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).build())
            .repairable(material.repairIngredient());
      }

      public Item.Properties wolfArmor(final ArmorMaterial material) {
         return this.durability(ArmorType.BODY.getDurability(material.durability()))
            .attributes(material.createAttributes(ArmorType.BODY))
            .repairable(material.repairIngredient())
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(material.equipSound())
                  .setAsset(material.assetId())
                  .setAllowedEntities(HolderSet.direct(EntityTypes.WOLF.builtInRegistryHolder()))
                  .setCanBeSheared(true)
                  .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_UNEQUIP_WOLF))
                  .build()
            )
            .component(DataComponents.BREAK_SOUND, SoundEvents.WOLF_ARMOR_BREAK)
            .stacksTo(1);
      }

      public Item.Properties horseArmor(final ArmorMaterial material) {
         HolderGetter<EntityType<?>> entityGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(material.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.HORSE_ARMOR)
                  .setAsset(material.assetId())
                  .setAllowedEntities(entityGetter.getOrThrow(EntityTypeTags.CAN_WEAR_HORSE_ARMOR))
                  .setDamageOnHurt(false)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties nautilusArmor(final ArmorMaterial material) {
         HolderGetter<EntityType<?>> entityGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(material.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.ARMOR_EQUIP_NAUTILUS)
                  .setAsset(material.assetId())
                  .setAllowedEntities(entityGetter.getOrThrow(EntityTypeTags.CAN_WEAR_NAUTILUS_ARMOR))
                  .setDamageOnHurt(false)
                  .setEquipOnInteract(true)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.ARMOR_UNEQUIP_NAUTILUS)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties trimMaterial(final ResourceKey<TrimMaterial> material) {
         return this.delayedHolderComponent(DataComponents.PROVIDES_TRIM_MATERIAL, material);
      }

      public Item.Properties requiredFeatures(final FeatureFlag... flags) {
         this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
         return this;
      }

      public Item.Properties requiredFeatures(final FeatureFlagSet flags) {
         if (!FeatureFlags.REGISTRY.isSubset(flags)) {
            throw new IllegalArgumentException("Mismatched flag sets");
         }

         this.requiredFeatures = flags;
         return this;
      }

      public Item.Properties setId(final ResourceKey<Item> id) {
         this.id = id;
         return this;
      }

      public Item.Properties overrideDescription(final String descriptionId) {
         this.descriptionId = DependantName.fixed(descriptionId);
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

      private ResourceKey<Item> itemIdOrThrow() {
         return Objects.requireNonNull(this.id, "Item id not set");
      }

      protected String effectiveDescriptionId() {
         return this.descriptionId.get(this.itemIdOrThrow());
      }

      public Identifier effectiveModel() {
         return this.model.get(this.itemIdOrThrow());
      }

      public <T> Item.Properties component(final DataComponentType<T> type, final T value) {
         this.componentInitializer = this.componentInitializer.add(type, value);
         return this;
      }

      public <T> Item.Properties delayedComponent(final DataComponentType<T> type, final DataComponentInitializers.SingleComponentInitializer<T> initializer) {
         this.componentInitializer = this.componentInitializer.andThen(initializer.asInitializer(type));
         return this;
      }

      public <T> Item.Properties delayedHolderComponent(final DataComponentType<Holder<T>> type, final ResourceKey<T> valueKey) {
         this.componentInitializer = this.componentInitializer.andThen((components, context, var4) -> components.set(type, context.getOrThrow(valueKey)));
         return this;
      }

      public Item.Properties attributes(final ItemAttributeModifiers attributes) {
         return this.component(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
      }

      public Item.Properties compostable(final ResourceKey<NumberProvider> layers) {
         return this.component(DataComponents.COMPOSTABLE, new Compostable(layers));
      }

      private DataComponentInitializers.Initializer<Item> finalizeInitializer(final Component name, final Identifier model) {
         return this.componentInitializer
            .andThen((components, var3, var4) -> components.set(DataComponents.ITEM_NAME, name).set(DataComponents.ITEM_MODEL, model).addValidator(c -> {
               if (c.has(DataComponents.DAMAGE) && c.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
                  throw new IllegalStateException("Item cannot have both durability and be stackable");
               }
            }));
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
         public @Nullable MapItemSavedData mapData(final MapId id) {
            return null;
         }

         @Override
         public boolean isPeaceful() {
            return false;
         }
      };

      HolderLookup.@Nullable Provider registries();

      float tickRate();

      @Nullable MapItemSavedData mapData(MapId id);

      boolean isPeaceful();

      static Item.TooltipContext of(final @Nullable Level level) {
         return level == null ? EMPTY : new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return level.registryAccess();
            }

            @Override
            public float tickRate() {
               return level.tickRateManager().tickrate();
            }

            @Override
            public @Nullable MapItemSavedData mapData(final MapId id) {
               return level.getMapData(id);
            }

            @Override
            public boolean isPeaceful() {
               return level.getDifficulty() == Difficulty.PEACEFUL;
            }
         };
      }

      static Item.TooltipContext of(final HolderLookup.Provider registries) {
         return new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return registries;
            }

            @Override
            public float tickRate() {
               return 20.0F;
            }

            @Override
            public @Nullable MapItemSavedData mapData(final MapId id) {
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
