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

    public LevelEventHandler(final Minecraft minecraft, final ClientLevel level) {
        this.minecraft = minecraft;
        this.level = level;
    }

    public void globalLevelEvent(final int type, final BlockPos pos, final int data) {
        switch (type) {
            case 1023:
            case 1028:
            case 1038:
                Camera camera = this.minecraft.gameRenderer.mainCamera();
                if (camera.isInitialized()) {
                    Vec3 directionToEvent = Vec3.atCenterOf(pos).subtract(camera.position()).normalize();
                    Vec3 soundPos = camera.position().add(directionToEvent.scale(2.0));
                    if (type == 1023) {
                        this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    } else if (type == 1038) {
                        this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    } else {
                        this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
                    }
                }
        }
    }

    public void levelEvent(final int eventType, final BlockPos pos, final int data) {
        RandomSource random = this.level.getRandom();
        switch (eventType) {
            case 1000:
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1001:
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1002:
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1004:
                this.level.playLocalSound(pos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
                break;
            case 1009:
                if (data == 0) {
                    this.level
                        .playLocalSound(
                            pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false
                        );
                } else if (data == 1) {
                    this.level
                        .playLocalSound(
                            pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (random.nextFloat() - random.nextFloat()) * 0.4F, false
                        );
                }
                break;
            case 1010:
                this.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(data).ifPresent(song -> this.playJukeboxSong(song, pos));
                break;
            case 1011:
                this.stopJukeboxSongAndNotifyNearby(pos);
                break;
            case 1015:
                this.level
                    .playLocalSound(pos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1016:
                this.level
                    .playLocalSound(pos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1017:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1018:
                this.level
                    .playLocalSound(pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1019:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1020:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1021:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1022:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1024:
                this.level
                    .playLocalSound(pos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1025:
                this.level
                    .playLocalSound(pos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1026:
                this.level
                    .playLocalSound(pos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
                break;
            case 1027:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1029:
                this.level.playLocalSound(pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1030:
                this.level.playLocalSound(pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1031:
                this.level.playLocalSound(pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1032:
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, random.nextFloat() * 0.4F + 0.8F, 0.25F));
                break;
            case 1033:
                this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1034:
                this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1035:
                this.level.playLocalSound(pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1039:
                this.level.playLocalSound(pos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1040:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1041:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1042:
                this.level.playLocalSound(pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1043:
                this.level.playLocalSound(pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1044:
                this.level.playLocalSound(pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1045:
                this.level.playLocalSound(pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1046:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1047:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1048:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1049:
                this.level.playLocalSound(pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1050:
                this.level.playLocalSound(pos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1051:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false
                    );
                break;
            case 1052:
                this.level.playLocalSound(pos, SoundEvents.SULFUR_SPIKE_LAND, SoundSource.BLOCKS, 2.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1500:
                ComposterBlock.handleFill(this.level, pos, data > 0);
                break;
            case 1501:
                this.level
                    .playLocalSound(pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);

                for (int i = 0; i < 8; i++) {
                    this.level
                        .addParticle(
                            ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.2, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0
                        );
                }
                break;
            case 1502:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false
                    );

                for (int i = 0; i < 5; i++) {
                    double x = pos.getX() + random.nextDouble() * 0.6 + 0.2;
                    double y = pos.getY() + random.nextDouble() * 0.6 + 0.2;
                    double z = pos.getZ() + random.nextDouble() * 0.6 + 0.2;
                    this.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            case 1503:
                this.level.playLocalSound(pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

                for (int i = 0; i < 16; i++) {
                    double x = pos.getX() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    double y = pos.getY() + 0.8125;
                    double z = pos.getZ() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    this.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            case 1504:
                PointedDripstoneBlock.spawnDripParticle(this.level, pos, this.level.getBlockState(pos));
                break;
            case 1505:
                BoneMealItem.addGrowthParticles(this.level, pos, data);
                this.level.playLocalSound(pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 2000:
                this.shootParticles(data, pos, random, ParticleTypes.SMOKE);
                break;
            case 2001:
                BlockState blockState = Block.stateById(data);
                if (!blockState.isAir()) {
                    SoundType soundType = blockState.getSoundType();
                    this.level
                        .playLocalSound(
                            pos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false
                        );
                }

                this.level.addDestroyBlockEffect(pos, blockState);
                break;
            case 2002:
            case 2007: {
                Vec3 particlePos = Vec3.atBottomCenterOf(pos);
                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.SPLASH_POTION);

                for (int i = 0; i < 8; i++) {
                    this.level
                        .addParticle(
                            breakParticle,
                            particlePos.x,
                            particlePos.y,
                            particlePos.z,
                            random.nextGaussian() * 0.15,
                            random.nextDouble() * 0.2,
                            random.nextGaussian() * 0.15
                        );
                }

                float red = (data >> 16 & 0xFF) / 255.0F;
                float green = (data >> 8 & 0xFF) / 255.0F;
                float blue = (data >> 0 & 0xFF) / 255.0F;
                ParticleType<SpellParticleOption> particleType = eventType == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

                for (int i = 0; i < 100; i++) {
                    double dist = random.nextDouble() * 4.0;
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double velocityX = Math.cos(angle) * dist;
                    double velocityY = 0.01 + random.nextDouble() * 0.5;
                    double velocityZ = Math.sin(angle) * dist;
                    float randomBrightness = 0.75F + random.nextFloat() * 0.25F;
                    SpellParticleOption particle = SpellParticleOption.create(
                        particleType, red * randomBrightness, green * randomBrightness, blue * randomBrightness, (float)dist
                    );
                    this.level
                        .addParticle(
                            particle, particlePos.x + velocityX * 0.1, particlePos.y + 0.3, particlePos.z + velocityZ * 0.1, velocityX, velocityY, velocityZ
                        );
                }

                this.level.playLocalSound(pos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            }
            case 2003: {
                double x = pos.getX() + 0.5;
                double y = pos.getY();
                double z = pos.getZ() + 0.5;
                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);

                for (int i = 0; i < 8; i++) {
                    this.level.addParticle(breakParticle, x, y, z, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);
                }

                for (double angle = 0.0; angle < Math.PI * 2; angle += Math.PI / 20) {
                    this.level
                        .addParticle(
                            ParticleTypes.PORTAL,
                            x + Math.cos(angle) * 5.0,
                            y - 0.4,
                            z + Math.sin(angle) * 5.0,
                            Math.cos(angle) * -5.0,
                            0.0,
                            Math.sin(angle) * -5.0
                        );
                    this.level
                        .addParticle(
                            ParticleTypes.PORTAL,
                            x + Math.cos(angle) * 5.0,
                            y - 0.4,
                            z + Math.sin(angle) * 5.0,
                            Math.cos(angle) * -7.0,
                            0.0,
                            Math.sin(angle) * -7.0
                        );
                }
                break;
            }
            case 2004:
                for (int i = 0; i < 20; i++) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    this.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                    this.level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            case 2006:
                for (int i = 0; i < 200; i++) {
                    float dist = random.nextFloat() * 4.0F;
                    float angle = random.nextFloat() * (float) (Math.PI * 2);
                    double velocityX = Mth.cos(angle) * dist;
                    double velocityY = 0.01 + random.nextDouble() * 0.5;
                    double velocityZ = Mth.sin(angle) * dist;
                    this.level
                        .addParticle(
                            PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, dist),
                            pos.getX() + velocityX * 0.1,
                            pos.getY() + 0.3,
                            pos.getZ() + velocityZ * 0.1,
                            velocityX,
                            velocityY,
                            velocityZ
                        );
                }

                if (data == 1) {
                    this.level.playLocalSound(pos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                }
                break;
            case 2008:
                this.level.addParticle(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                break;
            case 2009:
                for (int i = 0; i < 8; i++) {
                    this.level
                        .addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.2, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
                }
                break;
            case 2010:
                this.shootParticles(data, pos, random, ParticleTypes.WHITE_SMOKE);
                break;
            case 2011:
                ParticleUtils.spawnParticleInBlock(this.level, pos, data, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2012:
                ParticleUtils.spawnParticleInBlock(this.level, pos, data, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2013:
                ParticleUtils.spawnSmashAttackParticles(this.level, pos, data);
                break;
            case 3000:
                this.level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION_EMITTER, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                this.level
                    .playLocalSound(
                        pos, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0F, (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F, false
                    );
                break;
            case 3001:
                this.level.playLocalSound(pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + random.nextFloat() * 0.3F, false);
                break;
            case 3002:
                if (data >= 0 && data < Direction.Axis.VALUES.length) {
                    ParticleUtils.spawnParticlesAlongAxis(
                        Direction.Axis.VALUES[data], this.level, pos, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19)
                    );
                } else {
                    ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
                }
                break;
            case 3003:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
                this.level.playLocalSound(pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 3004:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
                break;
            case 3005:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
                break;
            case 3006:
                int count = data >> 6;
                if (count > 0) {
                    if (random.nextFloat() < 0.3F + count * 0.1F) {
                        float volume = 0.15F + 0.02F * count * count * random.nextFloat();
                        float pitch = 0.4F + 0.3F * count * random.nextFloat();
                        this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, volume, pitch, false);
                    }

                    byte particleData = (byte)(data & 63);
                    IntProvider repetition = UniformInt.of(0, count);
                    float speedVar = 0.005F;
                    Supplier<Vec3> speedSupplier = () -> new Vec3(
                        Mth.nextDouble(random, -0.005F, 0.005F), Mth.nextDouble(random, -0.005F, 0.005F), Mth.nextDouble(random, -0.005F, 0.005F)
                    );
                    if (particleData == 0) {
                        for (Direction direction : Direction.values()) {
                            float fullBlockRotation = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                            double fullBlockFactor = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                            ParticleUtils.spawnParticlesOnBlockFace(
                                this.level, pos, new SculkChargeParticleOptions(fullBlockRotation), repetition, direction, speedSupplier, fullBlockFactor
                            );
                        }
                    } else {
                        for (Direction direction : MultifaceBlock.unpack(particleData)) {
                            float facesBlockRotation = direction == Direction.UP ? (float) Math.PI : 0.0F;
                            double facesBlockFactor = 0.35;
                            ParticleUtils.spawnParticlesOnBlockFace(
                                this.level, pos, new SculkChargeParticleOptions(facesBlockRotation), repetition, direction, speedSupplier, 0.35
                            );
                        }
                    }
                } else {
                    this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    boolean isSolid = this.level.getBlockState(pos).isCollisionShapeFullBlock(this.level, pos);
                    int particleCount = isSolid ? 40 : 20;
                    float spread = isSolid ? 0.45F : 0.25F;
                    float speed = 0.07F;

                    for (int i = 0; i < particleCount; i++) {
                        float velocityX = 2.0F * random.nextFloat() - 1.0F;
                        float velocityY = 2.0F * random.nextFloat() - 1.0F;
                        float velocityZ = 2.0F * random.nextFloat() - 1.0F;
                        this.level
                            .addParticle(
                                ParticleTypes.SCULK_CHARGE_POP,
                                pos.getX() + 0.5 + velocityX * spread,
                                pos.getY() + 0.5 + velocityY * spread,
                                pos.getZ() + 0.5 + velocityZ * spread,
                                velocityX * 0.07F,
                                velocityY * 0.07F,
                                velocityZ * 0.07F
                            );
                    }
                }
                break;
            case 3007:
                for (int i = 0; i < 10; i++) {
                    this.level
                        .addParticle(new ShriekParticleOption(i * 5), pos.getX() + 0.5, pos.getY() + SculkShriekerBlock.TOP_Y, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                }

                BlockState state = this.level.getBlockState(pos);
                boolean isWaterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
                if (!isWaterlogged) {
                    this.level
                        .playLocalSound(
                            pos.getX() + 0.5,
                            pos.getY() + SculkShriekerBlock.TOP_Y,
                            pos.getZ() + 0.5,
                            SoundEvents.SCULK_SHRIEKER_SHRIEK,
                            SoundSource.BLOCKS,
                            2.0F,
                            0.6F + random.nextFloat() * 0.4F,
                            false
                        );
                }
                break;
            case 3008:
                BlockState blockStateForBrushing = Block.stateById(data);
                if (blockStateForBrushing.getBlock() instanceof BrushableBlock brushableBlock) {
                    this.level.playLocalSound(pos, brushableBlock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
                }

                this.level.addDestroyBlockEffect(pos, blockStateForBrushing);
                break;
            case 3009:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
                break;
            case 3011:
                TrialSpawner.addSpawnParticles(this.level, pos, random, TrialSpawner.FlameParticle.decode(data).particleType);
                break;
            case 3012:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addSpawnParticles(this.level, pos, random, TrialSpawner.FlameParticle.decode(data).particleType);
                break;
            case 3013:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pos, random, data, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
                break;
            case 3014:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addEjectItemParticles(this.level, pos, random);
                break;
            case 3015:
                if (this.level.getBlockEntity(pos) instanceof VaultBlockEntity entity) {
                    VaultBlockEntity.Client.emitActivationParticles(
                        this.level,
                        entity.getBlockPos(),
                        entity.getBlockState(),
                        entity.getSharedData(),
                        data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
                    );
                    this.level
                        .playLocalSound(
                            pos, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                        );
                }
                break;
            case 3016:
                VaultBlockEntity.Client.emitDeactivationParticles(this.level, pos, data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
                this.level
                    .playLocalSound(pos, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true);
                break;
            case 3017:
                TrialSpawner.addEjectItemParticles(this.level, pos, random);
                break;
            case 3018:
                for (int i = 0; i < 10; i++) {
                    double velocityX = random.nextGaussian() * 0.02;
                    double velocityY = random.nextGaussian() * 0.02;
                    double velocityZ = random.nextGaussian() * 0.02;
                    this.level
                        .addParticle(
                            ParticleTypes.POOF,
                            pos.getX() + random.nextDouble(),
                            pos.getY() + random.nextDouble(),
                            pos.getZ() + random.nextDouble(),
                            velocityX,
                            velocityY,
                            velocityZ
                        );
                }

                this.level
                    .playLocalSound(pos, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true);
                break;
            case 3019:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pos, random, data, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                break;
            case 3020:
                this.level
                    .playLocalSound(
                        pos,
                        SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                        SoundSource.BLOCKS,
                        data == 0 ? 0.3F : 1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pos, random, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                TrialSpawner.addBecomeOminousParticles(this.level, pos, random);
                break;
            case 3021:
                this.level
                    .playLocalSound(
                        pos, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addSpawnParticles(this.level, pos, random, TrialSpawner.FlameParticle.decode(data).particleType);
        }
    }

    private void shootParticles(final int data, final BlockPos pos, final RandomSource random, final SimpleParticleType particle) {
        Direction direction = Direction.from3DDataValue(data);
        int normalX = direction.getStepX();
        int normalY = direction.getStepY();
        int normalZ = direction.getStepZ();

        for (int i = 0; i < 10; i++) {
            double pow = random.nextDouble() * 0.2 + 0.01;
            double x = pos.getX() + normalX * 0.6 + 0.5 + normalX * 0.01 + (random.nextDouble() - 0.5) * normalZ * 0.5;
            double y = pos.getY() + normalY * 0.6 + 0.5 + normalY * 0.01 + (random.nextDouble() - 0.5) * normalY * 0.5;
            double z = pos.getZ() + normalZ * 0.6 + 0.5 + normalZ * 0.01 + (random.nextDouble() - 0.5) * normalX * 0.5;
            double velocityX = normalX * pow + random.nextGaussian() * 0.01;
            double velocityY = normalY * pow + random.nextGaussian() * 0.01;
            double velocityZ = normalZ * pow + random.nextGaussian() * 0.01;
            this.level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private void playJukeboxSong(final Holder<JukeboxSong> songHolder, final BlockPos pos) {
        this.stopJukeboxSong(pos);
        JukeboxSong song = songHolder.value();
        SoundEvent sound = song.soundEvent().value();
        SoundInstance instance = SimpleSoundInstance.forJukeboxSong(sound, Vec3.atCenterOf(pos));
        this.playingJukeboxSongs.put(pos, instance);
        this.minecraft.getSoundManager().play(instance);
        this.minecraft.gui.hud.setNowPlaying(song.description());
        this.notifyNearbyEntities(this.level, pos, true);
    }

    private void stopJukeboxSong(final BlockPos pos) {
        SoundInstance removedInstance = this.playingJukeboxSongs.remove(pos);
        if (removedInstance != null) {
            this.minecraft.getSoundManager().stop(removedInstance);
        }
    }

    private void stopJukeboxSongAndNotifyNearby(final BlockPos pos) {
        this.stopJukeboxSong(pos);
        this.notifyNearbyEntities(this.level, pos, false);
    }

    private void notifyNearbyEntities(final Level level, final BlockPos pos, final boolean isPlaying) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0))) {
            entity.setRecordPlayingNearby(pos, isPlaying);
        }
    }
}
