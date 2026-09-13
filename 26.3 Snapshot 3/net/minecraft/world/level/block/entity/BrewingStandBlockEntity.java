package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   private static final int INGREDIENT_SLOT = 3;
   private static final int FUEL_SLOT = 4;
   private static final int[] SLOTS_FOR_UP = new int[]{3};
   private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
   private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
   public static final int FUEL_USES = 20;
   public static final int DATA_BREW_TIME = 0;
   public static final int DATA_FUEL_USES = 1;
   public static final int NUM_DATA_VALUES = 2;
   private static final short DEFAULT_BREW_TIME = 0;
   private static final byte DEFAULT_FUEL = 0;
   private static final Component DEFAULT_NAME = Component.translatable("container.brewing");
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   private int brewTime;
   private boolean[] lastPotionCount;
   private Item ingredient;
   private int fuel;
   protected final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(final int dataId) {
         return switch (dataId) {
            case 0 -> BrewingStandBlockEntity.this.brewTime;
            case 1 -> BrewingStandBlockEntity.this.fuel;
            default -> 0;
         };
      }

      @Override
      public void set(final int dataId, final int value) {
         switch (dataId) {
            case 0:
               BrewingStandBlockEntity.this.brewTime = value;
               break;
            case 1:
               BrewingStandBlockEntity.this.fuel = value;
         }
      }

      @Override
      public int getCount() {
         return 2;
      }
   };
   private final RecipeManager.CachedCheck<BrewingInput, BrewingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.BREWING);

   public BrewingStandBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.BREWING_STAND, worldPosition, blockState);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(final NonNullList<ItemStack> items) {
      this.items = items;
   }

   public static void serverTick(final ServerLevel level, final BlockPos pos, final BlockState selfState, final BrewingStandBlockEntity entity) {
      ItemStack fuel = entity.items.get(4);
      if (entity.fuel <= 0 && fuel.is(ItemTags.BREWING_FUEL)) {
         entity.fuel = 20;
         fuel.shrink(1);
         setChanged(level, pos, selfState);
      }

      boolean brewable = isBrewable(level, entity);
      boolean isBrewing = entity.brewTime > 0;
      ItemStack ingredient = entity.items.get(3);
      if (isBrewing) {
         entity.brewTime--;
         boolean isDoneBrewing = entity.brewTime == 0;
         if (isDoneBrewing && brewable) {
            doBrew(level, pos, entity);
         } else if (!brewable || !ingredient.is(entity.ingredient)) {
            entity.brewTime = 0;
         }

         setChanged(level, pos, selfState);
      } else if (brewable && entity.fuel > 0) {
         entity.fuel--;
         entity.brewTime = 400;
         entity.ingredient = ingredient.getItem();
         setChanged(level, pos, selfState);
      }

      boolean[] newCount = entity.getPotionBits();
      if (!Arrays.equals(newCount, entity.lastPotionCount)) {
         entity.lastPotionCount = newCount;
         BlockState state = selfState;
         if (!(state.getBlock() instanceof BrewingStandBlock)) {
            return;
         }

         for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
            state = state.setValue(BrewingStandBlock.HAS_BOTTLE[i], newCount[i]);
         }

         level.setBlock(pos, state, 2);
      }
   }

   private boolean[] getPotionBits() {
      boolean[] result = new boolean[3];

      for (int potion = 0; potion < 3; potion++) {
         if (!this.items.get(potion).isEmpty()) {
            result[potion] = true;
         }
      }

      return result;
   }

   private static boolean isBrewable(final ServerLevel serverLevel, final BrewingStandBlockEntity entity) {
      NonNullList<ItemStack> items = entity.getItems();
      ItemStack ingredient = items.get(3);
      if (ingredient.isEmpty()) {
         return false;
      }

      RecipeManager recipeManager = serverLevel.recipeAccess();
      if (!recipeManager.propertySet(RecipePropertySet.BREWING_REAGENTS).test(ingredient)) {
         return false;
      }

      for (int dest = 0; dest < 3; dest++) {
         ItemStack itemStack = items.get(dest);
         if (!itemStack.isEmpty()) {
            Optional<RecipeHolder<BrewingRecipe>> recipe = entity.quickCheck.getRecipeFor(new BrewingInput(itemStack, ingredient), serverLevel);
            if (recipe.isPresent()) {
               return true;
            }
         }
      }

      return false;
   }

   private static void doBrew(final ServerLevel level, final BlockPos pos, final BrewingStandBlockEntity entity) {
      NonNullList<ItemStack> items = entity.getItems();
      ItemStack ingredient = items.get(3);

      for (int dest = 0; dest < 3; dest++) {
         ItemStack container = items.get(dest);
         BrewingInput input = new BrewingInput(container, ingredient);
         Optional<RecipeHolder<BrewingRecipe>> recipe = entity.quickCheck.getRecipeFor(input, level);
         items.set(dest, recipe.isPresent() ? recipe.get().value().assemble(input) : container);
      }

      ItemStackTemplate remainder = ingredient.getItem().getCraftingRemainder();
      ingredient.shrink(1);
      if (remainder != null) {
         if (ingredient.isEmpty()) {
            ingredient = remainder.create();
         } else {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
         }
      }

      items.set(3, ingredient);
      level.levelEvent(1035, pos, 0);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(input, this.items);
      this.brewTime = input.getShortOr("BrewTime", (short)0);
      if (this.brewTime > 0) {
         this.ingredient = this.items.get(3).getItem();
      }

      this.fuel = input.getByteOr("Fuel", (byte)0);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.putShort("BrewTime", (short)this.brewTime);
      ContainerHelper.saveAllItems(output, this.items);
      output.putByte("Fuel", (byte)this.fuel);
   }

   @Override
   public boolean canPlaceItem(final int slot, final ItemStack itemStack) {
      if (slot == 4) {
         return itemStack.is(ItemTags.BREWING_FUEL);
      }

      if (this.level == null) {
         return false;
      }

      RecipeAccess recipeAccess = this.level.recipeAccess();
      return slot == 3
         ? recipeAccess.propertySet(RecipePropertySet.BREWING_REAGENTS).test(itemStack)
         : recipeAccess.propertySet(RecipePropertySet.BREWING_INPUTS).test(itemStack) && this.getItem(slot).isEmpty();
   }

   @Override
   public int[] getSlotsForFace(final Direction direction) {
      if (direction == Direction.UP) {
         return SLOTS_FOR_UP;
      } else {
         return direction == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
      return this.canPlaceItem(slot, itemStack);
   }

   @Override
   public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
      return slot == 3 ? itemStack.is(Items.GLASS_BOTTLE) : true;
   }

   @Override
   protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory) {
      return new BrewingStandMenu(containerId, inventory, this, this.dataAccess);
   }
}
