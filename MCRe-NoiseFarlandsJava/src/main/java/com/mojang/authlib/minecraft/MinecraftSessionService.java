package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.UUID;

public interface MinecraftSessionService {
    /**
     * Attempts to join the specified Minecraft server.
     * <p />
     * If this method returns without throwing an exception, the join was successful and a subsequent call to
     * {@link #hasJoinedServer(String, String, InetAddress)} will return true.
     *
     * @param profileId The player profile ID to join as
     * @param authenticationToken The authenticated token of the user
     * @param serverId The random ID of the server to join
     * @throws com.mojang.authlib.exceptions.AuthenticationUnavailableException Thrown when the servers return a malformed response, or are otherwise unavailable
     * @throws com.mojang.authlib.exceptions.InvalidCredentialsException Thrown when the specified authenticationToken is invalid
     * @throws com.mojang.authlib.exceptions.AuthenticationException Generic exception indicating that we could not authenticate the user
     */
    void joinServer(UUID profileId, String authenticationToken, String serverId) throws AuthenticationException;

    /**
     * Checks if the specified user has joined a Minecraft server.
     * <p />
     *
     * @param profileName The player name to check for
     * @param serverId The random ID of the server to check for
     * @param address The address connected from
     * @throws com.mojang.authlib.exceptions.AuthenticationUnavailableException Thrown when the servers return a malformed response, or are otherwise unavailable
     * @return Full game profile if the user had joined, otherwise null
     */
    @Nullable
    ProfileResult hasJoinedServer(String profileName, String serverId, @Nullable InetAddress address) throws AuthenticationUnavailableException;

    /**
     * Gets the packed property representation of any textures contained in the given profile.
     *
     * @param profile the profile to get textures from
     * @return the packed property containing texture data, or {@code null} if this profile does not have any
     *
     * @see MinecraftSessionService#unpackTextures(Property)
     * @see MinecraftSessionService#getTextures(GameProfile)
     */
    @Nullable
    Property getPackedTextures(GameProfile profile);

    /**
     * Unpacks the texture data contained in the given packed profile property.
     *
     * @param packedTextures the raw texture data to unpack
     * @return the unpacked set of textures, or {@link MinecraftProfileTextures#EMPTY} if the data was malformed
     * @see MinecraftSessionService#getPackedTextures(GameProfile)
     * @see MinecraftSessionService#getTextures(GameProfile)
     */
    MinecraftProfileTextures unpackTextures(Property packedTextures);

    /**
     * Gets and unpacks any textures contains in the given profile.
     *
     * @param profile the profile to get textures from
     * @return the unpacked set of textures, or {@link MinecraftProfileTextures#EMPTY} if the data was missing or malformed
     * @see MinecraftSessionService#getPackedTextures(GameProfile)
     * @see MinecraftSessionService#unpackTextures(Property)
     */
    default MinecraftProfileTextures getTextures(final GameProfile profile) {
        final Property packed = getPackedTextures(profile);
        return packed != null ? unpackTextures(packed) : MinecraftProfileTextures.EMPTY;
    }

    /**
     * Fetches the profile information associated with the given ID from the session service.
     * This will include all properties associated with the profile.
     * <p/>
     * The profile must have an ID. If no information is found, nothing will be done.
     *
     * @param profileId     The ID of the game profile to request.
     * @param requireSecure If the profile property map should include verifiable signature information.
     * @return Fetched profile for the requested user, or {@code null} if unsuccessful or the user did not exist.
     */
    @Nullable
    ProfileResult fetchProfile(UUID profileId, boolean requireSecure);

    /**
     * Verifies the signature and returns the value of a {@link com.mojang.authlib.properties.Property}.
     *
     * @param property Property to return the value of.
     * @return String value
     * @throws com.mojang.authlib.minecraft.InsecurePublicKeyException If data is insecure or missing
     */
    String getSecurePropertyValue(Property property) throws InsecurePublicKeyException;
}
