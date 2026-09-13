package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EnchantingTableBlockEntity extends BlockEntity implements Nameable {
   private static final Component DEFAULT_NAME = Component.translatable("container.enchant");
   public int time;
   public float flip;
   public float oFlip;
   public float flipT;
   public float flipA;
   public float open;
   public float oOpen;
   public float rot;
   public float oRot;
   public float tRot;
   private static final RandomSource RANDOM = RandomSource.create();
   private @Nullable Component name;

   public EnchantingTableBlockEntity(BlockPos p_329912_, BlockState p_331662_) {
      super(BlockEntityType.ENCHANTING_TABLE, p_329912_, p_331662_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_407657_) {
      super.saveAdditional(p_407657_);
      p_407657_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
   }

   @Override
   protected void loadAdditional(ValueInput p_406753_) {
      super.loadAdditional(p_406753_);
      this.name = parseCustomNameSafe(p_406753_, "CustomName");
   }

   public static void bookAnimationTick(Level p_334676_, BlockPos p_332815_, BlockState p_332072_, EnchantingTableBlockEntity p_333258_) {
      p_333258_.oOpen = p_333258_.open;
      p_333258_.oRot = p_333258_.rot;
      Player player = p_334676_.getNearestPlayer(p_332815_.getX() + 0.5, p_332815_.getY() + 0.5, p_332815_.getZ() + 0.5, 3.0, false);
      if (player != null) {
         double d0 = player.getX() - (p_332815_.getX() + 0.5);
         double d1 = player.getZ() - (p_332815_.getZ() + 0.5);
         p_333258_.tRot = (float)Mth.atan2(d1, d0);
         p_333258_.open += 0.1F;
         if (p_333258_.open < 0.5F || RANDOM.nextInt(40) == 0) {
            float f1 = p_333258_.flipT;

            do {
               p_333258_.flipT = p_333258_.flipT + (RANDOM.nextInt(4) - RANDOM.nextInt(4));
            } while (f1 == p_333258_.flipT);
         }
      } else {
         p_333258_.tRot += 0.02F;
         p_333258_.open -= 0.1F;
      }

      while (p_333258_.rot >= (float) Math.PI) {
         p_333258_.rot -= (float) (Math.PI * 2);
      }

      while (p_333258_.rot < (float) -Math.PI) {
         p_333258_.rot += (float) (Math.PI * 2);
      }

      while (p_333258_.tRot >= (float) Math.PI) {
         p_333258_.tRot -= (float) (Math.PI * 2);
      }

      while (p_333258_.tRot < (float) -Math.PI) {
         p_333258_.tRot += (float) (Math.PI * 2);
      }

      float f2 = p_333258_.tRot - p_333258_.rot;

      while (f2 >= (float) Math.PI) {
         f2 -= (float) (Math.PI * 2);
      }

      while (f2 < (float) -Math.PI) {
         f2 += (float) (Math.PI * 2);
      }

      p_333258_.rot += f2 * 0.4F;
      p_333258_.open = Mth.clamp(p_333258_.open, 0.0F, 1.0F);
      p_333258_.time++;
      p_333258_.oFlip = p_333258_.flip;
      float f = (p_333258_.flipT - p_333258_.flip) * 0.4F;
      float f3 = 0.2F;
      f = Mth.clamp(f, -0.2F, 0.2F);
      p_333258_.flipA = p_333258_.flipA + (f - p_333258_.flipA) * 0.9F;
      p_333258_.flip = p_333258_.flip + p_333258_.flipA;
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   public void setCustomName(@Nullable Component p_330108_) {
      this.name = p_330108_;
   }

   @Override
   public @Nullable Component getCustomName() {
      return this.name;
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_397172_) {
      super.applyImplicitComponents(p_397172_);
      this.name = p_397172_.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_334287_) {
      super.collectImplicitComponents(p_334287_);
      p_334287_.set(DataComponents.CUSTOM_NAME, this.name);
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_409262_) {
      p_409262_.discard("CustomName");
   }
}
