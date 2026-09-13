package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class EnchantmentHelper {
   public static int getItemEnchantmentLevel(Holder<Enchantment> p_344652_, ItemStack p_44845_) {
      ItemEnchantments itemenchantments = p_44845_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
      return itemenchantments.getLevel(p_344652_);
   }

   public static ItemEnchantments updateEnchantments(ItemStack p_333740_, Consumer<ItemEnchantments.Mutable> p_328467_) {
      DataComponentType<ItemEnchantments> datacomponenttype = getComponentType(p_333740_);
      ItemEnchantments itemenchantments = p_333740_.get(datacomponenttype);
      if (itemenchantments == null) {
         return ItemEnchantments.EMPTY;
      }

      ItemEnchantments.Mutable itemenchantments$mutable = new ItemEnchantments.Mutable(itemenchantments);
      p_328467_.accept(itemenchantments$mutable);
      ItemEnchantments itemenchantments1 = itemenchantments$mutable.toImmutable();
      p_333740_.set(datacomponenttype, itemenchantments1);
      return itemenchantments1;
   }

   public static boolean canStoreEnchantments(ItemStack p_333572_) {
      return p_333572_.has(getComponentType(p_333572_));
   }

   public static void setEnchantments(ItemStack p_44867_, ItemEnchantments p_330134_) {
      p_44867_.set(getComponentType(p_44867_), p_330134_);
   }

   public static ItemEnchantments getEnchantmentsForCrafting(ItemStack p_335659_) {
      return p_335659_.getOrDefault(getComponentType(p_335659_), ItemEnchantments.EMPTY);
   }

   private static DataComponentType<ItemEnchantments> getComponentType(ItemStack p_335414_) {
      return p_335414_.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
   }

   public static boolean hasAnyEnchantments(ItemStack p_335287_) {
      return !p_335287_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
         || !p_335287_.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
   }

   public static int processDurabilityChange(ServerLevel p_344040_, ItemStack p_345474_, int p_342600_) {
      MutableFloat mutablefloat = new MutableFloat(p_342600_);
      runIterationOnItem(p_345474_, (p_341764_, p_341765_) -> p_341764_.value().modifyDurabilityChange(p_344040_, p_341765_, p_345474_, mutablefloat));
      return mutablefloat.intValue();
   }

   public static int processAmmoUse(ServerLevel p_344585_, ItemStack p_344182_, ItemStack p_343578_, int p_342951_) {
      MutableFloat mutablefloat = new MutableFloat(p_342951_);
      runIterationOnItem(p_344182_, (p_341622_, p_341623_) -> p_341622_.value().modifyAmmoCount(p_344585_, p_341623_, p_343578_, mutablefloat));
      return mutablefloat.intValue();
   }

   public static int processBlockExperience(ServerLevel p_343042_, ItemStack p_343624_, int p_342499_) {
      MutableFloat mutablefloat = new MutableFloat(p_342499_);
      runIterationOnItem(p_343624_, (p_341808_, p_341809_) -> p_341808_.value().modifyBlockExperience(p_343042_, p_341809_, p_343624_, mutablefloat));
      return mutablefloat.intValue();
   }

   public static int processMobExperience(ServerLevel p_343500_, @Nullable Entity p_345230_, Entity p_344218_, int p_342604_) {
      if (p_345230_ instanceof LivingEntity livingentity) {
         MutableFloat mutablefloat = new MutableFloat(p_342604_);
         runIterationOnEquipment(
            livingentity,
            (p_341777_, p_341778_, p_341779_) -> p_341777_.value().modifyMobExperience(p_343500_, p_341778_, p_341779_.itemStack(), p_344218_, mutablefloat)
         );
         return mutablefloat.intValue();
      } else {
         return p_342604_;
      }
   }

   public static ItemStack createBook(EnchantmentInstance p_361083_) {
      ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
      itemstack.enchant(p_361083_.enchantment(), p_361083_.level());
      return itemstack;
   }

   private static void runIterationOnItem(ItemStack p_343610_, EnchantmentHelper.EnchantmentVisitor p_342837_) {
      ItemEnchantments itemenchantments = p_343610_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

      for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
         p_342837_.accept((Holder<Enchantment>)entry.getKey(), entry.getIntValue());
      }
   }

   private static void runIterationOnItem(
      ItemStack p_44852_, EquipmentSlot p_344793_, LivingEntity p_344959_, EnchantmentHelper.EnchantmentInSlotVisitor p_342058_
   ) {
      if (!p_44852_.isEmpty()) {
         ItemEnchantments itemenchantments = p_44852_.get(DataComponents.ENCHANTMENTS);
         if (itemenchantments != null && !itemenchantments.isEmpty()) {
            EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(p_44852_, p_344793_, p_344959_);

            for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
               Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
               if (holder.value().matchingSlot(p_344793_)) {
                  p_342058_.accept(holder, entry.getIntValue(), enchantediteminuse);
               }
            }
         }
      }
   }

   private static void runIterationOnEquipment(LivingEntity p_344171_, EnchantmentHelper.EnchantmentInSlotVisitor p_343067_) {
      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         runIterationOnItem(p_344171_.getItemBySlot(equipmentslot), equipmentslot, p_344171_, p_343067_);
      }
   }

   public static boolean isImmuneToDamage(ServerLevel p_343151_, LivingEntity p_344523_, DamageSource p_343996_) {
      MutableBoolean mutableboolean = new MutableBoolean();
      runIterationOnEquipment(
         p_344523_,
         (p_341729_, p_341730_, p_341731_) -> mutableboolean.setValue(
            mutableboolean.isTrue() || p_341729_.value().isImmuneToDamage(p_343151_, p_341730_, p_344523_, p_343996_)
         )
      );
      return mutableboolean.isTrue();
   }

   public static float getDamageProtection(ServerLevel p_345416_, LivingEntity p_342248_, DamageSource p_44858_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnEquipment(
         p_342248_,
         (p_341717_, p_341718_, p_341719_) -> p_341717_.value()
            .modifyDamageProtection(p_345416_, p_341718_, p_341719_.itemStack(), p_342248_, p_44858_, mutablefloat)
      );
      return mutablefloat.floatValue();
   }

   public static float modifyDamage(ServerLevel p_343245_, ItemStack p_342430_, Entity p_344044_, DamageSource p_344705_, float p_344247_) {
      MutableFloat mutablefloat = new MutableFloat(p_344247_);
      runIterationOnItem(
         p_342430_, (p_341744_, p_341745_) -> p_341744_.value().modifyDamage(p_343245_, p_341745_, p_342430_, p_344044_, p_344705_, mutablefloat)
      );
      return mutablefloat.floatValue();
   }

   public static float modifyFallBasedDamage(ServerLevel p_345393_, ItemStack p_344524_, Entity p_343535_, DamageSource p_343627_, float p_342940_) {
      MutableFloat mutablefloat = new MutableFloat(p_342940_);
      runIterationOnItem(
         p_344524_, (p_341771_, p_341772_) -> p_341771_.value().modifyFallBasedDamage(p_345393_, p_341772_, p_344524_, p_343535_, p_343627_, mutablefloat)
      );
      return mutablefloat.floatValue();
   }

   public static float modifyArmorEffectiveness(ServerLevel p_345408_, ItemStack p_344868_, Entity p_345361_, DamageSource p_343275_, float p_345487_) {
      MutableFloat mutablefloat = new MutableFloat(p_345487_);
      runIterationOnItem(
         p_344868_, (p_341681_, p_341682_) -> p_341681_.value().modifyArmorEffectivness(p_345408_, p_341682_, p_344868_, p_345361_, p_343275_, mutablefloat)
      );
      return mutablefloat.floatValue();
   }

   public static float modifyKnockback(ServerLevel p_344591_, ItemStack p_345053_, Entity p_343711_, DamageSource p_344321_, float p_343554_) {
      MutableFloat mutablefloat = new MutableFloat(p_343554_);
      runIterationOnItem(
         p_345053_, (p_341790_, p_341791_) -> p_341790_.value().modifyKnockback(p_344591_, p_341791_, p_345053_, p_343711_, p_344321_, mutablefloat)
      );
      return mutablefloat.floatValue();
   }

   public static void doPostAttackEffects(ServerLevel p_343618_, Entity p_343098_, DamageSource p_342187_) {
      if (p_342187_.getEntity() instanceof LivingEntity livingentity) {
         doPostAttackEffectsWithItemSource(p_343618_, p_343098_, p_342187_, livingentity.getWeaponItem());
      } else {
         doPostAttackEffectsWithItemSource(p_343618_, p_343098_, p_342187_, null);
      }
   }

   public static void doLungeEffects(ServerLevel p_455218_, Entity p_453041_) {
      if (p_453041_ instanceof LivingEntity livingentity) {
         runIterationOnItem(
            p_453041_.getWeaponItem(),
            EquipmentSlot.MAINHAND,
            livingentity,
            (p_449861_, p_449862_, p_449863_) -> p_449861_.value().doLunge(p_455218_, p_449862_, p_449863_, p_453041_)
         );
      }
   }

   public static void doPostAttackEffectsWithItemSource(ServerLevel p_345038_, Entity p_342420_, DamageSource p_344777_, @Nullable ItemStack p_344587_) {
      doPostAttackEffectsWithItemSourceOnBreak(p_345038_, p_342420_, p_344777_, p_344587_, null);
   }

   public static void doPostAttackEffectsWithItemSourceOnBreak(
      ServerLevel p_366803_, Entity p_366526_, DamageSource p_361714_, @Nullable ItemStack p_361802_, @Nullable Consumer<Item> p_364917_
   ) {
      if (p_366526_ instanceof LivingEntity livingentity) {
         runIterationOnEquipment(
            livingentity,
            (p_341753_, p_341754_, p_341755_) -> p_341753_.value()
               .doPostAttack(p_366803_, p_341754_, p_341755_, EnchantmentTarget.VICTIM, p_366526_, p_361714_)
         );
      }

      if (p_361802_ != null) {
         if (p_361714_.getEntity() instanceof LivingEntity livingentity1) {
            runIterationOnItem(
               p_361802_,
               EquipmentSlot.MAINHAND,
               livingentity1,
               (p_341641_, p_341642_, p_341643_) -> p_341641_.value()
                  .doPostAttack(p_366803_, p_341642_, p_341643_, EnchantmentTarget.ATTACKER, p_366526_, p_361714_)
            );
         } else if (p_364917_ != null) {
            EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(p_361802_, null, null, p_364917_);
            runIterationOnItem(
               p_361802_,
               (p_359919_, p_359920_) -> p_359919_.value()
                  .doPostAttack(p_366803_, p_359920_, enchantediteminuse, EnchantmentTarget.ATTACKER, p_366526_, p_361714_)
            );
         }
      }
   }

   public static void runLocationChangedEffects(ServerLevel p_342390_, LivingEntity p_344486_) {
      runIterationOnEquipment(
         p_344486_, (p_341602_, p_341603_, p_341604_) -> p_341602_.value().runLocationChangedEffects(p_342390_, p_341603_, p_341604_, p_344486_)
      );
   }

   public static void runLocationChangedEffects(ServerLevel p_342666_, ItemStack p_342169_, LivingEntity p_343458_, EquipmentSlot p_344449_) {
      runIterationOnItem(
         p_342169_,
         p_344449_,
         p_343458_,
         (p_341794_, p_341795_, p_341796_) -> p_341794_.value().runLocationChangedEffects(p_342666_, p_341795_, p_341796_, p_343458_)
      );
   }

   public static void stopLocationBasedEffects(LivingEntity p_342428_) {
      runIterationOnEquipment(p_342428_, (p_341606_, p_341607_, p_341608_) -> p_341606_.value().stopLocationBasedEffects(p_341607_, p_341608_, p_342428_));
   }

   public static void stopLocationBasedEffects(ItemStack p_343782_, LivingEntity p_342864_, EquipmentSlot p_342427_) {
      runIterationOnItem(
         p_343782_, p_342427_, p_342864_, (p_341625_, p_341626_, p_341627_) -> p_341625_.value().stopLocationBasedEffects(p_341626_, p_341627_, p_342864_)
      );
   }

   public static void tickEffects(ServerLevel p_344571_, LivingEntity p_343172_) {
      runIterationOnEquipment(p_343172_, (p_341782_, p_341783_, p_341784_) -> p_341782_.value().tick(p_344571_, p_341783_, p_341784_, p_343172_));
   }

   public static int getEnchantmentLevel(Holder<Enchantment> p_342592_, LivingEntity p_44838_) {
      Iterable<ItemStack> iterable = p_342592_.value().getSlotItems(p_44838_).values();
      int i = 0;

      for (ItemStack itemstack : iterable) {
         int j = getItemEnchantmentLevel(p_342592_, itemstack);
         if (j > i) {
            i = j;
         }
      }

      return i;
   }

   public static int processProjectileCount(ServerLevel p_344575_, ItemStack p_345314_, Entity p_343374_, int p_343111_) {
      MutableFloat mutablefloat = new MutableFloat(p_343111_);
      runIterationOnItem(p_345314_, (p_341617_, p_341618_) -> p_341617_.value().modifyProjectileCount(p_344575_, p_341618_, p_345314_, p_343374_, mutablefloat));
      return Math.max(0, mutablefloat.intValue());
   }

   public static float processProjectileSpread(ServerLevel p_342105_, ItemStack p_345162_, Entity p_343316_, float p_342659_) {
      MutableFloat mutablefloat = new MutableFloat(p_342659_);
      runIterationOnItem(
         p_345162_, (p_341674_, p_341675_) -> p_341674_.value().modifyProjectileSpread(p_342105_, p_341675_, p_345162_, p_343316_, mutablefloat)
      );
      return Math.max(0.0F, mutablefloat.floatValue());
   }

   public static int getPiercingCount(ServerLevel p_343271_, ItemStack p_345451_, ItemStack p_343657_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnItem(p_345451_, (p_341723_, p_341724_) -> p_341723_.value().modifyPiercingCount(p_343271_, p_341724_, p_343657_, mutablefloat));
      return Math.max(0, mutablefloat.intValue());
   }

   public static void onProjectileSpawned(ServerLevel p_343338_, ItemStack p_344853_, Projectile p_367369_, Consumer<Item> p_345317_) {
      LivingEntity livingentity = p_367369_.getOwner() instanceof LivingEntity livingentity1 ? livingentity1 : null;
      EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(p_344853_, null, livingentity, p_345317_);
      runIterationOnItem(p_344853_, (p_341759_, p_341760_) -> p_341759_.value().onProjectileSpawned(p_343338_, p_341760_, enchantediteminuse, p_367369_));
   }

   public static void onHitBlock(
      ServerLevel p_344864_,
      ItemStack p_342595_,
      @Nullable LivingEntity p_345505_,
      Entity p_345420_,
      @Nullable EquipmentSlot p_343177_,
      Vec3 p_343033_,
      BlockState p_343989_,
      Consumer<Item> p_344574_
   ) {
      EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(p_342595_, p_343177_, p_345505_, p_344574_);
      runIterationOnItem(
         p_342595_, (p_341663_, p_341664_) -> p_341663_.value().onHitBlock(p_344864_, p_341664_, enchantediteminuse, p_345420_, p_343033_, p_343989_)
      );
   }

   public static int modifyDurabilityToRepairFromXp(ServerLevel p_345080_, ItemStack p_343144_, int p_342792_) {
      MutableFloat mutablefloat = new MutableFloat(p_342792_);
      runIterationOnItem(p_343144_, (p_341656_, p_341657_) -> p_341656_.value().modifyDurabilityToRepairFromXp(p_345080_, p_341657_, p_343144_, mutablefloat));
      return Math.max(0, mutablefloat.intValue());
   }

   public static float processEquipmentDropChance(ServerLevel p_342296_, LivingEntity p_342126_, DamageSource p_344732_, float p_343626_) {
      MutableFloat mutablefloat = new MutableFloat(p_343626_);
      RandomSource randomsource = p_342126_.getRandom();
      runIterationOnEquipment(p_342126_, (p_341693_, p_341694_, p_341695_) -> {
         LootContext lootcontext = Enchantment.damageContext(p_342296_, p_341694_, p_342126_, p_344732_);
         p_341693_.value().getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(p_341820_ -> {
            if (p_341820_.enchanted() == EnchantmentTarget.VICTIM && p_341820_.affected() == EnchantmentTarget.VICTIM && p_341820_.matches(lootcontext)) {
               mutablefloat.setValue(p_341820_.effect().process(p_341694_, randomsource, mutablefloat.floatValue()));
            }
         });
      });
      if (p_344732_.getEntity() instanceof LivingEntity livingentity) {
         runIterationOnEquipment(livingentity, (p_341650_, p_341651_, p_341652_) -> {
            LootContext lootcontext = Enchantment.damageContext(p_342296_, p_341651_, p_342126_, p_344732_);
            p_341650_.value().getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(p_341669_ -> {
               if (p_341669_.enchanted() == EnchantmentTarget.ATTACKER && p_341669_.affected() == EnchantmentTarget.VICTIM && p_341669_.matches(lootcontext)) {
                  mutablefloat.setValue(p_341669_.effect().process(p_341651_, randomsource, mutablefloat.floatValue()));
               }
            });
         });
      }

      return mutablefloat.floatValue();
   }

   public static void forEachModifier(ItemStack p_344460_, EquipmentSlotGroup p_343938_, BiConsumer<Holder<Attribute>, AttributeModifier> p_345426_) {
      runIterationOnItem(p_344460_, (p_341748_, p_341749_) -> p_341748_.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(p_341738_ -> {
         if (((Enchantment)p_341748_.value()).definition().slots().contains(p_343938_)) {
            p_345426_.accept(p_341738_.attribute(), p_341738_.getModifier(p_341749_, p_343938_));
         }
      }));
   }

   public static void forEachModifier(ItemStack p_343035_, EquipmentSlot p_342305_, BiConsumer<Holder<Attribute>, AttributeModifier> p_342639_) {
      runIterationOnItem(p_343035_, (p_341598_, p_341599_) -> p_341598_.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(p_341804_ -> {
         if (((Enchantment)p_341598_.value()).matchingSlot(p_342305_)) {
            p_342639_.accept(p_341804_.attribute(), p_341804_.getModifier(p_341599_, p_342305_));
         }
      }));
   }

   public static int getFishingLuckBonus(ServerLevel p_345183_, ItemStack p_44905_, Entity p_344199_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnItem(p_44905_, (p_341704_, p_341705_) -> p_341704_.value().modifyFishingLuckBonus(p_345183_, p_341705_, p_44905_, p_344199_, mutablefloat));
      return Math.max(0, mutablefloat.intValue());
   }

   public static float getFishingTimeReduction(ServerLevel p_344336_, ItemStack p_343914_, Entity p_342898_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnItem(
         p_343914_, (p_341814_, p_341815_) -> p_341814_.value().modifyFishingTimeReduction(p_344336_, p_341815_, p_343914_, p_342898_, mutablefloat)
      );
      return Math.max(0.0F, mutablefloat.floatValue());
   }

   public static int getTridentReturnToOwnerAcceleration(ServerLevel p_342510_, ItemStack p_342608_, Entity p_343773_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnItem(
         p_342608_,
         (p_341632_, p_341633_) -> p_341632_.value().modifyTridentReturnToOwnerAcceleration(p_342510_, p_341633_, p_342608_, p_343773_, mutablefloat)
      );
      return Math.max(0, mutablefloat.intValue());
   }

   public static float modifyCrossbowChargingTime(ItemStack p_344573_, LivingEntity p_343136_, float p_343873_) {
      MutableFloat mutablefloat = new MutableFloat(p_343873_);
      runIterationOnItem(p_344573_, (p_449866_, p_449867_) -> p_449866_.value().modifyCrossbowChargeTime(p_343136_.getRandom(), p_449867_, mutablefloat));
      return Math.max(0.0F, mutablefloat.floatValue());
   }

   public static float getTridentSpinAttackStrength(ItemStack p_345397_, LivingEntity p_342067_) {
      MutableFloat mutablefloat = new MutableFloat(0.0F);
      runIterationOnItem(p_345397_, (p_449857_, p_449858_) -> p_449857_.value().modifyTridentSpinAttackStrength(p_342067_.getRandom(), p_449858_, mutablefloat));
      return mutablefloat.floatValue();
   }

   public static boolean hasTag(ItemStack p_344479_, TagKey<Enchantment> p_343396_) {
      ItemEnchantments itemenchantments = p_344479_.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

      for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
         Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
         if (holder.is(p_343396_)) {
            return true;
         }
      }

      return false;
   }

   public static boolean has(ItemStack p_345483_, DataComponentType<?> p_344623_) {
      MutableBoolean mutableboolean = new MutableBoolean(false);
      runIterationOnItem(p_345483_, (p_341711_, p_341712_) -> {
         if (p_341711_.value().effects().has(p_344623_)) {
            mutableboolean.setTrue();
         }
      });
      return mutableboolean.booleanValue();
   }

   public static <T> Optional<T> pickHighestLevel(ItemStack p_343484_, DataComponentType<List<T>> p_342070_) {
      Pair<List<T>, Integer> pair = getHighestLevel(p_343484_, p_342070_);
      if (pair != null) {
         List<T> list = (List<T>)pair.getFirst();
         int i = (Integer)pair.getSecond();
         return Optional.of(list.get(Math.min(i, list.size()) - 1));
      } else {
         return Optional.empty();
      }
   }

   public static <T> Pair<T, Integer> getHighestLevel(ItemStack p_345335_, DataComponentType<T> p_344437_) {
      MutableObject<Pair<T, Integer>> mutableobject = new MutableObject();
      runIterationOnItem(p_345335_, (p_449853_, p_449854_) -> {
         if (mutableobject.get() == null || (Integer)((Pair)mutableobject.get()).getSecond() < p_449854_) {
            T t = p_449853_.value().effects().get(p_344437_);
            if (t != null) {
               mutableobject.setValue(Pair.of(t, p_449854_));
            }
         }
      });
      return (Pair<T, Integer>)mutableobject.get();
   }

   public static Optional<EnchantedItemInUse> getRandomItemWith(DataComponentType<?> p_345106_, LivingEntity p_44908_, Predicate<ItemStack> p_345112_) {
      List<EnchantedItemInUse> list = new ArrayList<>();

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = p_44908_.getItemBySlot(equipmentslot);
         if (p_345112_.test(itemstack)) {
            ItemEnchantments itemenchantments = itemstack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
               Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
               if (holder.value().effects().has(p_345106_) && holder.value().matchingSlot(equipmentslot)) {
                  list.add(new EnchantedItemInUse(itemstack, equipmentslot, p_44908_));
               }
            }
         }
      }

      return Util.getRandomSafe(list, p_44908_.getRandom());
   }

   public static int getEnchantmentCost(RandomSource p_220288_, int p_220289_, int p_220290_, ItemStack p_220291_) {
      Enchantable enchantable = p_220291_.get(DataComponents.ENCHANTABLE);
      if (enchantable == null) {
         return 0;
      }

      if (p_220290_ > 15) {
         p_220290_ = 15;
      }

      int i = p_220288_.nextInt(8) + 1 + (p_220290_ >> 1) + p_220288_.nextInt(p_220290_ + 1);
      if (p_220289_ == 0) {
         return Math.max(i / 3, 1);
      } else {
         return p_220289_ == 1 ? i * 2 / 3 + 1 : Math.max(i, p_220290_ * 2);
      }
   }

   public static ItemStack enchantItem(
      RandomSource p_344212_, ItemStack p_345193_, int p_344120_, RegistryAccess p_345399_, Optional<? extends HolderSet<Enchantment>> p_342141_
   ) {
      return enchantItem(
         p_344212_,
         p_345193_,
         p_344120_,
         p_342141_.map(HolderSet::stream)
            .orElseGet(() -> p_345399_.lookupOrThrow(Registries.ENCHANTMENT).listElements().map(p_341773_ -> (Holder<Enchantment>)p_341773_))
      );
   }

   public static ItemStack enchantItem(RandomSource p_220293_, ItemStack p_220294_, int p_220295_, Stream<Holder<Enchantment>> p_344664_) {
      List<EnchantmentInstance> list = selectEnchantment(p_220293_, p_220294_, p_220295_, p_344664_);
      if (p_220294_.is(Items.BOOK)) {
         p_220294_ = new ItemStack(Items.ENCHANTED_BOOK);
      }

      for (EnchantmentInstance enchantmentinstance : list) {
         p_220294_.enchant(enchantmentinstance.enchantment(), enchantmentinstance.level());
      }

      return p_220294_;
   }

   public static List<EnchantmentInstance> selectEnchantment(RandomSource p_220298_, ItemStack p_220299_, int p_220300_, Stream<Holder<Enchantment>> p_342119_) {
      List<EnchantmentInstance> list = Lists.newArrayList();
      Enchantable enchantable = p_220299_.get(DataComponents.ENCHANTABLE);
      if (enchantable == null) {
         return list;
      }

      p_220300_ += 1 + p_220298_.nextInt(enchantable.value() / 4 + 1) + p_220298_.nextInt(enchantable.value() / 4 + 1);
      float f = (p_220298_.nextFloat() + p_220298_.nextFloat() - 1.0F) * 0.15F;
      p_220300_ = Mth.clamp(Math.round(p_220300_ + p_220300_ * f), 1, Integer.MAX_VALUE);
      List<EnchantmentInstance> list1 = getAvailableEnchantmentResults(p_220300_, p_220299_, p_342119_);
      if (!list1.isEmpty()) {
         WeightedRandom.getRandomItem(p_220298_, list1, EnchantmentInstance::weight).ifPresent(list::add);

         while (p_220298_.nextInt(50) <= p_220300_) {
            if (!list.isEmpty()) {
               filterCompatibleEnchantments(list1, list.getLast());
            }

            if (list1.isEmpty()) {
               break;
            }

            WeightedRandom.getRandomItem(p_220298_, list1, EnchantmentInstance::weight).ifPresent(list::add);
            p_220300_ /= 2;
         }
      }

      return list;
   }

   public static void filterCompatibleEnchantments(List<EnchantmentInstance> p_44863_, EnchantmentInstance p_44864_) {
      p_44863_.removeIf(p_390856_ -> !Enchantment.areCompatible(p_44864_.enchantment(), p_390856_.enchantment()));
   }

   public static boolean isEnchantmentCompatible(Collection<Holder<Enchantment>> p_44860_, Holder<Enchantment> p_345339_) {
      for (Holder<Enchantment> holder : p_44860_) {
         if (!Enchantment.areCompatible(holder, p_345339_)) {
            return false;
         }
      }

      return true;
   }

   public static List<EnchantmentInstance> getAvailableEnchantmentResults(int p_44818_, ItemStack p_44819_, Stream<Holder<Enchantment>> p_342857_) {
      List<EnchantmentInstance> list = Lists.newArrayList();
      boolean flag = p_44819_.is(Items.BOOK);
      p_342857_.filter(p_341799_ -> p_341799_.value().isPrimaryItem(p_44819_) || flag).forEach(p_341708_ -> {
         Enchantment enchantment = p_341708_.value();

         for (int i = enchantment.getMaxLevel(); i >= enchantment.getMinLevel(); i--) {
            if (p_44818_ >= enchantment.getMinCost(i) && p_44818_ <= enchantment.getMaxCost(i)) {
               list.add(new EnchantmentInstance((Holder<Enchantment>)p_341708_, i));
               break;
            }
         }
      });
      return list;
   }

   public static void enchantItemFromProvider(
      ItemStack p_344649_, RegistryAccess p_345511_, ResourceKey<EnchantmentProvider> p_342294_, DifficultyInstance p_343182_, RandomSource p_344701_
   ) {
      EnchantmentProvider enchantmentprovider = p_345511_.lookupOrThrow(Registries.ENCHANTMENT_PROVIDER).getValue(p_342294_);
      if (enchantmentprovider != null) {
         updateEnchantments(p_344649_, p_341687_ -> enchantmentprovider.enchant(p_344649_, p_341687_, p_344701_, p_343182_));
      }
   }

   @FunctionalInterface
   interface EnchantmentInSlotVisitor {
      void accept(Holder<Enchantment> var1, int var2, EnchantedItemInUse var3);
   }

   @FunctionalInterface
   interface EnchantmentVisitor {
      void accept(Holder<Enchantment> var1, int var2);
   }
}
