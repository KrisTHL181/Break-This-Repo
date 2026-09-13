package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.NullOps;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class ItemStack implements DataComponentHolder {
   private static final List<Component> OP_NBT_WARNING = List.of(
      Component.translatable("item.op_warning.line1").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
      Component.translatable("item.op_warning.line2").withStyle(ChatFormatting.RED),
      Component.translatable("item.op_warning.line3").withStyle(ChatFormatting.RED)
   );
   private static final Component UNBREAKABLE_TOOLTIP = Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE);
   private static final Component INTANGIBLE_TOOLTIP = Component.translatable("item.intangible").withStyle(ChatFormatting.GRAY);
   public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive(
      "ItemStack",
      p_390809_ -> RecordCodecBuilder.mapCodec(
         p_359412_ -> p_359412_.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_327171_ -> p_327171_.components.asPatch())
            )
            .apply(p_359412_, ItemStack::new)
      )
   );
   public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
   public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codec.lazyInitialized(
      () -> RecordCodecBuilder.create(
         p_359410_ -> p_359410_.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_327155_ -> p_327155_.components.asPatch())
            )
            .apply(p_359410_, (p_327172_, p_327173_) -> new ItemStack(p_327172_, 1, p_327173_))
      )
   );
   public static final Codec<ItemStack> STRICT_CODEC = CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = SINGLE_ITEM_CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(p_327153_ -> p_327153_.orElse(ItemStack.EMPTY), p_327154_ -> p_327154_.isEmpty() ? Optional.empty() : Optional.of(p_327154_));
   public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = Item.CODEC.xmap(ItemStack::new, ItemStack::getItemHolder);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = createOptionalStreamCodec(DataComponentPatch.STREAM_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_UNTRUSTED_STREAM_CODEC = createOptionalStreamCodec(
      DataComponentPatch.DELIMITED_STREAM_CODEC
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
      public ItemStack decode(RegistryFriendlyByteBuf p_327992_) {
         ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_327992_);
         if (itemstack.isEmpty()) {
            throw new DecoderException("Empty ItemStack not allowed");
         } else {
            return itemstack;
         }
      }

      public void encode(RegistryFriendlyByteBuf p_331904_, ItemStack p_328866_) {
         if (p_328866_.isEmpty()) {
            throw new EncoderException("Empty ItemStack not allowed");
         }

         ItemStack.OPTIONAL_STREAM_CODEC.encode(p_331904_, p_328866_);
      }
   };
   public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(
      ByteBufCodecs.collection(NonNullList::createWithCapacity)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ItemStack EMPTY = new ItemStack((Void)null);
   private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
   private int count;
   private int popTime;
   @Deprecated
   private final @Nullable Item item;
   final PatchedDataComponentMap components;
   private @Nullable Entity entityRepresentation;

   public static DataResult<ItemStack> validateStrict(ItemStack p_332181_) {
      DataResult<Unit> dataresult = validateComponents(p_332181_.getComponents());
      if (dataresult.isError()) {
         return dataresult.map(p_327165_ -> p_332181_);
      } else {
         return p_332181_.getCount() > p_332181_.getMaxStackSize()
            ? DataResult.error(() -> "Item stack with stack size of " + p_332181_.getCount() + " was larger than maximum: " + p_332181_.getMaxStackSize())
            : DataResult.success(p_332181_);
      }
   }

   private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> createOptionalStreamCodec(
      final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> p_393485_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf p_328393_) {
            int i = p_328393_.readVarInt();
            if (i <= 0) {
               return ItemStack.EMPTY;
            }

            Holder<Item> holder = Item.STREAM_CODEC.decode(p_328393_);
            DataComponentPatch datacomponentpatch = p_393485_.decode(p_328393_);
            return new ItemStack(holder, i, datacomponentpatch);
         }

         public void encode(RegistryFriendlyByteBuf p_332266_, ItemStack p_335702_) {
            if (p_335702_.isEmpty()) {
               p_332266_.writeVarInt(0);
            } else {
               p_332266_.writeVarInt(p_335702_.getCount());
               Item.STREAM_CODEC.encode(p_332266_, p_335702_.getItemHolder());
               p_393485_.encode(p_332266_, p_335702_.components.asPatch());
            }
         }
      };
   }

   public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> validatedStreamCodec(final StreamCodec<RegistryFriendlyByteBuf, ItemStack> p_332790_) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf p_330762_) {
            ItemStack itemstack = p_332790_.decode(p_330762_);
            if (!itemstack.isEmpty()) {
               RegistryOps<Unit> registryops = p_330762_.registryAccess().createSerializationContext(NullOps.INSTANCE);
               ItemStack.CODEC.encodeStart(registryops, itemstack).getOrThrow(DecoderException::new);
            }

            return itemstack;
         }

         public void encode(RegistryFriendlyByteBuf p_336131_, ItemStack p_329943_) {
            p_332790_.encode(p_336131_, p_329943_);
         }
      };
   }

   public Optional<TooltipComponent> getTooltipImage() {
      return this.getItem().getTooltipImage(this);
   }

   @Override
   public DataComponentMap getComponents() {
      return !this.isEmpty() ? this.components : DataComponentMap.EMPTY;
   }

   public DataComponentMap getPrototype() {
      return !this.isEmpty() ? this.getItem().components() : DataComponentMap.EMPTY;
   }

   public DataComponentPatch getComponentsPatch() {
      return !this.isEmpty() ? this.components.asPatch() : DataComponentPatch.EMPTY;
   }

   public DataComponentMap immutableComponents() {
      return !this.isEmpty() ? this.components.toImmutableMap() : DataComponentMap.EMPTY;
   }

   public boolean hasNonDefault(DataComponentType<?> p_377204_) {
      return !this.isEmpty() && this.components.hasNonDefault(p_377204_);
   }

   public ItemStack(ItemLike p_41599_) {
      this(p_41599_, 1);
   }

   public ItemStack(Holder<Item> p_204116_) {
      this(p_204116_.value(), 1);
   }

   public ItemStack(Holder<Item> p_310702_, int p_41605_, DataComponentPatch p_328221_) {
      this(p_310702_.value(), p_41605_, PatchedDataComponentMap.fromPatch(p_310702_.value().components(), p_328221_));
   }

   public ItemStack(Holder<Item> p_220155_, int p_220156_) {
      this(p_220155_.value(), p_220156_);
   }

   public ItemStack(ItemLike p_41601_, int p_41602_) {
      this(p_41601_, p_41602_, new PatchedDataComponentMap(p_41601_.asItem().components()));
   }

   private ItemStack(ItemLike p_331826_, int p_332766_, PatchedDataComponentMap p_333722_) {
      this.item = p_331826_.asItem();
      this.count = p_332766_;
      this.components = p_333722_;
   }

   private ItemStack(@Nullable Void p_282703_) {
      this.item = null;
      this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
   }

   public static DataResult<Unit> validateComponents(DataComponentMap p_336343_) {
      if (p_336343_.has(DataComponents.MAX_DAMAGE) && p_336343_.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
         return DataResult.error(() -> "Item cannot be both damageable and stackable");
      }

      ItemContainerContents itemcontainercontents = p_336343_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

      for (ItemStack itemstack : itemcontainercontents.nonEmptyItems()) {
         int i = itemstack.getCount();
         int j = itemstack.getMaxStackSize();
         if (i > j) {
            return DataResult.error(() -> "Item stack with count of " + i + " was larger than maximum: " + j);
         }
      }

      return DataResult.success(Unit.INSTANCE);
   }

   public boolean isEmpty() {
      return this == EMPTY || this.item == Items.AIR || this.count <= 0;
   }

   public boolean isItemEnabled(FeatureFlagSet p_250869_) {
      return this.isEmpty() || this.getItem().isEnabled(p_250869_);
   }

   public ItemStack split(int p_41621_) {
      int i = Math.min(p_41621_, this.getCount());
      ItemStack itemstack = this.copyWithCount(i);
      this.shrink(i);
      return itemstack;
   }

   public ItemStack copyAndClear() {
      if (this.isEmpty()) {
         return EMPTY;
      }

      ItemStack itemstack = this.copy();
      this.setCount(0);
      return itemstack;
   }

   public Item getItem() {
      return this.isEmpty() ? Items.AIR : this.item;
   }

   public Holder<Item> getItemHolder() {
      return this.getItem().builtInRegistryHolder();
   }

   public boolean is(TagKey<Item> p_204118_) {
      return this.getItem().builtInRegistryHolder().is(p_204118_);
   }

   public boolean is(Item p_150931_) {
      return this.getItem() == p_150931_;
   }

   public boolean is(Predicate<Holder<Item>> p_220168_) {
      return p_220168_.test(this.getItem().builtInRegistryHolder());
   }

   public boolean is(Holder<Item> p_220166_) {
      return this.getItem().builtInRegistryHolder() == p_220166_;
   }

   public boolean is(HolderSet<Item> p_299078_) {
      return p_299078_.contains(this.getItemHolder());
   }

   public Stream<TagKey<Item>> getTags() {
      return this.getItem().builtInRegistryHolder().tags();
   }

   public InteractionResult useOn(UseOnContext p_41662_) {
      Player player = p_41662_.getPlayer();
      BlockPos blockpos = p_41662_.getClickedPos();
      if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(p_41662_.getLevel(), blockpos, false))) {
         return InteractionResult.PASS;
      }

      Item item = this.getItem();
      InteractionResult interactionresult = item.useOn(p_41662_);
      if (player != null && interactionresult instanceof InteractionResult.Success interactionresult$success && interactionresult$success.wasItemInteraction()) {
         player.awardStat(Stats.ITEM_USED.get(item));
      }

      return interactionresult;
   }

   public float getDestroySpeed(BlockState p_41692_) {
      return this.getItem().getDestroySpeed(this, p_41692_);
   }

   public InteractionResult use(Level p_41683_, Player p_41684_, InteractionHand p_41685_) {
      ItemStack itemstack = this.copy();
      boolean flag = this.getUseDuration(p_41684_) <= 0;
      InteractionResult interactionresult = this.getItem().use(p_41683_, p_41684_, p_41685_);
      return flag && interactionresult instanceof InteractionResult.Success interactionresult$success
         ? interactionresult$success.heldItemTransformedTo(
            interactionresult$success.heldItemTransformedTo() == null
               ? this.applyAfterUseComponentSideEffects(p_41684_, itemstack)
               : interactionresult$success.heldItemTransformedTo().applyAfterUseComponentSideEffects(p_41684_, itemstack)
         )
         : interactionresult;
   }

   public ItemStack finishUsingItem(Level p_41672_, LivingEntity p_41673_) {
      ItemStack itemstack = this.copy();
      ItemStack itemstack1 = this.getItem().finishUsingItem(this, p_41672_, p_41673_);
      return itemstack1.applyAfterUseComponentSideEffects(p_41673_, itemstack);
   }

   private ItemStack applyAfterUseComponentSideEffects(LivingEntity p_367870_, ItemStack p_361647_) {
      UseRemainder useremainder = p_361647_.get(DataComponents.USE_REMAINDER);
      UseCooldown usecooldown = p_361647_.get(DataComponents.USE_COOLDOWN);
      int i = p_361647_.getCount();
      ItemStack itemstack = this;
      if (useremainder != null) {
         itemstack = useremainder.convertIntoRemainder(this, i, p_367870_.hasInfiniteMaterials(), p_367870_::handleExtraItemsCreatedOnUse);
      }

      if (usecooldown != null) {
         usecooldown.apply(p_361647_, p_367870_);
      }

      return itemstack;
   }

   public int getMaxStackSize() {
      return this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   public boolean isStackable() {
      return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
   }

   public boolean isDamageableItem() {
      return this.has(DataComponents.MAX_DAMAGE) && !this.has(DataComponents.UNBREAKABLE) && this.has(DataComponents.DAMAGE);
   }

   public boolean isDamaged() {
      return this.isDamageableItem() && this.getDamageValue() > 0;
   }

   public int getDamageValue() {
      return Mth.clamp(this.getOrDefault(DataComponents.DAMAGE, 0), 0, this.getMaxDamage());
   }

   public void setDamageValue(int p_41722_) {
      this.set(DataComponents.DAMAGE, Mth.clamp(p_41722_, 0, this.getMaxDamage()));
   }

   public int getMaxDamage() {
      return this.getOrDefault(DataComponents.MAX_DAMAGE, 0);
   }

   public boolean isBroken() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
   }

   public boolean nextDamageWillBreak() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
   }

   public void hurtAndBreak(int p_220158_, ServerLevel p_342197_, @Nullable ServerPlayer p_220160_, Consumer<Item> p_343361_) {
      int i = this.processDurabilityChange(p_220158_, p_342197_, p_220160_);
      if (i != 0) {
         this.applyDamage(this.getDamageValue() + i, p_220160_, p_343361_);
      }
   }

   private int processDurabilityChange(int p_362423_, ServerLevel p_364910_, @Nullable ServerPlayer p_365570_) {
      if (!this.isDamageableItem()) {
         return 0;
      } else if (p_365570_ != null && p_365570_.hasInfiniteMaterials()) {
         return 0;
      } else {
         return p_362423_ > 0 ? EnchantmentHelper.processDurabilityChange(p_364910_, this, p_362423_) : p_362423_;
      }
   }

   private void applyDamage(int p_365629_, @Nullable ServerPlayer p_367167_, Consumer<Item> p_364849_) {
      if (p_367167_ != null) {
         CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(p_367167_, this, p_365629_);
      }

      this.setDamageValue(p_365629_);
      if (this.isBroken()) {
         Item item = this.getItem();
         this.shrink(1);
         p_364849_.accept(item);
      }
   }

   public void hurtWithoutBreaking(int p_363289_, Player p_369700_) {
      if (p_369700_ instanceof ServerPlayer serverplayer) {
         int i = this.processDurabilityChange(p_363289_, serverplayer.level(), serverplayer);
         if (i == 0) {
            return;
         }

         int j = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1);
         this.applyDamage(j, serverplayer, p_359411_ -> {});
      }
   }

   public void hurtAndBreak(int p_407155_, LivingEntity p_406974_, InteractionHand p_410631_) {
      this.hurtAndBreak(p_407155_, p_406974_, p_410631_.asEquipmentSlot());
   }

   public void hurtAndBreak(int p_41623_, LivingEntity p_41624_, EquipmentSlot p_335324_) {
      if (p_41624_.level() instanceof ServerLevel serverlevel) {
         this.hurtAndBreak(
            p_41623_,
            serverlevel,
            p_41624_ instanceof ServerPlayer serverplayer ? serverplayer : null,
            p_341563_ -> p_41624_.onEquippedItemBroken(p_341563_, p_335324_)
         );
      }
   }

   public ItemStack hurtAndConvertOnBreak(int p_343792_, ItemLike p_344647_, LivingEntity p_342270_, EquipmentSlot p_345347_) {
      this.hurtAndBreak(p_343792_, p_342270_, p_345347_);
      if (this.isEmpty()) {
         ItemStack itemstack = this.transmuteCopyIgnoreEmpty(p_344647_, 1);
         if (itemstack.isDamageableItem()) {
            itemstack.setDamageValue(0);
         }

         return itemstack;
      } else {
         return this;
      }
   }

   public boolean isBarVisible() {
      return this.getItem().isBarVisible(this);
   }

   public int getBarWidth() {
      return this.getItem().getBarWidth(this);
   }

   public int getBarColor() {
      return this.getItem().getBarColor(this);
   }

   public boolean overrideStackedOnOther(Slot p_150927_, ClickAction p_150928_, Player p_150929_) {
      return this.getItem().overrideStackedOnOther(this, p_150927_, p_150928_, p_150929_);
   }

   public boolean overrideOtherStackedOnMe(ItemStack p_150933_, Slot p_150934_, ClickAction p_150935_, Player p_150936_, SlotAccess p_150937_) {
      return this.getItem().overrideOtherStackedOnMe(this, p_150933_, p_150934_, p_150935_, p_150936_, p_150937_);
   }

   public boolean hurtEnemy(LivingEntity p_41641_, LivingEntity p_366644_) {
      Item item = this.getItem();
      item.hurtEnemy(this, p_41641_, p_366644_);
      if (this.has(DataComponents.WEAPON)) {
         if (p_366644_ instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(item));
         }

         return true;
      } else {
         return false;
      }
   }

   public void postHurtEnemy(LivingEntity p_343236_, LivingEntity p_363977_) {
      this.getItem().postHurtEnemy(this, p_343236_, p_363977_);
      Weapon weapon = this.get(DataComponents.WEAPON);
      if (weapon != null) {
         this.hurtAndBreak(weapon.itemDamagePerAttack(), p_363977_, EquipmentSlot.MAINHAND);
      }
   }

   public void mineBlock(Level p_41687_, BlockState p_41688_, BlockPos p_41689_, Player p_41690_) {
      Item item = this.getItem();
      if (item.mineBlock(this, p_41687_, p_41688_, p_41689_, p_41690_)) {
         p_41690_.awardStat(Stats.ITEM_USED.get(item));
      }
   }

   public boolean isCorrectToolForDrops(BlockState p_41736_) {
      return this.getItem().isCorrectToolForDrops(this, p_41736_);
   }

   public InteractionResult interactLivingEntity(Player p_41648_, LivingEntity p_41649_, InteractionHand p_41650_) {
      Equippable equippable = this.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.equipOnInteract()) {
         InteractionResult interactionresult = equippable.equipOnTarget(p_41648_, p_41649_, this);
         if (interactionresult != InteractionResult.PASS) {
            return interactionresult;
         }
      }

      return this.getItem().interactLivingEntity(this, p_41648_, p_41649_, p_41650_);
   }

   public ItemStack copy() {
      if (this.isEmpty()) {
         return EMPTY;
      }

      ItemStack itemstack = new ItemStack(this.getItem(), this.count, this.components.copy());
      itemstack.setPopTime(this.getPopTime());
      return itemstack;
   }

   public ItemStack copyWithCount(int p_256354_) {
      if (this.isEmpty()) {
         return EMPTY;
      }

      ItemStack itemstack = this.copy();
      itemstack.setCount(p_256354_);
      return itemstack;
   }

   public ItemStack transmuteCopy(ItemLike p_345281_) {
      return this.transmuteCopy(p_345281_, this.getCount());
   }

   public ItemStack transmuteCopy(ItemLike p_334328_, int p_334821_) {
      return this.isEmpty() ? EMPTY : this.transmuteCopyIgnoreEmpty(p_334328_, p_334821_);
   }

   private ItemStack transmuteCopyIgnoreEmpty(ItemLike p_332114_, int p_333334_) {
      return new ItemStack(p_332114_.asItem().builtInRegistryHolder(), p_333334_, this.components.asPatch());
   }

   public static boolean matches(ItemStack p_41729_, ItemStack p_41730_) {
      if (p_41729_ == p_41730_) {
         return true;
      } else {
         return p_41729_.getCount() != p_41730_.getCount() ? false : isSameItemSameComponents(p_41729_, p_41730_);
      }
   }

   @Deprecated
   public static boolean listMatches(List<ItemStack> p_335471_, List<ItemStack> p_334624_) {
      if (p_335471_.size() != p_334624_.size()) {
         return false;
      }

      for (int i = 0; i < p_335471_.size(); i++) {
         if (!matches(p_335471_.get(i), p_334624_.get(i))) {
            return false;
         }
      }

      return true;
   }

   public static boolean isSameItem(ItemStack p_287761_, ItemStack p_287676_) {
      return p_287761_.is(p_287676_.getItem());
   }

   public static boolean isSameItemSameComponents(ItemStack p_334397_, ItemStack p_331609_) {
      if (!p_334397_.is(p_331609_.getItem())) {
         return false;
      } else {
         return p_334397_.isEmpty() && p_331609_.isEmpty() ? true : Objects.equals(p_334397_.components, p_331609_.components);
      }
   }

   public static boolean matchesIgnoringComponents(ItemStack p_460667_, ItemStack p_457382_, Predicate<DataComponentType<?>> p_453488_) {
      if (p_460667_ == p_457382_) {
         return true;
      }

      if (p_460667_.getCount() != p_457382_.getCount()) {
         return false;
      }

      if (!p_460667_.is(p_457382_.getItem())) {
         return false;
      }

      if (p_460667_.isEmpty() && p_457382_.isEmpty()) {
         return true;
      }

      if (p_460667_.components.size() != p_457382_.components.size()) {
         return false;
      }

      for (DataComponentType<?> datacomponenttype : p_460667_.components.keySet()) {
         Object object = p_460667_.components.get(datacomponenttype);
         Object object1 = p_457382_.components.get(datacomponenttype);
         if (object == null || object1 == null) {
            return false;
         }

         if (!Objects.equals(object, object1) && !p_453488_.test(datacomponenttype)) {
            return false;
         }
      }

      return true;
   }

   public static MapCodec<ItemStack> lenientOptionalFieldOf(String p_336149_) {
      return CODEC.lenientOptionalFieldOf(p_336149_)
         .xmap(p_327174_ -> p_327174_.orElse(EMPTY), p_327162_ -> p_327162_.isEmpty() ? Optional.empty() : Optional.of(p_327162_));
   }

   public static int hashItemAndComponents(@Nullable ItemStack p_334004_) {
      if (p_334004_ != null) {
         int i = 31 + p_334004_.getItem().hashCode();
         return 31 * i + p_334004_.getComponents().hashCode();
      } else {
         return 0;
      }
   }

   @Deprecated
   public static int hashStackList(List<ItemStack> p_333449_) {
      int i = 0;

      for (ItemStack itemstack : p_333449_) {
         i = i * 31 + hashItemAndComponents(itemstack);
      }

      return i;
   }

   @Override
   public String toString() {
      return this.getCount() + " " + this.getItem();
   }

   public void inventoryTick(Level p_41667_, Entity p_41668_, @Nullable EquipmentSlot p_391620_) {
      if (this.popTime > 0) {
         this.popTime--;
      }

      if (p_41667_ instanceof ServerLevel serverlevel) {
         this.getItem().inventoryTick(this, serverlevel, p_41668_, p_391620_);
      }
   }

   public void onCraftedBy(Player p_41680_, int p_41681_) {
      p_41680_.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), p_41681_);
      this.getItem().onCraftedBy(this, p_41680_);
   }

   public void onCraftedBySystem(Level p_311164_) {
      this.getItem().onCraftedPostProcess(this, p_311164_);
   }

   public int getUseDuration(LivingEntity p_343439_) {
      return this.getItem().getUseDuration(this, p_343439_);
   }

   public ItemUseAnimation getUseAnimation() {
      return this.getItem().getUseAnimation(this);
   }

   public void releaseUsing(Level p_41675_, LivingEntity p_41676_, int p_41677_) {
      ItemStack itemstack = this.copy();
      if (this.getItem().releaseUsing(this, p_41675_, p_41676_, p_41677_)) {
         ItemStack itemstack1 = this.applyAfterUseComponentSideEffects(p_41676_, itemstack);
         if (itemstack1 != this) {
            p_41676_.setItemInHand(p_41676_.getUsedItemHand(), itemstack1);
         }
      }
   }

   public void causeUseVibration(Entity p_460083_, Holder.Reference<GameEvent> p_460931_) {
      UseEffects useeffects = this.get(DataComponents.USE_EFFECTS);
      if (useeffects != null && useeffects.interactVibrations()) {
         p_460083_.gameEvent(p_460931_);
      }
   }

   public boolean useOnRelease() {
      return this.getItem().useOnRelease(this);
   }

   public <T> @Nullable T set(DataComponentType<T> p_332666_, @Nullable T p_335655_) {
      return this.components.set(p_332666_, p_335655_);
   }

   public <T> @Nullable T set(TypedDataComponent<T> p_425166_) {
      return this.components.set(p_425166_);
   }

   public <T> void copyFrom(DataComponentType<T> p_391623_, DataComponentGetter p_394516_) {
      this.set(p_391623_, p_394516_.get(p_391623_));
   }

   public <T, U> @Nullable T update(DataComponentType<T> p_331418_, T p_327708_, U p_332086_, BiFunction<T, U, T> p_329834_) {
      return this.set(p_331418_, p_329834_.apply(this.getOrDefault(p_331418_, p_327708_), p_332086_));
   }

   public <T> @Nullable T update(DataComponentType<T> p_329905_, T p_329705_, UnaryOperator<T> p_335114_) {
      T t = this.getOrDefault(p_329905_, p_329705_);
      return this.set(p_329905_, p_335114_.apply(t));
   }

   public <T> @Nullable T remove(DataComponentType<? extends T> p_333259_) {
      return this.components.remove(p_333259_);
   }

   public void applyComponentsAndValidate(DataComponentPatch p_336111_) {
      DataComponentPatch datacomponentpatch = this.components.asPatch();
      this.components.applyPatch(p_336111_);
      Optional<Error<ItemStack>> optional = validateStrict(this).error();
      if (optional.isPresent()) {
         LOGGER.error("Failed to apply component patch '{}' to item: '{}'", p_336111_, optional.get().message());
         this.components.restorePatch(datacomponentpatch);
      }
   }

   public void applyComponents(DataComponentPatch p_328534_) {
      this.components.applyPatch(p_328534_);
   }

   public void applyComponents(DataComponentMap p_335208_) {
      this.components.setAll(p_335208_);
   }

   public Component getHoverName() {
      Component component = this.getCustomName();
      return component != null ? component : this.getItemName();
   }

   public @Nullable Component getCustomName() {
      Component component = this.get(DataComponents.CUSTOM_NAME);
      if (component != null) {
         return component;
      }

      WrittenBookContent writtenbookcontent = this.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if (writtenbookcontent != null) {
         String s = writtenbookcontent.title().raw();
         if (!StringUtil.isBlank(s)) {
            return Component.literal(s);
         }
      }

      return null;
   }

   public Component getItemName() {
      return this.getItem().getName(this);
   }

   public Component getStyledHoverName() {
      MutableComponent mutablecomponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().color());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutablecomponent.withStyle(ChatFormatting.ITALIC);
      }

      return mutablecomponent;
   }

   public <T extends TooltipProvider> void addToTooltip(
      DataComponentType<T> p_331934_, Item.TooltipContext p_333562_, TooltipDisplay p_397538_, Consumer<Component> p_334534_, TooltipFlag p_333715_
   ) {
      T t = (T)this.get(p_331934_);
      if (t != null && p_397538_.shows(p_331934_)) {
         t.addToTooltip(p_333562_, p_334534_, p_333715_, this.components);
      }
   }

   public List<Component> getTooltipLines(Item.TooltipContext p_331329_, @Nullable Player p_41652_, TooltipFlag p_41653_) {
      TooltipDisplay tooltipdisplay = this.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      if (!p_41653_.isCreative() && tooltipdisplay.hideTooltip()) {
         boolean flag = this.getItem().shouldPrintOpWarning(this, p_41652_);
         return flag ? OP_NBT_WARNING : List.of();
      } else {
         List<Component> list = Lists.newArrayList();
         list.add(this.getStyledHoverName());
         this.addDetailsToTooltip(p_331329_, tooltipdisplay, p_41652_, p_41653_, list::add);
         return list;
      }
   }

   public void addDetailsToTooltip(
      Item.TooltipContext p_396953_, TooltipDisplay p_394554_, @Nullable Player p_393346_, TooltipFlag p_392044_, Consumer<Component> p_396200_
   ) {
      this.getItem().appendHoverText(this, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.TROPICAL_FISH_PATTERN, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.INSTRUMENT, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.MAP_ID, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.BEES, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.CONTAINER_LOOT, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.CONTAINER, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.BANNER_PATTERNS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.POT_DECORATIONS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.WRITTEN_BOOK_CONTENT, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.CHARGED_PROJECTILES, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.FIREWORKS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.FIREWORK_EXPLOSION, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.POTION_CONTENTS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.TRIM, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.ENCHANTMENTS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.DYED_COLOR, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.PROFILE, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.LORE, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addAttributeTooltips(p_396200_, p_394554_, p_393346_);
      this.addUnitComponentToTooltip(DataComponents.INTANGIBLE_PROJECTILE, INTANGIBLE_TOOLTIP, p_394554_, p_396200_);
      this.addUnitComponentToTooltip(DataComponents.UNBREAKABLE, UNBREAKABLE_TOOLTIP, p_394554_, p_396200_);
      this.addToTooltip(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.SUSPICIOUS_STEW_EFFECTS, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.BLOCK_STATE, p_396953_, p_394554_, p_396200_, p_392044_);
      this.addToTooltip(DataComponents.ENTITY_DATA, p_396953_, p_394554_, p_396200_, p_392044_);
      if ((this.is(Items.SPAWNER) || this.is(Items.TRIAL_SPAWNER)) && p_394554_.shows(DataComponents.BLOCK_ENTITY_DATA)) {
         TypedEntityData<BlockEntityType<?>> typedentitydata = this.get(DataComponents.BLOCK_ENTITY_DATA);
         Spawner.appendHoverText(typedentitydata, p_396200_, "SpawnData");
      }

      AdventureModePredicate adventuremodepredicate1 = this.get(DataComponents.CAN_BREAK);
      if (adventuremodepredicate1 != null && p_394554_.shows(DataComponents.CAN_BREAK)) {
         p_396200_.accept(CommonComponents.EMPTY);
         p_396200_.accept(AdventureModePredicate.CAN_BREAK_HEADER);
         adventuremodepredicate1.addToTooltip(p_396200_);
      }

      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
      if (adventuremodepredicate != null && p_394554_.shows(DataComponents.CAN_PLACE_ON)) {
         p_396200_.accept(CommonComponents.EMPTY);
         p_396200_.accept(AdventureModePredicate.CAN_PLACE_HEADER);
         adventuremodepredicate.addToTooltip(p_396200_);
      }

      if (p_392044_.isAdvanced()) {
         if (this.isDamaged() && p_394554_.shows(DataComponents.DAMAGE)) {
            p_396200_.accept(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
         }

         p_396200_.accept(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
         int i = this.components.size();
         if (i > 0) {
            p_396200_.accept(Component.translatable("item.components", i).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      if (p_393346_ != null && !this.getItem().isEnabled(p_393346_.level().enabledFeatures())) {
         p_396200_.accept(DISABLED_ITEM_TOOLTIP);
      }

      boolean flag = this.getItem().shouldPrintOpWarning(this, p_393346_);
      if (flag) {
         OP_NBT_WARNING.forEach(p_396200_);
      }
   }

   private void addUnitComponentToTooltip(DataComponentType<?> p_450330_, Component p_454467_, TooltipDisplay p_450569_, Consumer<Component> p_456121_) {
      if (this.has(p_450330_) && p_450569_.shows(p_450330_)) {
         p_456121_.accept(p_454467_);
      }
   }

   private void addAttributeTooltips(Consumer<Component> p_333346_, TooltipDisplay p_391795_, @Nullable Player p_332769_) {
      if (p_391795_.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
         for (EquipmentSlotGroup equipmentslotgroup : EquipmentSlotGroup.values()) {
            MutableBoolean mutableboolean = new MutableBoolean(true);
            this.forEachModifier(equipmentslotgroup, (p_405609_, p_405610_, p_405611_) -> {
               if (p_405611_ != ItemAttributeModifiers.Display.hidden()) {
                  if (mutableboolean.isTrue()) {
                     p_333346_.accept(CommonComponents.EMPTY);
                     p_333346_.accept(Component.translatable("item.modifiers." + equipmentslotgroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                     mutableboolean.setFalse();
                  }

                  p_405611_.apply(p_333346_, p_332769_, p_405609_, p_405610_);
               }
            });
         }
      }
   }

   public boolean hasFoil() {
      Boolean obool = this.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      return obool != null ? obool : this.getItem().isFoil(this);
   }

   public Rarity getRarity() {
      Rarity rarity = this.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
      if (!this.isEnchanted()) {
         return rarity;
      }

      return switch (rarity) {
         case COMMON, UNCOMMON -> Rarity.RARE;
         case RARE -> Rarity.EPIC;
         default -> rarity;
      };
   }

   public boolean isEnchantable() {
      if (!this.has(DataComponents.ENCHANTABLE)) {
         return false;
      }

      ItemEnchantments itemenchantments = this.get(DataComponents.ENCHANTMENTS);
      return itemenchantments != null && itemenchantments.isEmpty();
   }

   public void enchant(Holder<Enchantment> p_342791_, int p_41665_) {
      EnchantmentHelper.updateEnchantments(this, p_341557_ -> p_341557_.upgrade(p_342791_, p_41665_));
   }

   public boolean isEnchanted() {
      return !this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
   }

   public ItemEnchantments getEnchantments() {
      return this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
   }

   public boolean isFramed() {
      return this.entityRepresentation instanceof ItemFrame;
   }

   public void setEntityRepresentation(@Nullable Entity p_41637_) {
      if (!this.isEmpty()) {
         this.entityRepresentation = p_41637_;
      }
   }

   public @Nullable ItemFrame getFrame() {
      return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
   }

   public @Nullable Entity getEntityRepresentation() {
      return !this.isEmpty() ? this.entityRepresentation : null;
   }

   public void forEachModifier(EquipmentSlotGroup p_344758_, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> p_409084_) {
      ItemAttributeModifiers itemattributemodifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      itemattributemodifiers.forEach(p_344758_, p_409084_);
      EnchantmentHelper.forEachModifier(
         this, p_344758_, (p_405603_, p_405604_) -> p_409084_.accept(p_405603_, p_405604_, ItemAttributeModifiers.Display.attributeModifiers())
      );
   }

   public void forEachModifier(EquipmentSlot p_331036_, BiConsumer<Holder<Attribute>, AttributeModifier> p_334430_) {
      ItemAttributeModifiers itemattributemodifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      itemattributemodifiers.forEach(p_331036_, p_334430_);
      EnchantmentHelper.forEachModifier(this, p_331036_, p_334430_);
   }

   public Component getDisplayName() {
      MutableComponent mutablecomponent = Component.empty().append(this.getHoverName());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutablecomponent.withStyle(ChatFormatting.ITALIC);
      }

      MutableComponent mutablecomponent1 = ComponentUtils.wrapInSquareBrackets(mutablecomponent);
      if (!this.isEmpty()) {
         mutablecomponent1.withStyle(this.getRarity().color()).withStyle(p_390810_ -> p_390810_.withHoverEvent(new HoverEvent.ShowItem(this)));
      }

      return mutablecomponent1;
   }

   public SwingAnimation getSwingAnimation() {
      return this.getOrDefault(DataComponents.SWING_ANIMATION, SwingAnimation.DEFAULT);
   }

   public boolean canPlaceOnBlockInAdventureMode(BlockInWorld p_331134_) {
      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
      return adventuremodepredicate != null && adventuremodepredicate.test(p_331134_);
   }

   public boolean canBreakBlockInAdventureMode(BlockInWorld p_333133_) {
      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_BREAK);
      return adventuremodepredicate != null && adventuremodepredicate.test(p_333133_);
   }

   public int getPopTime() {
      return this.popTime;
   }

   public void setPopTime(int p_41755_) {
      this.popTime = p_41755_;
   }

   public int getCount() {
      return this.isEmpty() ? 0 : this.count;
   }

   public void setCount(int p_41765_) {
      this.count = p_41765_;
   }

   public void limitSize(int p_328100_) {
      if (!this.isEmpty() && this.getCount() > p_328100_) {
         this.setCount(p_328100_);
      }
   }

   public void grow(int p_41770_) {
      this.setCount(this.getCount() + p_41770_);
   }

   public void shrink(int p_41775_) {
      this.grow(-p_41775_);
   }

   public void consume(int p_329683_, @Nullable LivingEntity p_334302_) {
      if (p_334302_ == null || !p_334302_.hasInfiniteMaterials()) {
         this.shrink(p_329683_);
      }
   }

   public ItemStack consumeAndReturn(int p_343693_, @Nullable LivingEntity p_344112_) {
      ItemStack itemstack = this.copyWithCount(p_343693_);
      this.consume(p_343693_, p_344112_);
      return itemstack;
   }

   public void onUseTick(Level p_41732_, LivingEntity p_41733_, int p_41734_) {
      Consumable consumable = this.get(DataComponents.CONSUMABLE);
      if (consumable != null && consumable.shouldEmitParticlesAndSounds(p_41734_)) {
         consumable.emitParticlesAndSounds(p_41733_.getRandom(), p_41733_, this, 5);
      }

      KineticWeapon kineticweapon = this.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null && !p_41732_.isClientSide()) {
         kineticweapon.damageEntities(this, p_41734_, p_41733_, p_41733_.getUsedItemHand().asEquipmentSlot());
      } else {
         this.getItem().onUseTick(p_41732_, p_41733_, this, p_41734_);
      }
   }

   public void onDestroyed(ItemEntity p_150925_) {
      this.getItem().onDestroyed(p_150925_);
   }

   public boolean canBeHurtBy(DamageSource p_334859_) {
      DamageResistant damageresistant = this.get(DataComponents.DAMAGE_RESISTANT);
      return damageresistant == null || !damageresistant.isResistantTo(p_334859_);
   }

   public boolean isValidRepairItem(ItemStack p_368140_) {
      Repairable repairable = this.get(DataComponents.REPAIRABLE);
      return repairable != null && repairable.isValidRepairItem(p_368140_);
   }

   public boolean canDestroyBlock(BlockState p_394125_, Level p_391865_, BlockPos p_396538_, Player p_395561_) {
      return this.getItem().canDestroyBlock(this, p_394125_, p_391865_, p_396538_, p_395561_);
   }

   public DamageSource getDamageSource(LivingEntity p_460155_, Supplier<DamageSource> p_451611_) {
      return Optional.ofNullable(this.get(DataComponents.DAMAGE_TYPE))
         .flatMap(p_449798_ -> p_449798_.unwrap(p_460155_.registryAccess()))
         .map(p_449795_ -> new DamageSource((Holder<DamageType>)p_449795_, p_460155_))
         .or(() -> Optional.ofNullable(this.getItem().getItemDamageSource(p_460155_)))
         .orElseGet(p_451611_);
   }
}
