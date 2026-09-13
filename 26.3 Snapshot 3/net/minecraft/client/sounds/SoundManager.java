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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Identifier EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("empty");
   public static final Sound EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
   public static final Identifier INTENTIONALLY_EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("intentionally_empty");
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
   public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
      INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = new GsonBuilder().registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {};
   private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<Identifier, Resource> soundCache = new HashMap<>();

   public SoundManager(final Options options) {
      this.soundEngine = new SoundEngine(this, options, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(final ResourceManager manager, final ProfilerFiller profiler) {
      SoundManager.Preparations preparations = new SoundManager.Preparations();

      try (Zone ignored = profiler.zone("list")) {
         preparations.listResources(manager);
      }

      for (String namespace : manager.getNamespaces()) {
         try (Zone ignored = profiler.zone(namespace)) {
            for (Resource resource : manager.getResourceStack(Identifier.fromNamespaceAndPath(namespace, "sounds.json"))) {
               profiler.push(resource.sourcePackId());

               try (Reader reader = resource.openAsReader()) {
                  profiler.push("parse");
                  Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                  profiler.popPush("register");

                  for (Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                     preparations.handleRegistration(Identifier.fromNamespaceAndPath(namespace, entry.getKey()), entry.getValue());
                  }

                  profiler.pop();
               } catch (RuntimeException e) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{"sounds.json", resource.sourcePackId(), e});
               }

               profiler.pop();
            }
         } catch (IOException var21) {
         }
      }

      return preparations;
   }

   protected void apply(final SoundManager.Preparations preparations, final ResourceManager manager, final ProfilerFiller profiler) {
      preparations.apply(this.registry, this.soundCache, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for (Identifier location : this.registry.keySet()) {
            WeighedSoundEvents event = this.registry.get(location);
            if (!ComponentUtils.isTranslationResolvable(event.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey(location)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", event.getSubtitle(), location);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for (Identifier location : this.registry.keySet()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(location)) {
               LOGGER.debug("Not having sound event for: {}", location);
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

   private static boolean validateSoundResource(final Sound sound, final Identifier eventLocation, final ResourceProvider resourceProvider) {
      Identifier soundPath = sound.getPath();
      if (resourceProvider.getResource(soundPath).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", soundPath, eventLocation);
         return false;
      } else {
         return true;
      }
   }

   public @Nullable WeighedSoundEvents getSoundEvent(final Identifier location) {
      return this.registry.get(location);
   }

   public Collection<Identifier> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(final TickableSoundInstance instance) {
      this.soundEngine.queueTickingSound(instance);
   }

   public SoundEngine.PlayResult play(final SoundInstance instance) {
      return this.soundEngine.play(instance);
   }

   public void playDelayed(final SoundInstance instance, final int delay) {
      this.soundEngine.playDelayed(instance, delay);
   }

   public void updateSource(final Camera camera) {
      this.soundEngine.updateSource(camera);
   }

   public void pauseAllExcept(final SoundSource... ignoredSources) {
      this.soundEngine.pauseAllExcept(ignoredSources);
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

   public void tick(final boolean paused) {
      this.soundEngine.tick(paused);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void refreshCategoryVolume(final SoundSource category) {
      this.soundEngine.refreshCategoryVolume(category);
   }

   public void stop(final SoundInstance soundInstance) {
      this.soundEngine.stop(soundInstance);
   }

   public void updateCategoryVolume(final SoundSource source, final float gain) {
      this.soundEngine.updateCategoryVolume(source, gain);
   }

   public boolean isActive(final SoundInstance instance) {
      return this.soundEngine.isActive(instance);
   }

   public void addListener(final SoundEventListener listener) {
      this.soundEngine.addEventListener(listener);
   }

   public void removeListener(final SoundEventListener listener) {
      this.soundEngine.removeEventListener(listener);
   }

   public void stop(final @Nullable Identifier sound, final @Nullable SoundSource source) {
      this.soundEngine.stop(sound, source);
   }

   public String getChannelDebugString() {
      return this.soundEngine.getChannelDebugString();
   }

   public void getSoundCacheDebugStats(final SoundBufferLibrary.DebugOutput output) {
      this.soundEngine.getSoundCacheDebugStats(output);
   }

   public void reload() {
      this.soundEngine.reload();
   }

   protected static class Preparations {
      private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<Identifier, Resource> soundCache = Map.of();

      private void listResources(final ResourceManager resourceManager) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(resourceManager);
      }

      private void handleRegistration(final Identifier eventLocation, final SoundEventRegistration soundEventRegistration) {
         WeighedSoundEvents registration = this.registry.get(eventLocation);
         boolean missesRegistration = registration == null;
         if (missesRegistration || soundEventRegistration.isReplace()) {
            if (!missesRegistration) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", eventLocation);
            }

            registration = new WeighedSoundEvents(eventLocation, soundEventRegistration.getSubtitle());
            this.registry.put(eventLocation, registration);
         }

         ResourceProvider cachedProvider = ResourceProvider.fromMap(this.soundCache);

         for (final Sound sound : soundEventRegistration.getSounds()) {
            final Identifier soundLocation = sound.getLocation();
            Weighted<Sound> weighted;
            switch (sound.getType()) {
               case FILE:
                  if (!SoundManager.validateSoundResource(sound, eventLocation, cachedProvider)) {
                     continue;
                  }

                  weighted = sound;
                  break;
               case SOUND_EVENT:
                  weighted = new Weighted<Sound>() {
                     @Override
                     public int getWeight() {
                        WeighedSoundEvents registrationx = Preparations.this.registry.get(soundLocation);
                        return registrationx == null ? 0 : registrationx.getWeight();
                     }

                     public Sound getSound(final RandomSource random) {
                        WeighedSoundEvents registrationx = Preparations.this.registry.get(soundLocation);
                        if (registrationx == null) {
                           return SoundManager.EMPTY_SOUND;
                        }

                        Sound wrappedSound = registrationx.getSound(random);
                        return new Sound(
                           wrappedSound.getLocation(),
                           new MultipliedFloats(wrappedSound.getVolume(), sound.getVolume()),
                           new MultipliedFloats(wrappedSound.getPitch(), sound.getPitch()),
                           sound.getWeight(),
                           Sound.Type.FILE,
                           wrappedSound.shouldStream() || sound.shouldStream(),
                           wrappedSound.shouldPreload(),
                           wrappedSound.getAttenuationDistance()
                        );
                     }

                     @Override
                     public void preloadIfRequired(final SoundEngine soundEngine) {
                        WeighedSoundEvents registrationx = Preparations.this.registry.get(soundLocation);
                        if (registrationx != null) {
                           registrationx.preloadIfRequired(soundEngine);
                        }
                     }
                  };
                  break;
               default:
                  throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getType());
            }

            registration.addSound(weighted);
         }
      }

      public void apply(final Map<Identifier, WeighedSoundEvents> registry, final Map<Identifier, Resource> soundCache, final SoundEngine engine) {
         registry.clear();
         soundCache.clear();
         soundCache.putAll(this.soundCache);

         for (Entry<Identifier, WeighedSoundEvents> entry : this.registry.entrySet()) {
            registry.put(entry.getKey(), entry.getValue());
            entry.getValue().preloadIfRequired(engine);
         }
      }
   }
}
