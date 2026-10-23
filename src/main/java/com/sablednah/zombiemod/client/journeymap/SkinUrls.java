package com.sablednah.zombiemod.client.journeymap;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Finds the skin URL in an encoded player profile. No client classes, on purpose: it is plain
 * JSON work, and keeping it apart from {@link MapFaces} (which draws textures) is what lets it be
 * checked on a dedicated server, where any class naming a texture fails to load.
 */
final class SkinUrls {

    /** The only host a skin is fetched from - the one vanilla fetches every player head from. */
    static final String HOST = "textures.minecraft.net";

    private SkinUrls() {}

    /** The first skin URL anywhere in this JSON, or null. Any string that decodes to one counts. */
    static String find(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return decode(json.getAsString());
        }
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                String url = find(e);
                if (url != null) {
                    return url;
                }
            }
        } else if (json.isJsonObject()) {
            for (var e : json.getAsJsonObject().entrySet()) {
                String url = find(e.getValue());
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    /** The skin URL inside a textures property value, or null if this string is not one. */
    private static String decode(String value) {
        try {
            String text = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            String url = root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            return HOST.equals(URI.create(url).getHost()) ? url : null;
        } catch (RuntimeException notOne) {
            return null;
        }
    }
}
