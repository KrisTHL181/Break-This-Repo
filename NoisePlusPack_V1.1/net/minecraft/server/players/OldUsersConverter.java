package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class OldUsersConverter {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final File OLD_IPBANLIST = new File("banned-ips.txt");
   public static final File OLD_USERBANLIST = new File("banned-players.txt");
   public static final File OLD_OPLIST = new File("ops.txt");
   public static final File OLD_WHITELIST = new File("white-list.txt");

   static List<String> readOldListFormat(File p_11074_, Map<String, String[]> p_11075_) throws IOException {
      List<String> list = Files.readLines(p_11074_, StandardCharsets.UTF_8);

      for (String s : list) {
         s = s.trim();
         if (!s.startsWith("#") && !s.isEmpty()) {
            String[] astring = s.split("\\|");
            p_11075_.put(astring[0].toLowerCase(Locale.ROOT), astring);
         }
      }

      return list;
   }

   private static void lookupPlayers(MinecraftServer p_11087_, Collection<String> p_11088_, ProfileLookupCallback p_11089_) {
      String[] astring = p_11088_.stream().filter(p_11077_ -> !StringUtil.isNullOrEmpty(p_11077_)).toArray(String[]::new);
      if (p_11087_.usesAuthentication()) {
         p_11087_.services().profileRepository().findProfilesByNames(astring, p_11089_);
      } else {
         for (String s : astring) {
            p_11089_.onProfileLookupSucceeded(s, UUIDUtil.createOfflinePlayerUUID(s));
         }
      }
   }

   public static boolean convertUserBanlist(final MinecraftServer p_11082_) {
      final UserBanList userbanlist = new UserBanList(PlayerList.USERBANLIST_FILE, new EmptyNotificationService());
      if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
         if (userbanlist.getFile().exists()) {
            try {
               userbanlist.load();
            } catch (IOException ioexception1) {
               LOGGER.warn("Could not load existing file {}", userbanlist.getFile().getName(), ioexception1);
            }
         }

         try {
            final Map<String, String[]> map = Maps.newHashMap();
            readOldListFormat(OLD_USERBANLIST, map);
            ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
               public void onProfileLookupSucceeded(String p_424792_, UUID p_428104_) {
                  NameAndId nameandid = new NameAndId(p_428104_, p_424792_);
                  p_11082_.services().nameToIdCache().add(nameandid);
                  String[] astring = map.get(nameandid.name().toLowerCase(Locale.ROOT));
                  if (astring == null) {
                     OldUsersConverter.LOGGER.warn("Could not convert user banlist entry for {}", nameandid.name());
                     throw new OldUsersConverter.ConversionError("Profile not in the conversionlist");
                  }

                  Date date = astring.length > 1 ? OldUsersConverter.parseDate(astring[1], null) : null;
                  String s = astring.length > 2 ? astring[2] : null;
                  Date date1 = astring.length > 3 ? OldUsersConverter.parseDate(astring[3], null) : null;
                  String s1 = astring.length > 4 ? astring[4] : null;
                  userbanlist.add(new UserBanListEntry(nameandid, date, s, date1, s1));
               }

               public void onProfileLookupFailed(String p_300345_, Exception p_11121_) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user banlist entry for {}", p_300345_, p_11121_);
                  if (!(p_11121_ instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + p_300345_ + " from backend systems", p_11121_);
                  }
               }
            };
            lookupPlayers(p_11082_, map.keySet(), profilelookupcallback);
            userbanlist.save();
            renameOldFile(OLD_USERBANLIST);
            return true;
         } catch (IOException ioexception) {
            LOGGER.warn("Could not read old user banlist to convert it!", ioexception);
            return false;
         } catch (OldUsersConverter.ConversionError oldusersconverter$conversionerror) {
            LOGGER.error("Conversion failed, please try again later", oldusersconverter$conversionerror);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertIpBanlist(MinecraftServer p_11099_) {
      IpBanList ipbanlist = new IpBanList(PlayerList.IPBANLIST_FILE, new EmptyNotificationService());
      if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
         if (ipbanlist.getFile().exists()) {
            try {
               ipbanlist.load();
            } catch (IOException ioexception1) {
               LOGGER.warn("Could not load existing file {}", ipbanlist.getFile().getName(), ioexception1);
            }
         }

         try {
            Map<String, String[]> map = Maps.newHashMap();
            readOldListFormat(OLD_IPBANLIST, map);

            for (String s : map.keySet()) {
               String[] astring = map.get(s);
               Date date = astring.length > 1 ? parseDate(astring[1], null) : null;
               String s1 = astring.length > 2 ? astring[2] : null;
               Date date1 = astring.length > 3 ? parseDate(astring[3], null) : null;
               String s2 = astring.length > 4 ? astring[4] : null;
               ipbanlist.add(new IpBanListEntry(s, date, s1, date1, s2));
            }

            ipbanlist.save();
            renameOldFile(OLD_IPBANLIST);
            return true;
         } catch (IOException ioexception) {
            LOGGER.warn("Could not parse old ip banlist to convert it!", ioexception);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertOpsList(final MinecraftServer p_11103_) {
      final ServerOpList serveroplist = new ServerOpList(PlayerList.OPLIST_FILE, new EmptyNotificationService());
      if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
         if (serveroplist.getFile().exists()) {
            try {
               serveroplist.load();
            } catch (IOException ioexception1) {
               LOGGER.warn("Could not load existing file {}", serveroplist.getFile().getName(), ioexception1);
            }
         }

         try {
            List<String> list = Files.readLines(OLD_OPLIST, StandardCharsets.UTF_8);
            ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
               public void onProfileLookupSucceeded(String p_427773_, UUID p_424007_) {
                  NameAndId nameandid = new NameAndId(p_424007_, p_427773_);
                  p_11103_.services().nameToIdCache().add(nameandid);
                  serveroplist.add(new ServerOpListEntry(nameandid, p_11103_.operatorUserPermissions(), false));
               }

               public void onProfileLookupFailed(String p_300384_, Exception p_11131_) {
                  OldUsersConverter.LOGGER.warn("Could not lookup oplist entry for {}", p_300384_, p_11131_);
                  if (!(p_11131_ instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + p_300384_ + " from backend systems", p_11131_);
                  }
               }
            };
            lookupPlayers(p_11103_, list, profilelookupcallback);
            serveroplist.save();
            renameOldFile(OLD_OPLIST);
            return true;
         } catch (IOException ioexception) {
            LOGGER.warn("Could not read old oplist to convert it!", ioexception);
            return false;
         } catch (OldUsersConverter.ConversionError oldusersconverter$conversionerror) {
            LOGGER.error("Conversion failed, please try again later", oldusersconverter$conversionerror);
            return false;
         }
      } else {
         return true;
      }
   }

   public static boolean convertWhiteList(final MinecraftServer p_11105_) {
      final UserWhiteList userwhitelist = new UserWhiteList(PlayerList.WHITELIST_FILE, new EmptyNotificationService());
      if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
         if (userwhitelist.getFile().exists()) {
            try {
               userwhitelist.load();
            } catch (IOException ioexception1) {
               LOGGER.warn("Could not load existing file {}", userwhitelist.getFile().getName(), ioexception1);
            }
         }

         try {
            List<String> list = Files.readLines(OLD_WHITELIST, StandardCharsets.UTF_8);
            ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
               public void onProfileLookupSucceeded(String p_425291_, UUID p_427841_) {
                  NameAndId nameandid = new NameAndId(p_427841_, p_425291_);
                  p_11105_.services().nameToIdCache().add(nameandid);
                  userwhitelist.add(new UserWhiteListEntry(nameandid));
               }

               public void onProfileLookupFailed(String p_301126_, Exception p_11141_) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", p_301126_, p_11141_);
                  if (!(p_11141_ instanceof ProfileNotFoundException)) {
                     throw new OldUsersConverter.ConversionError("Could not request user " + p_301126_ + " from backend systems", p_11141_);
                  }
               }
            };
            lookupPlayers(p_11105_, list, profilelookupcallback);
            userwhitelist.save();
            renameOldFile(OLD_WHITELIST);
            return true;
         } catch (IOException ioexception) {
            LOGGER.warn("Could not read old whitelist to convert it!", ioexception);
            return false;
         } catch (OldUsersConverter.ConversionError oldusersconverter$conversionerror) {
            LOGGER.error("Conversion failed, please try again later", oldusersconverter$conversionerror);
            return false;
         }
      } else {
         return true;
      }
   }

   public static @Nullable UUID convertMobOwnerIfNecessary(final MinecraftServer p_11084_, String p_11085_) {
      if (!StringUtil.isNullOrEmpty(p_11085_) && p_11085_.length() <= 16) {
         Optional<UUID> optional = p_11084_.services().nameToIdCache().get(p_11085_).map(NameAndId::id);
         if (optional.isPresent()) {
            return optional.get();
         } else if (!p_11084_.isSingleplayer() && p_11084_.usesAuthentication()) {
            final List<NameAndId> list = new ArrayList<>();
            ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
               public void onProfileLookupSucceeded(String p_425980_, UUID p_429724_) {
                  NameAndId nameandid = new NameAndId(p_429724_, p_425980_);
                  p_11084_.services().nameToIdCache().add(nameandid);
                  list.add(nameandid);
               }

               public void onProfileLookupFailed(String p_297583_, Exception p_11151_) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", p_297583_, p_11151_);
               }
            };
            lookupPlayers(p_11084_, Lists.newArrayList(new String[]{p_11085_}), profilelookupcallback);
            return !list.isEmpty() ? list.getFirst().id() : null;
         } else {
            return UUIDUtil.createOfflinePlayerUUID(p_11085_);
         }
      } else {
         try {
            return UUID.fromString(p_11085_);
         } catch (IllegalArgumentException illegalargumentexception) {
            return null;
         }
      }
   }

   public static boolean convertPlayers(final DedicatedServer p_11091_) {
      final File file1 = getWorldPlayersDirectory(p_11091_);
      final File file2 = new File(file1.getParentFile(), "playerdata");
      final File file3 = new File(file1.getParentFile(), "unknownplayers");
      if (file1.exists() && file1.isDirectory()) {
         File[] afile = file1.listFiles();
         List<String> list = Lists.newArrayList();

         for (File file4 : afile) {
            String s = file4.getName();
            if (s.toLowerCase(Locale.ROOT).endsWith(".dat")) {
               String s1 = s.substring(0, s.length() - ".dat".length());
               if (!s1.isEmpty()) {
                  list.add(s1);
               }
            }
         }

         try {
            final String[] astring = list.toArray(new String[list.size()]);
            ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
               public void onProfileLookupSucceeded(String p_429889_, UUID p_430886_) {
                  NameAndId nameandid = new NameAndId(p_430886_, p_429889_);
                  p_11091_.services().nameToIdCache().add(nameandid);
                  this.movePlayerFile(file2, this.getFileNameForProfile(p_429889_), p_430886_.toString());
               }

               public void onProfileLookupFailed(String p_297890_, Exception p_11173_) {
                  OldUsersConverter.LOGGER.warn("Could not lookup user uuid for {}", p_297890_, p_11173_);
                  if (p_11173_ instanceof ProfileNotFoundException) {
                     String s2 = this.getFileNameForProfile(p_297890_);
                     this.movePlayerFile(file3, s2, s2);
                  } else {
                     throw new OldUsersConverter.ConversionError("Could not request user " + p_297890_ + " from backend systems", p_11173_);
                  }
               }

               private void movePlayerFile(File p_11168_, String p_11169_, String p_11170_) {
                  File file5 = new File(file1, p_11169_ + ".dat");
                  File file6 = new File(p_11168_, p_11170_ + ".dat");
                  OldUsersConverter.ensureDirectoryExists(p_11168_);
                  if (!file5.renameTo(file6)) {
                     throw new OldUsersConverter.ConversionError("Could not convert file for " + p_11169_);
                  }
               }

               private String getFileNameForProfile(String p_299270_) {
                  String s2 = null;

                  for (String s3 : astring) {
                     if (s3 != null && s3.equalsIgnoreCase(p_299270_)) {
                        s2 = s3;
                        break;
                     }
                  }

                  if (s2 == null) {
                     throw new OldUsersConverter.ConversionError("Could not find the filename for " + p_299270_ + " anymore");
                  } else {
                     return s2;
                  }
               }
            };
            lookupPlayers(p_11091_, Lists.newArrayList(astring), profilelookupcallback);
            return true;
         } catch (OldUsersConverter.ConversionError oldusersconverter$conversionerror) {
            LOGGER.error("Conversion failed, please try again later", oldusersconverter$conversionerror);
            return false;
         }
      } else {
         return true;
      }
   }

   static void ensureDirectoryExists(File p_11094_) {
      if (p_11094_.exists()) {
         if (!p_11094_.isDirectory()) {
            throw new OldUsersConverter.ConversionError("Can't create directory " + p_11094_.getName() + " in world save directory.");
         }
      } else if (!p_11094_.mkdirs()) {
         throw new OldUsersConverter.ConversionError("Can't create directory " + p_11094_.getName() + " in world save directory.");
      }
   }

   public static boolean serverReadyAfterUserconversion(MinecraftServer p_11107_) {
      boolean flag = areOldUserlistsRemoved();
      return flag && areOldPlayersConverted(p_11107_);
   }

   private static boolean areOldUserlistsRemoved() {
      boolean flag = false;
      if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
         flag = true;
      }

      boolean flag1 = false;
      if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
         flag1 = true;
      }

      boolean flag2 = false;
      if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
         flag2 = true;
      }

      boolean flag3 = false;
      if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
         flag3 = true;
      }

      if (!flag && !flag1 && !flag2 && !flag3) {
         return true;
      }

      LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
      LOGGER.warn("** please remove the following files and restart the server:");
      if (flag) {
         LOGGER.warn("* {}", OLD_USERBANLIST.getName());
      }

      if (flag1) {
         LOGGER.warn("* {}", OLD_IPBANLIST.getName());
      }

      if (flag2) {
         LOGGER.warn("* {}", OLD_OPLIST.getName());
      }

      if (flag3) {
         LOGGER.warn("* {}", OLD_WHITELIST.getName());
      }

      return false;
   }

   private static boolean areOldPlayersConverted(MinecraftServer p_11109_) {
      File file1 = getWorldPlayersDirectory(p_11109_);
      if (file1.exists() && file1.isDirectory() && (file1.list().length > 0 || !file1.delete())) {
         LOGGER.warn("**** DETECTED OLD PLAYER DIRECTORY IN THE WORLD SAVE");
         LOGGER.warn("**** THIS USUALLY HAPPENS WHEN THE AUTOMATIC CONVERSION FAILED IN SOME WAY");
         LOGGER.warn("** please restart the server and if the problem persists, remove the directory '{}'", file1.getPath());
         return false;
      } else {
         return true;
      }
   }

   private static File getWorldPlayersDirectory(MinecraftServer p_11111_) {
      return p_11111_.getWorldPath(LevelResource.PLAYER_OLD_DATA_DIR).toFile();
   }

   private static void renameOldFile(File p_11101_) {
      File file1 = new File(p_11101_.getName() + ".converted");
      p_11101_.renameTo(file1);
   }

   static Date parseDate(String p_11096_, Date p_11097_) {
      Date date;
      try {
         date = BanListEntry.DATE_FORMAT.parse(p_11096_);
      } catch (ParseException parseexception) {
         date = p_11097_;
      }

      return date;
   }

   static class ConversionError extends RuntimeException {
      ConversionError(String p_11182_, Throwable p_11183_) {
         super(p_11182_, p_11183_);
      }

      ConversionError(String p_11177_) {
         super(p_11177_);
      }
   }
}
