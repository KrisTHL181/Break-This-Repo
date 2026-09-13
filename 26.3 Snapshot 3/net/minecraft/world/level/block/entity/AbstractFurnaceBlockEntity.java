package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible, RecipeCraftingHolder {
   protected static final int SLOT_INPUT = 0;
   protected static final int SLOT_FUEL = 1;
   protected static final int SLOT_RESULT = 2;
   public static final int DATA_LIT_TIME = 0;
   private static final int[] SLOTS_FOR_UP = new int[]{0};
   private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
   private static final int[] SLOTS_FOR_SIDES = new int[]{1};
   public static final int DATA_LIT_DURATION = 1;
   public static final int DATA_COOKING_PROGRESS = 2;
   public static final int DATA_COOKING_TOTAL_TIME = 3;
   public static final int NUM_DATA_VALUES = 4;
   public static final int BURN_TIME_STANDARD = 200;
   public static final int BURN_COOL_SPEED = 2;
   private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
   private static final short DEFAULT_COOKING_TIMER = 0;
   private static final short DEFAULT_COOKING_TOTAL_TIME = 0;
   private static final short DEFAULT_LIT_TIME_REMAINING = 0;
   private static final short DEFAULT_LIT_TOTAL_TIME = 0;
   protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
   private int litTimeRemaining;
   private int litTotalTime;
   private int cookingTimer;
   private int cookingTotalTime;
   protected final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(final int dataId) {
         switch (dataId) {
            case 0:
               return AbstractFurnaceBlockEntity.this.litTimeRemaining;
            case 1:
               return AbstractFurnaceBlockEntity.this.litTotalTime;
            case 2:
               return AbstractFurnaceBlockEntity.this.cookingTimer;
            case 3:
               return AbstractFurnaceBlockEntity.this.cookingTotalTime;
            default:
               return 0;
         }
      }

      @Override
      public void set(final int dataId, final int value) {
         switch (dataId) {
            case 0:
               AbstractFurnaceBlockEntity.this.litTimeRemaining = value;
               break;
            case 1:
               AbstractFurnaceBlockEntity.this.litTotalTime = value;
               break;
            case 2:
               AbstractFurnaceBlockEntity.this.cookingTimer = value;
               break;
            case 3:
               AbstractFurnaceBlockEntity.this.cookingTotalTime = value;
         }
      }

      @Override
      public int getCount() {
         return 4;
      }
   };
   private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap();
   private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

   protected AbstractFurnaceBlockEntity(
      final BlockEntityType<?> type, final BlockPos worldPosition, final BlockState blockState, final RecipeType<? extends AbstractCookingRecipe> recipeType
   ) {
      super(type, worldPosition, blockState);
      this.quickCheck = RecipeManager.createCheck(recipeType);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(input, this.items);
      this.cookingTimer = input.getShortOr("cooking_time_spent", (short)0);
      this.cookingTotalTime = input.getShortOr("cooking_total_time", (short)0);
      this.litTimeRemaining = input.getShortOr("lit_time_remaining", (short)0);
      this.litTotalTime = input.getShortOr("lit_total_time", (short)0);
      this.recipesUsed.clear();
      this.recipesUsed.putAll(input.<Map>read("RecipesUsed", RECIPES_USED_CODEC).orElse(Map.of()));
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.putShort("cooking_time_spent", (short)this.cookingTimer);
      output.putShort("cooking_total_time", (short)this.cookingTotalTime);
      output.putShort("lit_time_remaining", (short)this.litTimeRemaining);
      output.putShort("lit_total_time", (short)this.litTotalTime);
      ContainerHelper.saveAllItems(output, this.items);
      output.store("RecipesUsed", RECIPES_USED_CODEC, this.recipesUsed);
   }

   public static void serverTick(final ServerLevel level, final BlockPos pos, BlockState state, final AbstractFurnaceBlockEntity entity) {
      boolean changed = false;
      boolean isLit;
      boolean wasLit;
      if (entity.litTimeRemaining > 0) {
         wasLit = true;
         entity.litTimeRemaining--;
         isLit = entity.litTimeRemaining > 0;
      } else {
         wasLit = false;
         isLit = false;
      }

      ItemStack fuel = entity.items.get(1);
      ItemStack ingredient = entity.items.get(0);
      boolean hasIngredient = !ingredient.isEmpty();
      boolean hasFuel = !fuel.isEmpty();
      if (isLit || hasFuel && hasIngredient) {
         if (hasIngredient) {
            SingleRecipeInput input = new SingleRecipeInput(ingredient);
            RecipeHolder<? extends AbstractCookingRecipe> recipe = entity.quickCheck.getRecipeFor(input, level).orElse(null);
            if (recipe != null) {
               int maxStackSize = entity.getMaxStackSize();
               ItemStack burnResult = recipe.value().assemble(input);
               if (!burnResult.isEmpty() && canBurn(entity.items, maxStackSize, burnResult)) {
                  if (!isLit) {
                     int newLitTime = entity.getBurnDuration(level.fuelValues(), fuel);
                     entity.litTimeRemaining = newLitTime;
                     entity.litTotalTime = newLitTime;
                     if (newLitTime > 0) {
                        consumeFuel(entity.items, fuel);
                        isLit = true;
                        changed = true;
                     }
                  }

                  if (isLit) {
                     entity.cookingTimer++;
                     if (entity.cookingTimer == entity.cookingTotalTime) {
                        entity.cookingTimer = 0;
                        entity.cookingTotalTime = recipe.value().cookingTime();
                        burn(entity.items, ingredient, burnResult);
                        entity.setRecipeUsed(recipe);
                        changed = true;
                     }
                  } else {
                     entity.cookingTimer = 0;
                  }
               } else {
                  entity.cookingTimer = 0;
               }
            }
         } else {
            entity.cookingTimer = 0;
         }
      } else if (entity.cookingTimer > 0) {
         entity.cookingTimer = Mth.clamp(entity.cookingTimer - 2, 0, entity.cookingTotalTime);
      }

      if (wasLit != isLit) {
         changed = true;
         state = state.setValue(AbstractFurnaceBlock.LIT, isLit);
         level.setBlockAndUpdate(pos, state);
      }

      if (changed) {
         setChanged(level, pos, state);
      }
   }

   private static void consumeFuel(final NonNullList<ItemStack> items, final ItemStack fuel) {
      Item fuelItem = fuel.getItem();
      fuel.shrink(1);
      if (fuel.isEmpty()) {
         ItemStackTemplate remainder = fuelItem.getCraftingRemainder();
         items.set(1, remainder != null ? remainder.create() : ItemStack.EMPTY);
      }
   }

   private static boolean canBurn(final NonNullList<ItemStack> items, final int maxStackSize, final ItemStack burnResult) {
      ItemStack resultItemStack = items.get(2);
      if (resultItemStack.isEmpty()) {
         return true;
      }

      if (!ItemStack.isSameItemSameComponents(resultItemStack, burnResult)) {
         return false;
      }

      int resultCount = resultItemStack.getCount() + burnResult.count();
      int maxResultCount = Math.min(maxStackSize, burnResult.getMaxStackSize());
      return resultCount <= maxResultCount;
   }

   private static void burn(final NonNullList<ItemStack> items, final ItemStack inputItemStack, final ItemStack result) {
      ItemStack resultItemStack = items.get(2);
      if (resultItemStack.isEmpty()) {
         items.set(2, result.copy());
      } else {
         resultItemStack.grow(result.getCount());
      }

      if (inputItemStack.is(Items.WET_SPONGE) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
         items.set(1, new ItemStack(Items.WATER_BUCKET));
      }

      inputItemStack.shrink(1);
   }

   protected int getBurnDuration(final FuelValues fuelValues, final ItemStack itemStack) {
      return fuelValues.burnDuration(itemStack);
   }

   private static int getTotalCookTime(final ServerLevel level, final AbstractFurnaceBlockEntity entity) {
      SingleRecipeInput input = new SingleRecipeInput(entity.getItem(0));
      return entity.quickCheck.getRecipeFor(input, level).map(recipeHolder -> recipeHolder.value().cookingTime()).orElse(200);
   }

   @Override
   public int[] getSlotsForFace(final Direction direction) {
      if (direction == Direction.DOWN) {
         return SLOTS_FOR_DOWN;
      } else {
         return direction == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
      }
   }

   @Override
   public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
      return this.canPlaceItem(slot, itemStack);
   }

   @Override
   public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
      return direction == Direction.DOWN && slot == 1 ? itemStack.is(Items.WATER_BUCKET) || itemStack.is(Items.BUCKET) : true;
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

   @Override
   public void setItem(final int slot, final ItemStack itemStack) {
      ItemStack oldStack = this.items.get(slot);
      boolean same = !itemStack.isEmpty() && ItemStack.isSameItemSameComponents(oldStack, itemStack);
      this.items.set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
      if (slot == 0 && !same && this.level instanceof ServerLevel serverLevel) {
         this.cookingTotalTime = getTotalCookTime(serverLevel, this);
         this.cookingTimer = 0;
         this.setChanged();
      }
   }

   @Override
   public boolean canPlaceItem(final int slot, final ItemStack itemStack) {
      if (slot == 2) {
         return false;
      }

      if (slot != 1) {
         return true;
      }

      ItemStack fuelSlot = this.items.get(1);
      return this.level.fuelValues().isFuel(itemStack) || itemStack.is(Items.BUCKET) && !fuelSlot.is(Items.BUCKET);
   }

   @Override
   public void setRecipeUsed(final @Nullable RecipeHolder<?> recipeUsed) {
      if (recipeUsed != null) {
         ResourceKey<Recipe<?>> id = recipeUsed.id();
         this.recipesUsed.addTo(id, 1);
      }
   }

   @Override
   public @Nullable RecipeHolder<?> getRecipeUsed() {
      return null;
   }

   @Override
   public void awardUsedRecipes(final Player player, final List<ItemStack> itemStacks) {
   }

   public void awardUsedRecipesAndPopExperience(final ServerPlayer player) {
      List<RecipeHolder<?>> recipesToAward = this.getRecipesToAwardAndPopExperience(player.level(), player.position());
      player.awardRecipes(recipesToAward);

      for (RecipeHolder<?> recipe : recipesToAward) {
         player.triggerRecipeCrafted(recipe, this.items);
      }

      this.recipesUsed.clear();
   }

   public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(final ServerLevel level, final Vec3 position) {
      List<RecipeHolder<?>> recipesToAward = Lists.newArrayList();
      ObjectIterator var4 = this.recipesUsed.reference2IntEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<ResourceKey<Recipe<?>>> entry = (Entry<ResourceKey<Recipe<?>>>)var4.next();
         level.recipeAccess().byKey((ResourceKey<Recipe<?>>)entry.getKey()).ifPresent(recipe -> {
            recipesToAward.add((RecipeHolder<?>)recipe);
            createExperience(level, position, entry.getIntValue(), ((AbstractCookingRecipe)recipe.value()).experience());
         });
      }

      return recipesToAward;
   }

   private static void createExperience(final ServerLevel level, final Vec3 position, final int amount, final float value) {
      int xpReward = Mth.floor(amount * value);
      float xpFraction = Mth.frac(amount * value);
      if (xpFraction != 0.0F && level.getRandom().nextFloat() < xpFraction) {
         xpReward++;
      }

      ExperienceOrb.award(level, position, xpReward);
   }

   @Override
   public void fillStackedContents(final StackedItemContents contents) {
      for (ItemStack itemStack : this.items) {
         contents.accountStack(itemStack);
      }
   }

   @Override
   public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
      super.preRemoveSideEffects(pos, state);
      if (this.level instanceof ServerLevel serverLevel) {
         this.getRecipesToAwardAndPopExperience(serverLevel, Vec3.atCenterOf(pos));
      }
   }
}
