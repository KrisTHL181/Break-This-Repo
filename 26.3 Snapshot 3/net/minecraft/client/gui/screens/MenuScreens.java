package net.minecraft.client.gui.screens;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MenuScreens {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> SCREENS = Maps.newHashMap();

   public static <T extends AbstractContainerMenu> void create(final MenuType<T> type, final Minecraft minecraft, final int containerId, final Component title) {
      MenuScreens.ScreenConstructor<T, ?> constructor = getConstructor(type);
      if (constructor == null) {
         LOGGER.warn("Failed to create screen for menu type: {}", BuiltInRegistries.MENU.getKey(type));
      } else {
         constructor.fromPacket(title, type, minecraft, containerId);
      }
   }

   private static <T extends AbstractContainerMenu> MenuScreens.@Nullable ScreenConstructor<T, ?> getConstructor(final MenuType<T> type) {
      return (MenuScreens.ScreenConstructor<T, ?>)SCREENS.get(type);
   }

   private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(
      final MenuType<? extends M> type, final MenuScreens.ScreenConstructor<M, U> factory
   ) {
      MenuScreens.ScreenConstructor<?, ?> prev = SCREENS.put(type, factory);
      if (prev != null) {
         throw new IllegalStateException("Duplicate registration for " + BuiltInRegistries.MENU.getKey(type));
      }
   }

   public static boolean selfTest() {
      boolean failed = false;

      for (MenuType<?> menuType : BuiltInRegistries.MENU) {
         if (!SCREENS.containsKey(menuType)) {
            LOGGER.debug("Menu {} has no matching screen", BuiltInRegistries.MENU.getKey(menuType));
            failed = true;
         }
      }

      return failed;
   }

   static {
      register(MenuType.GENERIC_9x1, ContainerScreen::new);
      register(MenuType.GENERIC_9x2, ContainerScreen::new);
      register(MenuType.GENERIC_9x3, ContainerScreen::new);
      register(MenuType.GENERIC_9x4, ContainerScreen::new);
      register(MenuType.GENERIC_9x5, ContainerScreen::new);
      register(MenuType.GENERIC_9x6, ContainerScreen::new);
      register(MenuType.GENERIC_3x3, DispenserScreen::new);
      register(MenuType.CRAFTER_3x3, CrafterScreen::new);
      register(MenuType.ANVIL, AnvilScreen::new);
      register(MenuType.BEACON, BeaconScreen::new);
      register(MenuType.BLAST_FURNACE, BlastFurnaceScreen::new);
      register(MenuType.BREWING_STAND, BrewingStandScreen::new);
      register(MenuType.CRAFTING, CraftingScreen::new);
      register(MenuType.ENCHANTMENT, EnchantmentScreen::new);
      register(MenuType.FURNACE, FurnaceScreen::new);
      register(MenuType.GRINDSTONE, GrindstoneScreen::new);
      register(MenuType.HOPPER, HopperScreen::new);
      register(MenuType.LECTERN, LecternScreen::new);
      register(MenuType.LOOM, LoomScreen::new);
      register(MenuType.MERCHANT, MerchantScreen::new);
      register(MenuType.SHULKER_BOX, ShulkerBoxScreen::new);
      register(MenuType.SMITHING, SmithingScreen::new);
      register(MenuType.SMOKER, SmokerScreen::new);
      register(MenuType.CARTOGRAPHY_TABLE, CartographyTableScreen::new);
      register(MenuType.STONECUTTER, StonecutterScreen::new);
   }

   private interface ScreenConstructor<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> {
      default void fromPacket(final Component title, final MenuType<T> type, final Minecraft minecraft, final int containerId) {
         U screen = this.create(type.create(containerId, minecraft.player.getInventory()), minecraft.player.getInventory(), title);
         minecraft.player.containerMenu = screen.getMenu();
         minecraft.gui.setScreen(screen);
      }

      U create(T menu, Inventory inventory, final Component title);
   }
}
