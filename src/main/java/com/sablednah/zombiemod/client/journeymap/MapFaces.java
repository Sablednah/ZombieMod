package com.sablednah.zombiemod.client.journeymap;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * A genus's face as a small texture, for a map icon: the 8x8 face of its head's skin with the hat
 * layer over it, scaled to 32x32.
 *
 * <p><b>Read off the head the mob is wearing</b>, not the genus file, because that is what the mob
 * actually shows - the Ghost and the Corpse wear real players' faces, which no genus file knows.
 *
 * <p>The skin is fetched once per texture hash, on a background thread, from Mojang's texture server
 * - the same host a vanilla client already asks for every player head it draws, and nowhere else.
 * Until it arrives, or if it never does, the map keeps its own icon. A head given only as a player
 * name, with no texture, has no URL to fetch here and keeps the map's icon too.
 */
final class MapFaces {

    private static final Logger LOG = LogUtils.getLogger();

    private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();

    private static final ExecutorService FETCH = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ZombieMod map faces");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private MapFaces() {}

    /** The face texture for this mob, or null while it is not ready or there is none. */
    static Identifier of(Mob mob) {
        String url = skinUrl(mob);
        if (url == null) {
            return null;
        }
        String hash = url.substring(url.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
        if (!hash.matches("[0-9a-f]+")) {
            return null;
        }
        Identifier ready = READY.get(hash);
        if (ready == null && STARTED.add(hash)) {
            FETCH.execute(() -> fetch(hash));
        }
        return ready;
    }

    private static String skinUrl(Mob mob) {
        ResolvableProfile profile = mob.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.PROFILE);
        if (profile == null) {
            return null;
        }
        // Through the codec rather than the profile's accessors, which have moved twice across the
        // versions this ships for. The encoded form carries the textures property somewhere; any
        // string in it that decodes to a skin URL will do.
        return ResolvableProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).result()
                .map(SkinUrls::find).orElse(null);
    }

    private static void fetch(String hash) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + SkinUrls.HOST + "/texture/" + hash))
                    .timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                LOG.debug("ZombieMod: map face {} - HTTP {}", hash, response.statusCode());
                return;
            }
            NativeImage face;
            try (InputStream in = response.body(); NativeImage skin = NativeImage.read(in)) {
                face = crop(skin);
            }
            Identifier id = Identifier.fromNamespaceAndPath("zombiemod", "map_face/" + hash);
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getTextureManager().register(id,
                        new DynamicTexture(() -> "zombiemod map face " + hash, face));
                READY.put(hash, id);
            });
        } catch (Exception e) {
            // No face, the map's own icon instead. Not worth a warning: offline is a fine way to play.
            LOG.debug("ZombieMod: map face {} not fetched ({})", hash, e.toString());
        }
    }

    /**
     * Face at (8,8) and hat at (40,8), both 8x8, in every skin layout since the first; four pixels
     * per skin pixel, nearest-neighbour, because anything smoother invents pixels the head does not
     * have. The face is opaque; the hat covers it wherever the hat has anything.
     */
    private static NativeImage crop(NativeImage skin) {
        NativeImage out = new NativeImage(32, 32, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int base = skin.getPixel(8 + x / 4, 8 + y / 4) | 0xFF000000;
                int hat = skin.getPixel(40 + x / 4, 8 + y / 4);
                out.setPixel(x, y, (hat >>> 24) != 0 ? hat : base);
            }
        }
        return out;
    }
}
