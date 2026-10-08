package com.sablednah.zombiemod.platform;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Registering the mod's config.
 *
 * <p><b>Why this exists.</b> NeoForge 26.3.0.37-beta (FML 12.0.8) renamed {@code ModConfig.Type}:
 * {@code SERVER} became {@code SYNCED}. Either spelling is a {@code NoSuchFieldError} at load on the
 * other side of the rename, so the one line that names the type has to live per branch.
 *
 * <p>The file name is fixed at {@code zombiemod-server.toml} on every branch. The default name
 * follows the type, so a bare {@code SYNCED} would start a fresh {@code zombiemod-synced.toml} and
 * every existing server would silently lose its settings.
 */
public final class Configs {

    private Configs() {}

    /**
     * Registers the server-wide config as {@code config/zombiemod-server.toml}.
     *
     * <p><b>Differs per version.</b> On 26.3 (NeoForge .58+) the type is {@code SYNCED} and the
     * file name is passed explicitly.
     */
    public static void registerServer(ModContainer container, ModConfigSpec spec) {
        container.registerConfig(ModConfig.Type.SERVER, spec);
    }
}
