package com.mojang.authlib;

import java.util.UUID;

public interface ProfileLookupCallback {
   void onProfileLookupSucceeded(String var1, UUID var2);

   void onProfileLookupFailed(String var1, Exception var2);
}
