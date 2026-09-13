package com.mojang.authlib.services;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpDiscoveryService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.services.response.NameAndId;
import com.mojang.authlib.services.response.ProfileSearchResultsResponse;
import com.mojang.authlib.services.response.discovery.Service;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftServicesProfileRepository implements GameProfileRepository {
   private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServicesProfileRepository.class);
   private static final int ENTRIES_PER_PAGE = 10;
   private static final int MAX_FAIL_COUNT = 3;
   private static final int DELAY_BETWEEN_PAGES = 100;
   private static final int DELAY_BETWEEN_FAILURES = 750;
   private final MinecraftClient client;
   private final MinecraftServicesDiscoveryService discoveryService;

   public MinecraftServicesProfileRepository(Proxy proxy, MinecraftServicesDiscoveryService discoveryService) {
      this.client = MinecraftClient.unauthenticated(proxy);
      this.discoveryService = discoveryService;
   }

   @Override
   public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
      Set<String> criteria = Arrays.stream(names).filter(namex -> !Strings.isNullOrEmpty(namex)).collect(Collectors.toSet());

      for (List<String> request : Iterables.partition(criteria, 10)) {
         List<String> normalizedRequest = request.stream().map(MinecraftServicesProfileRepository::normalizeName).toList();
         int failCount = 0;

         boolean failed;
         do {
            failed = false;

            try {
               ProfileSearchResultsResponse response = this.client
                  .post(
                     MinecraftServicesDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PROFILES, "getManyByName")),
                     normalizedRequest,
                     ProfileSearchResultsResponse.class
                  );
               List<NameAndId> results = response != null ? response.profiles() : List.of();
               failCount = 0;
               LOGGER.debug("{} results returned, parsing", results.size());
               Set<String> received = new HashSet<>(results.size());

               for (NameAndId profile : results) {
                  LOGGER.debug("Successfully looked up profile {}", profile);
                  received.add(normalizeName(profile.name()));
                  callback.onProfileLookupSucceeded(profile.name(), profile.id());
               }

               for (String name : request) {
                  if (!received.contains(normalizeName(name))) {
                     LOGGER.debug("Couldn't find profile {}", name);
                     callback.onProfileLookupFailed(name, new ProfileNotFoundException("Server did not find the requested profile"));
                  }
               }

               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var15) {
               }
            } catch (MinecraftClientException var16) {
               MinecraftClientException e = var16;
               if (++failCount == 3) {
                  for (String name : request) {
                     LOGGER.debug("Couldn't find profile {} because of a server error", name);
                     callback.onProfileLookupFailed(name, e.toAuthenticationException());
                  }
               } else {
                  try {
                     Thread.sleep(750L);
                  } catch (InterruptedException var14) {
                  }

                  failed = true;
               }
            }
         } while (failed);
      }
   }

   @Override
   public Optional<NameAndId> findProfileByName(String name) {
      try {
         return Optional.ofNullable(
            this.client
               .get(
                  HttpDiscoveryService.constantURL(this.discoveryService.getUrl(Service.PROFILES, "getByName").replace("{name}", normalizeName(name))),
                  NameAndId.class
               )
         );
      } catch (MinecraftClientException e) {
         LOGGER.warn("Couldn't find profile with name: {}", name, e);
         return Optional.empty();
      }
   }

   private static String normalizeName(String name) {
      return name.toLowerCase(Locale.ROOT);
   }
}
