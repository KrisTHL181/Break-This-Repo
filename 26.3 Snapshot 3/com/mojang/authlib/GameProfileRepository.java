package com.mojang.authlib;

import com.mojang.authlib.services.response.NameAndId;
import java.util.Optional;

public interface GameProfileRepository {
   void findProfilesByNames(String[] var1, ProfileLookupCallback var2);

   Optional<NameAndId> findProfileByName(String var1);
}
