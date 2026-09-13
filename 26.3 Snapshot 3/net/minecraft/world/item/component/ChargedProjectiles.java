package net.minecraft.world.item.component;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import org.slf4j.Logger;

public record ChargedProjectiles(List<ItemStackTemplate> items) implements ContainerComponent<ChargedProjectiles>, TooltipProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_SIZE = 1024;
   public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
   public static final Codec<ChargedProjectiles> CODEC = ItemStackTemplate.CODEC
      .sizeLimitedListOf(1024)
      .xmap(ChargedProjectiles::new, projectiles -> projectiles.items);
   public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC
      .apply(ByteBufCodecs.list(1024))
      .map(ChargedProjectiles::new, projectiles -> projectiles.items);

   public ChargedProjectiles {
      if (items.size() > 1024) {
         throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is 1024");
      }
   }

   public static ChargedProjectiles of(final ItemStackTemplate stack) {
      return new ChargedProjectiles(List.of(stack));
   }

   public static ChargedProjectiles ofNonEmpty(final List<ItemStack> items) {
      List<ItemStackTemplate> list = items.stream().filter(i -> !i.isEmpty()).map(ItemStackTemplate::fromStack).limit(1024L).toList();
      if (list.size() != items.size()) {
         LOGGER.warn("Tried to load invalid items as charged projectiles");
      }

      return new ChargedProjectiles(list);
   }

   public boolean contains(final Item item) {
      for (ItemStackTemplate projectile : this.items) {
         if (projectile.is(item)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public Stream<ItemStack> itemCopies() {
      return this.items.stream().map(ItemStackTemplate::create);
   }

   @Override
   public int size() {
      return this.items.size();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public ChargedProjectiles copyWithContents(final Stream<ItemStack> newContents) {
      return new ChargedProjectiles(newContents.filter(s -> !s.isEmpty()).map(ItemStackTemplate::fromNonEmptyStack).toList());
   }

   public ChargedProjectiles.Mutable asMutable() {
      List<ItemStack> itemsList = new ArrayList<>(this.items.size());

      for (ItemStackTemplate item : this.items) {
         itemsList.add(item.create());
      }

      return new ChargedProjectiles.Mutable(itemsList);
   }

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      ItemStack current = null;
      int count = 0;

      for (ItemStackTemplate projectileTemplate : this.items) {
         ItemStack projectile = projectileTemplate.create();
         if (current == null) {
            current = projectile;
            count = 1;
         } else if (ItemStack.matches(current, projectile)) {
            count++;
         } else {
            addProjectileTooltip(context, consumer, current, count);
            current = projectile;
            count = 1;
         }
      }

      if (current != null) {
         addProjectileTooltip(context, consumer, current, count);
      }
   }

   private static void addProjectileTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final ItemStack projectile, final int count) {
      if (count == 1) {
         consumer.accept(Component.translatable("item.minecraft.crossbow.projectile.single", projectile.getDisplayName()));
      } else {
         consumer.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", count, projectile.getHoverName()));
      }

      TooltipDisplay projectileDisplay = projectile.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      projectile.addDetailsToTooltip(
         context, projectileDisplay, null, TooltipFlag.NORMAL, line -> consumer.accept(Component.literal("  ").append(line).withStyle(ChatFormatting.GRAY))
      );
   }

   public static class Mutable extends SimpleMutableContainer<ChargedProjectiles> {
      private Mutable(final List<ItemStack> items) {
         super(items);
      }

      @Override
      protected boolean addSlotWithItem(final ItemStack itemStack) {
         return !itemStack.isEmpty() && super.addSlotWithItem(itemStack);
      }

      @Override
      public boolean canInsertNewSlots() {
         return this.items.size() < 1024;
      }

      public ChargedProjectiles toImmutable() {
         List<ItemStackTemplate> nonEmptyItems = new ArrayList<>(this.items.size());

         for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
               nonEmptyItems.add(ItemStackTemplate.fromNonEmptyStack(item));
            }
         }

         return new ChargedProjectiles(nonEmptyItems);
      }
   }
}
