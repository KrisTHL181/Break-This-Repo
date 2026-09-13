package net.minecraft.client.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LevelEventHandler {
   private final Minecraft minecraft;
   private final ClientLevel level;
   private final Map<BlockPos, SoundInstance> playingJukeboxSongs = new HashMap<>();

   public LevelEventHandler(Minecraft p_364747_, ClientLevel p_429149_) {
      this.minecraft = p_364747_;
      this.level = p_429149_;
   }

   public void globalLevelEvent(int p_364279_, BlockPos p_361580_, int p_363929_) {
      switch (p_364279_) {
         case 1023:
         case 1028:
         case 1038:
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            if (camera.isInitialized()) {
               Vec3 vec3 = Vec3.atCenterOf(p_361580_).subtract(camera.position()).normalize();
               Vec3 vec31 = camera.position().add(vec3.scale(2.0));
               if (p_364279_ == 1023) {
                  this.level.playLocalSound(vec31.x, vec31.y, vec31.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else if (p_364279_ == 1038) {
                  this.level.playLocalSound(vec31.x, vec31.y, vec31.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else {
                  this.level.playLocalSound(vec31.x, vec31.y, vec31.z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
               }
            }
      }
   }

   public void levelEvent(int p_368193_, BlockPos p_362689_, int p_368262_) {
      RandomSource randomsource = this.level.random;
      switch (p_368193_) {
         case 1000:
            this.level.playLocalSound(p_362689_, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1001:
            this.level.playLocalSound(p_362689_, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1002:
            this.level.playLocalSound(p_362689_, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1004:
            this.level.playLocalSound(p_362689_, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            break;
         case 1009:
            if (p_368262_ == 0) {
               this.level
                  .playLocalSound(
                     p_362689_,
                     SoundEvents.FIRE_EXTINGUISH,
                     SoundSource.BLOCKS,
                     0.5F,
                     2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F,
                     false
                  );
            } else if (p_368262_ == 1) {
               this.level
                  .playLocalSound(
                     p_362689_,
                     SoundEvents.GENERIC_EXTINGUISH_FIRE,
                     SoundSource.BLOCKS,
                     0.7F,
                     1.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.4F,
                     false
                  );
            }
            break;
         case 1010:
            this.level
               .registryAccess()
               .lookupOrThrow(Registries.JUKEBOX_SONG)
               .get(p_368262_)
               .ifPresent(p_368563_ -> this.playJukeboxSong(p_368563_, p_362689_));
            break;
         case 1011:
            this.stopJukeboxSongAndNotifyNearby(p_362689_);
            break;
         case 1015:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1016:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1017:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ENDER_DRAGON_SHOOT,
                  SoundSource.HOSTILE,
                  10.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1018:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1019:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1020:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1021:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1022:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.WITHER_BREAK_BLOCK,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1024:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1025:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1026:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1027:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1029:
            this.level.playLocalSound(p_362689_, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1030:
            this.level.playLocalSound(p_362689_, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1031:
            this.level.playLocalSound(p_362689_, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1032:
            this.minecraft
               .getSoundManager()
               .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomsource.nextFloat() * 0.4F + 0.8F, 0.25F));
            break;
         case 1033:
            this.level.playLocalSound(p_362689_, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1034:
            this.level.playLocalSound(p_362689_, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1035:
            this.level.playLocalSound(p_362689_, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1039:
            this.level.playLocalSound(p_362689_, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1040:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1041:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.HUSK_CONVERTED_TO_ZOMBIE,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1042:
            this.level.playLocalSound(p_362689_, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1043:
            this.level.playLocalSound(p_362689_, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1044:
            this.level.playLocalSound(p_362689_, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1045:
            this.level
               .playLocalSound(p_362689_, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1046:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON,
                  SoundSource.BLOCKS,
                  2.0F,
                  this.level.random.nextFloat() * 0.1F + 0.9F,
                  false
               );
            break;
         case 1047:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,
                  SoundSource.BLOCKS,
                  2.0F,
                  this.level.random.nextFloat() * 0.1F + 0.9F,
                  false
               );
            break;
         case 1048:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.SKELETON_CONVERTED_TO_STRAY,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1049:
            this.level.playLocalSound(p_362689_, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1050:
            this.level.playLocalSound(p_362689_, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1051:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false
               );
            break;
         case 1500:
            ComposterBlock.handleFill(this.level, p_362689_, p_368262_ > 0);
            break;
         case 1501:
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
               );

            for (int l2 = 0; l2 < 8; l2++) {
               this.level
                  .addParticle(
                     ParticleTypes.LARGE_SMOKE,
                     p_362689_.getX() + randomsource.nextDouble(),
                     p_362689_.getY() + 1.2,
                     p_362689_.getZ() + randomsource.nextDouble(),
                     0.0,
                     0.0,
                     0.0
                  );
            }
            break;
         case 1502:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.REDSTONE_TORCH_BURNOUT,
                  SoundSource.BLOCKS,
                  0.5F,
                  2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F,
                  false
               );

            for (int k2 = 0; k2 < 5; k2++) {
               double d13 = p_362689_.getX() + randomsource.nextDouble() * 0.6 + 0.2;
               double d18 = p_362689_.getY() + randomsource.nextDouble() * 0.6 + 0.2;
               double d23 = p_362689_.getZ() + randomsource.nextDouble() * 0.6 + 0.2;
               this.level.addParticle(ParticleTypes.SMOKE, d13, d18, d23, 0.0, 0.0, 0.0);
            }
            break;
         case 1503:
            this.level.playLocalSound(p_362689_, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

            for (int j2 = 0; j2 < 16; j2++) {
               double d12 = p_362689_.getX() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
               double d17 = p_362689_.getY() + 0.8125;
               double d22 = p_362689_.getZ() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
               this.level.addParticle(ParticleTypes.SMOKE, d12, d17, d22, 0.0, 0.0, 0.0);
            }
            break;
         case 1504:
            PointedDripstoneBlock.spawnDripParticle(this.level, p_362689_, this.level.getBlockState(p_362689_));
            break;
         case 1505:
            BoneMealItem.addGrowthParticles(this.level, p_362689_, p_368262_);
            this.level.playLocalSound(p_362689_, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 2000:
            this.shootParticles(p_368262_, p_362689_, randomsource, ParticleTypes.SMOKE);
            break;
         case 2001:
            BlockState blockstate1 = Block.stateById(p_368262_);
            if (!blockstate1.isAir()) {
               SoundType soundtype = blockstate1.getSoundType();
               this.level
                  .playLocalSound(
                     p_362689_, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false
                  );
            }

            this.level.addDestroyBlockEffect(p_362689_, blockstate1);
            break;
         case 2002:
         case 2007:
            Vec3 vec3 = Vec3.atBottomCenterOf(p_362689_);

            for (int j = 0; j < 8; j++) {
               this.level
                  .addParticle(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                     vec3.x,
                     vec3.y,
                     vec3.z,
                     randomsource.nextGaussian() * 0.15,
                     randomsource.nextDouble() * 0.2,
                     randomsource.nextGaussian() * 0.15
                  );
            }

            float f2 = (p_368262_ >> 16 & 0xFF) / 255.0F;
            float f3 = (p_368262_ >> 8 & 0xFF) / 255.0F;
            float f5 = (p_368262_ >> 0 & 0xFF) / 255.0F;
            ParticleType<SpellParticleOption> particletype = p_368193_ == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

            for (int i2 = 0; i2 < 100; i2++) {
               double d11 = randomsource.nextDouble() * 4.0;
               double d16 = randomsource.nextDouble() * Math.PI * 2.0;
               double d21 = Math.cos(d16) * d11;
               double d25 = 0.01 + randomsource.nextDouble() * 0.5;
               double d5 = Math.sin(d16) * d11;
               float f1 = 0.75F + randomsource.nextFloat() * 0.25F;
               SpellParticleOption spellparticleoption = SpellParticleOption.create(particletype, f2 * f1, f3 * f1, f5 * f1, (float)d11);
               this.level.addParticle(spellparticleoption, vec3.x + d21 * 0.1, vec3.y + 0.3, vec3.z + d5 * 0.1, d21, d25, d5);
            }

            this.level.playLocalSound(p_362689_, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 2003:
            double d0 = p_362689_.getX() + 0.5;
            double d6 = p_362689_.getY();
            double d8 = p_362689_.getZ() + 0.5;

            for (int i3 = 0; i3 < 8; i3++) {
               this.level
                  .addParticle(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                     d0,
                     d6,
                     d8,
                     randomsource.nextGaussian() * 0.15,
                     randomsource.nextDouble() * 0.2,
                     randomsource.nextGaussian() * 0.15
                  );
            }

            for (double d10 = 0.0; d10 < Math.PI * 2; d10 += Math.PI / 20) {
               this.level
                  .addParticle(
                     ParticleTypes.PORTAL, d0 + Math.cos(d10) * 5.0, d6 - 0.4, d8 + Math.sin(d10) * 5.0, Math.cos(d10) * -5.0, 0.0, Math.sin(d10) * -5.0
                  );
               this.level
                  .addParticle(
                     ParticleTypes.PORTAL, d0 + Math.cos(d10) * 5.0, d6 - 0.4, d8 + Math.sin(d10) * 5.0, Math.cos(d10) * -7.0, 0.0, Math.sin(d10) * -7.0
                  );
            }
            break;
         case 2004:
            for (int l = 0; l < 20; l++) {
               double d7 = p_362689_.getX() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
               double d9 = p_362689_.getY() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
               double d14 = p_362689_.getZ() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
               this.level.addParticle(ParticleTypes.SMOKE, d7, d9, d14, 0.0, 0.0, 0.0);
               this.level.addParticle(ParticleTypes.FLAME, d7, d9, d14, 0.0, 0.0, 0.0);
            }
            break;
         case 2006:
            for (int l1 = 0; l1 < 200; l1++) {
               float f10 = randomsource.nextFloat() * 4.0F;
               float f11 = randomsource.nextFloat() * (float) (Math.PI * 2);
               double d15 = Mth.cos(f11) * f10;
               double d20 = 0.01 + randomsource.nextDouble() * 0.5;
               double d24 = Mth.sin(f11) * f10;
               this.level
                  .addParticle(
                     PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, f10),
                     p_362689_.getX() + d15 * 0.1,
                     p_362689_.getY() + 0.3,
                     p_362689_.getZ() + d24 * 0.1,
                     d15,
                     d20,
                     d24
                  );
            }

            if (p_368262_ == 1) {
               this.level
                  .playLocalSound(p_362689_, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
            }
            break;
         case 2008:
            this.level.addParticle(ParticleTypes.EXPLOSION, p_362689_.getX() + 0.5, p_362689_.getY() + 0.5, p_362689_.getZ() + 0.5, 0.0, 0.0, 0.0);
            break;
         case 2009:
            for (int k1 = 0; k1 < 8; k1++) {
               this.level
                  .addParticle(
                     ParticleTypes.CLOUD,
                     p_362689_.getX() + randomsource.nextDouble(),
                     p_362689_.getY() + 1.2,
                     p_362689_.getZ() + randomsource.nextDouble(),
                     0.0,
                     0.0,
                     0.0
                  );
            }
            break;
         case 2010:
            this.shootParticles(p_368262_, p_362689_, randomsource, ParticleTypes.WHITE_SMOKE);
            break;
         case 2011:
            ParticleUtils.spawnParticleInBlock(this.level, p_362689_, p_368262_, ParticleTypes.HAPPY_VILLAGER);
            break;
         case 2012:
            ParticleUtils.spawnParticleInBlock(this.level, p_362689_, p_368262_, ParticleTypes.HAPPY_VILLAGER);
            break;
         case 2013:
            ParticleUtils.spawnSmashAttackParticles(this.level, p_362689_, p_368262_);
            break;
         case 3000:
            this.level
               .addAlwaysVisibleParticle(
                  ParticleTypes.EXPLOSION_EMITTER, true, p_362689_.getX() + 0.5, p_362689_.getY() + 0.5, p_362689_.getZ() + 0.5, 0.0, 0.0, 0.0
               );
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.END_GATEWAY_SPAWN,
                  SoundSource.BLOCKS,
                  10.0F,
                  (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                  false
               );
            break;
         case 3001:
            this.level
               .playLocalSound(p_362689_, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
            break;
         case 3002:
            if (p_368262_ >= 0 && p_368262_ < Direction.Axis.VALUES.length) {
               ParticleUtils.spawnParticlesAlongAxis(
                  Direction.Axis.VALUES[p_368262_], this.level, p_362689_, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19)
               );
            } else {
               ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_362689_, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
            }
            break;
         case 3003:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_362689_, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
            this.level.playLocalSound(p_362689_, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 3004:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_362689_, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
            break;
         case 3005:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_362689_, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
            break;
         case 3006:
            int k = p_368262_ >> 6;
            if (k > 0) {
               if (randomsource.nextFloat() < 0.3F + k * 0.1F) {
                  float f4 = 0.15F + 0.02F * k * k * randomsource.nextFloat();
                  float f6 = 0.4F + 0.3F * k * randomsource.nextFloat();
                  this.level.playLocalSound(p_362689_, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, f4, f6, false);
               }

               byte b0 = (byte)(p_368262_ & 63);
               IntProvider intprovider = UniformInt.of(0, k);
               float f7 = 0.005F;
               Supplier<Vec3> supplier = () -> new Vec3(
                  Mth.nextDouble(randomsource, -0.005F, 0.005F), Mth.nextDouble(randomsource, -0.005F, 0.005F), Mth.nextDouble(randomsource, -0.005F, 0.005F)
               );
               if (b0 == 0) {
                  for (Direction direction : Direction.values()) {
                     float f = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                     double d4 = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                     ParticleUtils.spawnParticlesOnBlockFace(this.level, p_362689_, new SculkChargeParticleOptions(f), intprovider, direction, supplier, d4);
                  }
               } else {
                  for (Direction direction1 : MultifaceBlock.unpack(b0)) {
                     float f13 = direction1 == Direction.UP ? (float) Math.PI : 0.0F;
                     double d19 = 0.35;
                     ParticleUtils.spawnParticlesOnBlockFace(
                        this.level, p_362689_, new SculkChargeParticleOptions(f13), intprovider, direction1, supplier, 0.35
                     );
                  }
               }
            } else {
               this.level.playLocalSound(p_362689_, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
               boolean flag1 = this.level.getBlockState(p_362689_).isCollisionShapeFullBlock(this.level, p_362689_);
               int j1 = flag1 ? 40 : 20;
               float f8 = flag1 ? 0.45F : 0.25F;
               float f9 = 0.07F;

               for (int j3 = 0; j3 < j1; j3++) {
                  float f12 = 2.0F * randomsource.nextFloat() - 1.0F;
                  float f14 = 2.0F * randomsource.nextFloat() - 1.0F;
                  float f15 = 2.0F * randomsource.nextFloat() - 1.0F;
                  this.level
                     .addParticle(
                        ParticleTypes.SCULK_CHARGE_POP,
                        p_362689_.getX() + 0.5 + f12 * f8,
                        p_362689_.getY() + 0.5 + f14 * f8,
                        p_362689_.getZ() + 0.5 + f15 * f8,
                        f12 * 0.07F,
                        f14 * 0.07F,
                        f15 * 0.07F
                     );
               }
            }
            break;
         case 3007:
            for (int i1 = 0; i1 < 10; i1++) {
               this.level
                  .addParticle(
                     new ShriekParticleOption(i1 * 5),
                     p_362689_.getX() + 0.5,
                     p_362689_.getY() + SculkShriekerBlock.TOP_Y,
                     p_362689_.getZ() + 0.5,
                     0.0,
                     0.0,
                     0.0
                  );
            }

            BlockState blockstate2 = this.level.getBlockState(p_362689_);
            boolean flag = blockstate2.hasProperty(BlockStateProperties.WATERLOGGED) && blockstate2.getValue(BlockStateProperties.WATERLOGGED);
            if (!flag) {
               this.level
                  .playLocalSound(
                     p_362689_.getX() + 0.5,
                     p_362689_.getY() + SculkShriekerBlock.TOP_Y,
                     p_362689_.getZ() + 0.5,
                     SoundEvents.SCULK_SHRIEKER_SHRIEK,
                     SoundSource.BLOCKS,
                     2.0F,
                     0.6F + this.level.random.nextFloat() * 0.4F,
                     false
                  );
            }
            break;
         case 3008:
            BlockState blockstate = Block.stateById(p_368262_);
            if (blockstate.getBlock() instanceof BrushableBlock brushableblock) {
               this.level.playLocalSound(p_362689_, brushableblock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
            }

            this.level.addDestroyBlockEffect(p_362689_, blockstate);
            break;
         case 3009:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, p_362689_, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
            break;
         case 3011:
            TrialSpawner.addSpawnParticles(this.level, p_362689_, randomsource, TrialSpawner.FlameParticle.decode(p_368262_).particleType);
            break;
         case 3012:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_SPAWN_MOB,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addSpawnParticles(this.level, p_362689_, randomsource, TrialSpawner.FlameParticle.decode(p_368262_).particleType);
            break;
         case 3013:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, p_362689_, randomsource, p_368262_, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
            break;
         case 3014:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_EJECT_ITEM,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addEjectItemParticles(this.level, p_362689_, randomsource);
            break;
         case 3015:
            if (this.level.getBlockEntity(p_362689_) instanceof VaultBlockEntity vaultblockentity) {
               VaultBlockEntity.Client.emitActivationParticles(
                  this.level,
                  vaultblockentity.getBlockPos(),
                  vaultblockentity.getBlockState(),
                  vaultblockentity.getSharedData(),
                  p_368262_ == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
               );
               this.level
                  .playLocalSound(
                     p_362689_, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                  );
            }
            break;
         case 3016:
            VaultBlockEntity.Client.emitDeactivationParticles(this.level, p_362689_, p_368262_ == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
               );
            break;
         case 3017:
            TrialSpawner.addEjectItemParticles(this.level, p_362689_, randomsource);
            break;
         case 3018:
            for (int i = 0; i < 10; i++) {
               double d1 = randomsource.nextGaussian() * 0.02;
               double d2 = randomsource.nextGaussian() * 0.02;
               double d3 = randomsource.nextGaussian() * 0.02;
               this.level
                  .addParticle(
                     ParticleTypes.POOF,
                     p_362689_.getX() + randomsource.nextDouble(),
                     p_362689_.getY() + randomsource.nextDouble(),
                     p_362689_.getZ() + randomsource.nextDouble(),
                     d1,
                     d2,
                     d3
                  );
            }

            this.level
               .playLocalSound(
                  p_362689_, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
               );
            break;
         case 3019:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, p_362689_, randomsource, p_368262_, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
            break;
         case 3020:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                  SoundSource.BLOCKS,
                  p_368262_ == 0 ? 0.3F : 1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, p_362689_, randomsource, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
            TrialSpawner.addBecomeOminousParticles(this.level, p_362689_, randomsource);
            break;
         case 3021:
            this.level
               .playLocalSound(
                  p_362689_,
                  SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addSpawnParticles(this.level, p_362689_, randomsource, TrialSpawner.FlameParticle.decode(p_368262_).particleType);
      }
   }

   private void shootParticles(int p_365984_, BlockPos p_363841_, RandomSource p_369818_, SimpleParticleType p_368421_) {
      Direction direction = Direction.from3DDataValue(p_365984_);
      int i = direction.getStepX();
      int j = direction.getStepY();
      int k = direction.getStepZ();

      for (int l = 0; l < 10; l++) {
         double d0 = p_369818_.nextDouble() * 0.2 + 0.01;
         double d1 = p_363841_.getX() + i * 0.6 + 0.5 + i * 0.01 + (p_369818_.nextDouble() - 0.5) * k * 0.5;
         double d2 = p_363841_.getY() + j * 0.6 + 0.5 + j * 0.01 + (p_369818_.nextDouble() - 0.5) * j * 0.5;
         double d3 = p_363841_.getZ() + k * 0.6 + 0.5 + k * 0.01 + (p_369818_.nextDouble() - 0.5) * i * 0.5;
         double d4 = i * d0 + p_369818_.nextGaussian() * 0.01;
         double d5 = j * d0 + p_369818_.nextGaussian() * 0.01;
         double d6 = k * d0 + p_369818_.nextGaussian() * 0.01;
         this.level.addParticle(p_368421_, d1, d2, d3, d4, d5, d6);
      }
   }

   private void playJukeboxSong(Holder<JukeboxSong> p_369005_, BlockPos p_367637_) {
      this.stopJukeboxSong(p_367637_);
      JukeboxSong jukeboxsong = p_369005_.value();
      SoundEvent soundevent = jukeboxsong.soundEvent().value();
      SoundInstance soundinstance = SimpleSoundInstance.forJukeboxSong(soundevent, Vec3.atCenterOf(p_367637_));
      this.playingJukeboxSongs.put(p_367637_, soundinstance);
      this.minecraft.getSoundManager().play(soundinstance);
      this.minecraft.gui.setNowPlaying(jukeboxsong.description());
      this.notifyNearbyEntities(this.level, p_367637_, true);
   }

   private void stopJukeboxSong(BlockPos p_364210_) {
      SoundInstance soundinstance = this.playingJukeboxSongs.remove(p_364210_);
      if (soundinstance != null) {
         this.minecraft.getSoundManager().stop(soundinstance);
      }
   }

   private void stopJukeboxSongAndNotifyNearby(BlockPos p_362905_) {
      this.stopJukeboxSong(p_362905_);
      this.notifyNearbyEntities(this.level, p_362905_, false);
   }

   private void notifyNearbyEntities(Level p_366051_, BlockPos p_362804_, boolean p_363434_) {
      for (LivingEntity livingentity : p_366051_.getEntitiesOfClass(LivingEntity.class, new AABB(p_362804_).inflate(3.0))) {
         livingentity.setRecordPlayingNearby(p_362804_, p_363434_);
      }
   }
}
