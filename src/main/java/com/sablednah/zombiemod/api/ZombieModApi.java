package com.sablednah.zombiemod.api;

import java.util.Optional;

import com.sablednah.zombiemod.ZombieModRegistries;
import com.sablednah.zombiemod.core.Genus;
import com.sablednah.zombiemod.neoforge.GenusApplier;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;

/**
 * For other mods: make a mob they spawn into a ZombieMod genus. <b>Public contract</b> - these
 * signatures stay put across releases, and every type in them is vanilla, so a caller can reach
 * this by reflection or compile against it {@code compileOnly} without a hard dependency.
 *
 * <p>Asked for by ZARP so CrawlSpace's dungeons can place Patients in labs and Breakers in bunkers.
 * <b>A genus's spawn conditions do not apply here</b> - depth, light, district. The caller has
 * already decided where the mob goes; conditions are for the random draws, not for a mob somebody
 * named.
 *
 * <p><b>Server thread only.</b> Not from a worldgen thread: dressing a mob touches the server
 * scoreboard (glow colours), which world generation must not.
 *
 * <p><b>Order.</b> Call before {@code finalizeSpawn} and before {@code addFreshEntity}:
 * <ol>
 *   <li>Before {@code finalizeSpawn}, because ZombieMod's own spawn hook gives a random genus to a
 *       mob that has none - and leaves alone one that already has one. Dress it first and the
 *       hook steps aside.</li>
 *   <li>Before or after {@code addFreshEntity} both work. Before, the genus's AI is built when the
 *       mob joins the level, as for every genus; after, it is built here, at once.</li>
 * </ol>
 * Everything else - the dex "met" on hurt, kills, bounties, boss bars, Mob Drops - follows from the
 * genus key on the mob, the same as for one ZombieMod spawned itself.
 */
public final class ZombieModApi {

    private ZombieModApi() {}

    /**
     * Dresses an existing mob as a genus: attributes, equipment, head, name, glow, the
     * {@code zombiemod:genus} key, and its goals and abilities.
     *
     * @param mob   a mob on a {@link ServerLevel}, not yet given a different genus
     * @param genus e.g. {@code zombiemod:patient_zero}
     * @return false, changing nothing, if the genus is unknown, its base type is not this mob's
     *         type, the mob is not on a server level, or it already belongs to a <em>different</em>
     *         genus (re-dressing would leave the old one's attributes behind). True if it is now
     *         that genus, including when it already was.
     */
    public static boolean applyGenus(Mob mob, Identifier genus) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<Holder.Reference<Genus>> holder = level.registryAccess()
                .lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, genus));
        if (holder.isEmpty() || holder.get().value().base() != mob.getType()) {
            return false;
        }
        Optional<String> current = mob.getPersistentData().getString(GenusApplier.GENUS_TAG);
        if (current.isPresent()) {
            return current.get().equals(genus.toString());
        }
        GenusApplier.assign(mob, holder.get());
        // Before it joins a level, the join event builds the AI. After, nothing will - so here.
        // Never both: for a genus that keeps vanilla's goals, building twice doubles them.
        if (mob.isAddedToLevel()) {
            GenusApplier.applyAi(mob, holder.get().value());
        }
        return true;
    }

    /**
     * Makes the genus's base mob and dresses it, ready to place and add. Not added to the level,
     * and {@code finalizeSpawn} not called - the caller does both, in that order, as for any mob
     * it creates.
     *
     * @return empty if the genus is unknown or its base is not a mob
     */
    public static Optional<Mob> create(ServerLevel level, Identifier genus, EntitySpawnReason reason) {
        Optional<Holder.Reference<Genus>> holder = level.registryAccess()
                .lookupOrThrow(ZombieModRegistries.GENUS)
                .get(ResourceKey.create(ZombieModRegistries.GENUS, genus));
        if (holder.isEmpty() || !(holder.get().value().base().create(level, reason) instanceof Mob mob)) {
            return Optional.empty();
        }
        return applyGenus(mob, genus) ? Optional.of(mob) : Optional.empty();
    }

    /** The genus this mob belongs to, if any. */
    public static Optional<Identifier> genusOf(Mob mob) {
        return mob.getPersistentData().getString(GenusApplier.GENUS_TAG).map(Identifier::tryParse);
    }
}
