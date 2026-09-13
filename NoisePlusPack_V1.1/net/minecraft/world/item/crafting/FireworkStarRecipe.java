package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarRecipe extends CustomRecipe {
   private static final Map<Item, FireworkExplosion.Shape> SHAPE_BY_ITEM = Map.of(
      Items.FIRE_CHARGE,
      FireworkExplosion.Shape.LARGE_BALL,
      Items.FEATHER,
      FireworkExplosion.Shape.BURST,
      Items.GOLD_NUGGET,
      FireworkExplosion.Shape.STAR,
      Items.SKELETON_SKULL,
      FireworkExplosion.Shape.CREEPER,
      Items.WITHER_SKELETON_SKULL,
      FireworkExplosion.Shape.CREEPER,
      Items.CREEPER_HEAD,
      FireworkExplosion.Shape.CREEPER,
      Items.PLAYER_HEAD,
      FireworkExplosion.Shape.CREEPER,
      Items.DRAGON_HEAD,
      FireworkExplosion.Shape.CREEPER,
      Items.ZOMBIE_HEAD,
      FireworkExplosion.Shape.CREEPER,
      Items.PIGLIN_HEAD,
      FireworkExplosion.Shape.CREEPER
   );
   private static final Ingredient TRAIL_INGREDIENT = Ingredient.of(Items.DIAMOND);
   private static final Ingredient TWINKLE_INGREDIENT = Ingredient.of(Items.GLOWSTONE_DUST);
   private static final Ingredient GUNPOWDER_INGREDIENT = Ingredient.of(Items.GUNPOWDER);

   public FireworkStarRecipe(CraftingBookCategory p_251577_) {
      super(p_251577_);
   }

   public boolean matches(CraftingInput p_342190_, Level p_43896_) {
      if (p_342190_.ingredientCount() < 2) {
         return false;
      }

      boolean flag = false;
      boolean flag1 = false;
      boolean flag2 = false;
      boolean flag3 = false;
      boolean flag4 = false;

      for (int i = 0; i < p_342190_.size(); i++) {
         ItemStack itemstack = p_342190_.getItem(i);
         if (!itemstack.isEmpty()) {
            if (SHAPE_BY_ITEM.containsKey(itemstack.getItem())) {
               if (flag2) {
                  return false;
               }

               flag2 = true;
            } else if (TWINKLE_INGREDIENT.test(itemstack)) {
               if (flag4) {
                  return false;
               }

               flag4 = true;
            } else if (TRAIL_INGREDIENT.test(itemstack)) {
               if (flag3) {
                  return false;
               }

               flag3 = true;
            } else if (GUNPOWDER_INGREDIENT.test(itemstack)) {
               if (flag) {
                  return false;
               }

               flag = true;
            } else {
               if (!(itemstack.getItem() instanceof DyeItem)) {
                  return false;
               }

               flag1 = true;
            }
         }
      }

      return flag && flag1;
   }

   public ItemStack assemble(CraftingInput p_344010_, HolderLookup.Provider p_335220_) {
      FireworkExplosion.Shape fireworkexplosion$shape = FireworkExplosion.Shape.SMALL_BALL;
      boolean flag = false;
      boolean flag1 = false;
      IntList intlist = new IntArrayList();

      for (int i = 0; i < p_344010_.size(); i++) {
         ItemStack itemstack = p_344010_.getItem(i);
         if (!itemstack.isEmpty()) {
            FireworkExplosion.Shape fireworkexplosion$shape1 = SHAPE_BY_ITEM.get(itemstack.getItem());
            if (fireworkexplosion$shape1 != null) {
               fireworkexplosion$shape = fireworkexplosion$shape1;
            } else if (TWINKLE_INGREDIENT.test(itemstack)) {
               flag = true;
            } else if (TRAIL_INGREDIENT.test(itemstack)) {
               flag1 = true;
            } else if (itemstack.getItem() instanceof DyeItem dyeitem) {
               intlist.add(dyeitem.getDyeColor().getFireworkColor());
            }
         }
      }

      ItemStack itemstack1 = new ItemStack(Items.FIREWORK_STAR);
      itemstack1.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(fireworkexplosion$shape, intlist, IntList.of(), flag1, flag));
      return itemstack1;
   }

   @Override
   public RecipeSerializer<FireworkStarRecipe> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR;
   }
}
