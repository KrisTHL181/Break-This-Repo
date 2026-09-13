package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractCow extends Animal {
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.COW.getDimensions().scale(0.5F).withEyeHeight(0.665F);

   public AbstractCow(EntityType<? extends AbstractCow> p_455086_, Level p_452770_) {
      super(p_455086_, p_452770_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, p_460795_ -> p_460795_.is(ItemTags.COW_FOOD), false));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   @Override
   public boolean isFood(ItemStack p_456802_) {
      return p_456802_.is(ItemTags.COW_FOOD);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.COW_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_458948_) {
      return SoundEvents.COW_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.COW_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_451030_, BlockState p_460186_) {
      this.playSound(SoundEvents.COW_STEP, 0.15F, 1.0F);
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   public InteractionResult mobInteract(Player p_457035_, InteractionHand p_457450_) {
      ItemStack itemstack = p_457035_.getItemInHand(p_457450_);
      if (itemstack.is(Items.BUCKET) && !this.isBaby()) {
         p_457035_.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
         ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, p_457035_, Items.MILK_BUCKET.getDefaultInstance());
         p_457035_.setItemInHand(p_457450_, itemstack1);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_457035_, p_457450_);
      }
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_452371_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_452371_);
   }
}
