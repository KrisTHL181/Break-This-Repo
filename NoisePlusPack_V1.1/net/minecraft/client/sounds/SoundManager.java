package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Identifier EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("empty");
   public static final Sound EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
   public static final Identifier INTENTIONALLY_EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("intentionally_empty");
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
   public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
      INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
   );
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = new GsonBuilder().registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {};
   private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<Identifier, Resource> soundCache = new HashMap<>();

   public SoundManager(Options p_250027_) {
      this.soundEngine = new SoundEngine(this, p_250027_, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(ResourceManager p_120356_, ProfilerFiller p_120357_) {
      SoundManager.Preparations soundmanager$preparations = new SoundManager.Preparations();

      try (Zone zone = p_120357_.zone("list")) {
         soundmanager$preparations.listResources(p_120356_);
      }

      for (String s : p_120356_.getNamespaces()) {
         try (Zone zone1 = p_120357_.zone(s)) {
            for (Resource resource : p_120356_.getResourceStack(Identifier.fromNamespaceAndPath(s, "sounds.json"))) {
               p_120357_.push(resource.sourcePackId());

               try (Reader reader = resource.openAsReader()) {
                  p_120357_.push("parse");
                  Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                  p_120357_.popPush("register");

                  for (Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                     soundmanager$preparations.handleRegistration(Identifier.fromNamespaceAndPath(s, entry.getKey()), entry.getValue());
                  }

                  p_120357_.pop();
               } catch (RuntimeException runtimeexception) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{"sounds.json", resource.sourcePackId(), runtimeexception});
               }

               p_120357_.pop();
            }
         } catch (IOException var20) {
         }
      }

      return soundmanager$preparations;
   }

   protected void apply(SoundManager.Preparations p_120377_, ResourceManager p_120378_, ProfilerFiller p_120379_) {
      p_120377_.apply(this.registry, this.soundCache, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for (Identifier identifier : this.registry.keySet()) {
            WeighedSoundEvents weighedsoundevents = this.registry.get(identifier);
            if (!ComponentUtils.isTranslationResolvable(weighedsoundevents.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(identifier)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", weighedsoundevents.getSubtitle(), identifier);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for (Identifier identifier1 : this.registry.keySet()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(identifier1)) {
               LOGGER.debug("Not having sound event for: {}", identifier1);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   public ListenerTransform getListenerTransform() {
      return this.soundEngine.getListenerTransform();
   }

   static boolean validateSoundResource(Sound p_250396_, Identifier p_456345_, ResourceProvider p_248737_) {
      Identifier identifier = p_250396_.getPath();
      if (p_248737_.getResource(identifier).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", identifier, p_456345_);
         return false;
      } else {
         return true;
      }
   }

   public @Nullable WeighedSoundEvents getSoundEvent(Identifier p_459146_) {
      return this.registry.get(p_459146_);
   }

   public Collection<Identifier> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance p_120373_) {
      this.soundEngine.queueTickingSound(p_120373_);
   }

   public SoundEngine.PlayResult play(SoundInstance p_120368_) {
      return this.soundEngine.play(p_120368_);
   }

   public void playDelayed(SoundInstance p_120370_, int p_120371_) {
      this.soundEngine.playDelayed(p_120370_, p_120371_);
   }

   public void updateSource(Camera p_120362_) {
      this.soundEngine.updateSource(p_120362_);
   }

   public void pauseAllExcept(SoundSource... p_406130_) {
      this.soundEngine.pauseAllExcept(p_406130_);
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void emergencyShutdown() {
      this.soundEngine.emergencyShutdown();
   }

   public void tick(boolean p_120390_) {
      this.soundEngine.tick(p_120390_);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void refreshCategoryVolume(SoundSource p_460776_) {
      this.soundEngine.refreshCategoryVolume(p_460776_);
   }

   public void stop(SoundInstance p_120400_) {
      this.soundEngine.stop(p_120400_);
   }

   public void updateCategoryVolume(SoundSource p_455943_, float p_459400_) {
      this.soundEngine.updateCategoryVolume(p_455943_, p_459400_);
   }

   public boolean isActive(SoundInstance p_120404_) {
      return this.soundEngine.isActive(p_120404_);
   }

   public void addListener(SoundEventListener p_120375_) {
      this.soundEngine.addEventListener(p_120375_);
   }

   public void removeListener(SoundEventListener p_120402_) {
      this.soundEngine.removeEventListener(p_120402_);
   }

   public void stop(@Nullable Identifier p_452647_, @Nullable SoundSource p_120388_) {
      this.soundEngine.stop(p_452647_, p_120388_);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Preparations {
      final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<Identifier, Resource> soundCache = Map.of();

      void listResources(ResourceManager p_249271_) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(p_249271_);
      }

      void handleRegistration(Identifier p_458062_, SoundEventRegistration p_249632_) {
         WeighedSoundEvents weighedsoundevents = this.registry.get(p_458062_);
         boolean flag = weighedsoundevents == null;
         if (flag || p_249632_.isReplace()) {
            if (!flag) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", p_458062_);
            }

            weighedsoundevents = new WeighedSoundEvents(p_458062_, p_249632_.getSubtitle());
            this.registry.put(p_458062_, weighedsoundevents);
         }

         ResourceProvider resourceprovider = ResourceProvider.fromMap(this.soundCache);

         for (final Sound sound : p_249632_.getSounds()) {
            final Identifier identifier = sound.getLocation();
            Weighted<Sound> weighted;
            switch (sound.getType()) {
               case FILE:
                  if (!SoundManager.validateSoundResource(sound, p_458062_, resourceprovider)) {
                     continue;
                  }

                  weighted = sound;
                  break;
               case SOUND_EVENT:
                  weighted = new Weighted<Sound>() {
                     @Override
                     public int getWeight() {
                        WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(identifier);
                        return weighedsoundevents1 == null ? 0 : weighedsoundevents1.getWeight();
                     }

                     public Sound getSound(RandomSource p_235261_) {
                        WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(identifier);
                        if (weighedsoundevents1 == null) {
                           return SoundManager.EMPTY_SOUND;
                        }

                        Sound sound1 = weighedsoundevents1.getSound(p_235261_);
                        return new Sound(
                           sound1.getLocation(),
                           new MultipliedFloats(sound1.getVolume(), sound.getVolume()),
                           new MultipliedFloats(sound1.getPitch(), sound.getPitch()),
                           sound.getWeight(),
                           Sound.Type.FILE,
                           sound1.shouldStream() || sound.shouldStream(),
                           sound1.shouldPreload(),
                           sound1.getAttenuationDistance()
                        );
                     }

                     @Override
                     public void preloadIfRequired(SoundEngine p_120438_) {
                        WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(identifier);
                        if (weighedsoundevents1 != null) {
                           weighedsoundevents1.preloadIfRequired(p_120438_);
                        }
                     }
                  };
                  break;
               default:
                  throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getType());
            }

            weighedsoundevents.addSound(weighted);
         }
      }

      public void apply(Map<Identifier, WeighedSoundEvents> p_251229_, Map<Identifier, Resource> p_251045_, SoundEngine p_250302_) {
         p_251229_.clear();
         p_251045_.clear();
         p_251045_.putAll(this.soundCache);

         for (Entry<Identifier, WeighedSoundEvents> entry : this.registry.entrySet()) {
            p_251229_.put(entry.getKey(), entry.getValue());
            entry.getValue().preloadIfRequired(p_250302_);
         }
      }
   }
}
