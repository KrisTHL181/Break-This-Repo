package com.mojang.authlib;

import java.util.UUID;

public interface ProfileLookupCallback {
    void onProfileLookupSucceeded(String profileName, UUID profileId);

    void onProfileLookupFailed(String profileName, Exception exception);
}
