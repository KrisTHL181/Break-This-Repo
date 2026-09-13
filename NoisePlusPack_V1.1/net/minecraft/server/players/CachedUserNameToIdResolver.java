package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

public class CachedUserNameToIdResolver implements UserNameToIdResolver {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int GAMEPROFILES_MRU_LIMIT = 1000;
   private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
   private boolean resolveOfflineUsers = true;
   private final Map<String, CachedUserNameToIdResolver.GameProfileInfo> profilesByName = new ConcurrentHashMap<>();
   private final Map<UUID, CachedUserNameToIdResolver.GameProfileInfo> profilesByUUID = new ConcurrentHashMap<>();
   private final GameProfileRepository profileRepository;
   private final Gson gson = new GsonBuilder().create();
   private final File file;
   private final AtomicLong operationCount = new AtomicLong();

   public CachedUserNameToIdResolver(GameProfileRepository p_429966_, File p_428956_) {
      this.profileRepository = p_429966_;
      this.file = p_428956_;
      Lists.reverse(this.load()).forEach(this::safeAdd);
   }

   private void safeAdd(CachedUserNameToIdResolver.GameProfileInfo p_427969_) {
      NameAndId nameandid = p_427969_.nameAndId();
      p_427969_.setLastAccess(this.getNextOperation());
      this.profilesByName.put(nameandid.name().toLowerCase(Locale.ROOT), p_427969_);
      this.profilesByUUID.put(nameandid.id(), p_427969_);
   }

   private Optional<NameAndId> lookupGameProfile(GameProfileRepository p_427429_, String p_428400_) {
      if (!StringUtil.isValidPlayerName(p_428400_)) {
         return this.createUnknownProfile(p_428400_);
      }

      Optional<NameAndId> optional = p_427429_.findProfileByName(p_428400_).map(NameAndId::new);
      return optional.isEmpty() ? this.createUnknownProfile(p_428400_) : optional;
   }

   private Optional<NameAndId> createUnknownProfile(String p_430725_) {
      return this.resolveOfflineUsers ? Optional.of(NameAndId.createOffline(p_430725_)) : Optional.empty();
   }

   @Override
   public void resolveOfflineUsers(boolean p_428568_) {
      this.resolveOfflineUsers = p_428568_;
   }

   @Override
   public void add(NameAndId p_429952_) {
      this.addInternal(p_429952_);
   }

   private CachedUserNameToIdResolver.GameProfileInfo addInternal(NameAndId p_428081_) {
      Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT);
      calendar.setTime(new Date());
      calendar.add(2, 1);
      Date date = calendar.getTime();
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = new CachedUserNameToIdResolver.GameProfileInfo(p_428081_, date);
      this.safeAdd(cachedusernametoidresolver$gameprofileinfo);
      this.save();
      return cachedusernametoidresolver$gameprofileinfo;
   }

   private long getNextOperation() {
      return this.operationCount.incrementAndGet();
   }

   @Override
   public Optional<NameAndId> get(String p_426041_) {
      String s = p_426041_.toLowerCase(Locale.ROOT);
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = this.profilesByName.get(s);
      boolean flag = false;
      if (cachedusernametoidresolver$gameprofileinfo != null && new Date().getTime() >= cachedusernametoidresolver$gameprofileinfo.expirationDate.getTime()) {
         this.profilesByUUID.remove(cachedusernametoidresolver$gameprofileinfo.nameAndId().id());
         this.profilesByName.remove(cachedusernametoidresolver$gameprofileinfo.nameAndId().name().toLowerCase(Locale.ROOT));
         flag = true;
         cachedusernametoidresolver$gameprofileinfo = null;
      }

      Optional<NameAndId> optional;
      if (cachedusernametoidresolver$gameprofileinfo != null) {
         cachedusernametoidresolver$gameprofileinfo.setLastAccess(this.getNextOperation());
         optional = Optional.of(cachedusernametoidresolver$gameprofileinfo.nameAndId());
      } else {
         Optional<NameAndId> optional1 = this.lookupGameProfile(this.profileRepository, s);
         if (optional1.isPresent()) {
            optional = Optional.of(this.addInternal(optional1.get()).nameAndId());
            flag = false;
         } else {
            optional = Optional.empty();
         }
      }

      if (flag) {
         this.save();
      }

      return optional;
   }

   @Override
   public Optional<NameAndId> get(UUID p_423049_) {
      CachedUserNameToIdResolver.GameProfileInfo cachedusernametoidresolver$gameprofileinfo = this.profilesByUUID.get(p_423049_);
      if (cachedusernametoidresolver$gameprofileinfo == null) {
         return Optional.empty();
      }

      cachedusernametoidresolver$gameprofileinfo.setLastAccess(this.getNextOperation());
      return Optional.of(cachedusernametoidresolver$gameprofileinfo.nameAndId());
   }

   private static DateFormat createDateFormat() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   }

   private List<CachedUserNameToIdResolver.GameProfileInfo> load() {
      List<CachedUserNameToIdResolver.GameProfileInfo> list = Lists.newArrayList();

      try {
         Object object;
         try (Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
            JsonArray jsonarray = (JsonArray)this.gson.fromJson(reader, JsonArray.class);
            if (jsonarray != null) {
               DateFormat dateformat = createDateFormat();
               jsonarray.forEach(p_424444_ -> readGameProfile(p_424444_, dateformat).ifPresent(list::add));
               return list;
            }

            object = list;
         }

         return (List<CachedUserNameToIdResolver.GameProfileInfo>)object;
      } catch (FileNotFoundException var9) {
      } catch (JsonParseException | IOException ioexception) {
         LOGGER.warn("Failed to load profile cache {}", this.file, ioexception);
      }

      return list;
   }

   @Override
   public void save() {
      JsonArray jsonarray = new JsonArray();
      DateFormat dateformat = createDateFormat();
      this.getTopMRUProfiles(1000).forEach(p_422828_ -> jsonarray.add(writeGameProfile(p_422828_, dateformat)));
      String s = this.gson.toJson(jsonarray);

      try (Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         writer.write(s);
      } catch (IOException var9) {
      }
   }

   private Stream<CachedUserNameToIdResolver.GameProfileInfo> getTopMRUProfiles(int p_431126_) {
      return ImmutableList.copyOf(this.profilesByUUID.values())
         .stream()
         .sorted(Comparator.comparing(CachedUserNameToIdResolver.GameProfileInfo::lastAccess).reversed())
         .limit(p_431126_);
   }

   private static JsonElement writeGameProfile(CachedUserNameToIdResolver.GameProfileInfo p_426654_, DateFormat p_431612_) {
      JsonObject jsonobject = new JsonObject();
      p_426654_.nameAndId().appendTo(jsonobject);
      jsonobject.addProperty("expiresOn", p_431612_.format(p_426654_.expirationDate()));
      return jsonobject;
   }

   private static Optional<CachedUserNameToIdResolver.GameProfileInfo> readGameProfile(JsonElement p_427116_, DateFormat p_423404_) {
      if (p_427116_.isJsonObject()) {
         JsonObject jsonobject = p_427116_.getAsJsonObject();
         NameAndId nameandid = NameAndId.fromJson(jsonobject);
         if (nameandid != null) {
            JsonElement jsonelement = jsonobject.get("expiresOn");
            if (jsonelement != null) {
               String s = jsonelement.getAsString();

               try {
                  Date date = p_423404_.parse(s);
                  return Optional.of(new CachedUserNameToIdResolver.GameProfileInfo(nameandid, date));
               } catch (ParseException parseexception) {
                  LOGGER.warn("Failed to parse date {}", s, parseexception);
               }
            }
         }
      }

      return Optional.empty();
   }

   static class GameProfileInfo {
      private final NameAndId nameAndId;
      final Date expirationDate;
      private volatile long lastAccess;

      GameProfileInfo(NameAndId p_428052_, Date p_422633_) {
         this.nameAndId = p_428052_;
         this.expirationDate = p_422633_;
      }

      public NameAndId nameAndId() {
         return this.nameAndId;
      }

      public Date expirationDate() {
         return this.expirationDate;
      }

      public void setLastAccess(long p_422592_) {
         this.lastAccess = p_422592_;
      }

      public long lastAccess() {
         return this.lastAccess;
      }
   }
}
