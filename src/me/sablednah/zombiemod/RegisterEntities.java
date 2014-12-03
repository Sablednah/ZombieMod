package me.sablednah.zombiemod;

import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_7_R4.BiomeBase;
import net.minecraft.server.v1_7_R4.BiomeMeta;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.EntityTypes;
import net.minecraft.server.v1_7_R4.GroupDataEntity;
import net.minecraft.server.v1_7_R4.World;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import me.sablednah.zombiemod.ReflectionUtils;


/*
 old code


 // Custom NMS stuff here :/
 try {
 @SuppressWarnings("rawtypes")
 Class[] args = new Class[3];
 args[0] = Class.class;
 args[1] = String.class;
 args[2] = int.class;
 Method a = net.minecraft.server.v1_7_R1.EntityTypes.class.getDeclaredMethod("a", args);
 a.setAccessible(true);
 a.invoke(a, ZombieType.class, "Zombie", 54);
 a.invoke(a, ZombieGiantType.class, "Giant", 53);
 a.invoke(a, AngryGolem.class, "VillagerGolem", 99);
 } catch (Exception e) {
 e.printStackTrace();
 this.setEnabled(false);
 }


 */

public enum RegisterEntities {

	// CREEPER("Creeper", 50, EntityType.CREEPER, net.minecraft.server.v1_7_R1.EntityCreeper.class,
	// me.sablednah.zombiemod.EntityCreeper.class),
	// ENDERMAN("Enderman", 58, EntityType.ENDERMAN, net.minecraft.server.v1_7_R1.EntityEnderman.class,
	// me.sablednah.zombiemod.EntityEnderman.class),
	// SKELETON("Skeleton", 51, EntityType.SKELETON, net.minecraft.server.v1_7_R1.EntitySkeleton.class,
	// me.sablednah.zombiemod.EntitySkeleton.class),
	// SPIDER("Spider", 52, EntityType.SPIDER, net.minecraft.server.v1_7_R1.EntitySpider.class,
	// me.sablednah.zombiemod.EntitySpider.class),
	// GHAST("Ghast", 56, EntityType.GHAST, net.minecraft.server.v1_7_R1.EntityGhast.class,
	// me.sablednah.zombiemod.EntityGhast.class),
	// BLAZE("Blaze", 61, EntityType.BLAZE, net.minecraft.server.v1_7_R1.EntityBlaze.class,
	// me.sablednah.zombiemod.EntityBlaze.class),
	// WITHER("WitherBoss", 64, EntityType.WITHER, net.minecraft.server.v1_7_R1.EntityWither.class,
	// me.sablednah.zombiemod.EntityWither.class),
	// WITCH("Witch", 66, EntityType.WITCH, net.minecraft.server.v1_7_R1.EntityWitch.class,
	// me.sablednah.zombiemod.EntityWitch.class),
	ZOMBIE("Zombie", 54, EntityType.ZOMBIE, net.minecraft.server.v1_7_R4.EntityZombie.class, me.sablednah.zombiemod.ZombieType.class),
	GIANT_ZOMBIE("Giant", 53, EntityType.GIANT, net.minecraft.server.v1_7_R4.EntityGiantZombie.class, me.sablednah.zombiemod.ZombieGiantType.class),
	IRON_GOLEM("VillagerGolem", 99, EntityType.IRON_GOLEM, net.minecraft.server.v1_7_R4.EntityIronGolem.class, me.sablednah.zombiemod.AngryGolem.class);

	private String								name;
	private int									id;
	private EntityType							entityType;
	private Class<? extends EntityInsentient>	nmsClass;
	private Class<? extends EntityInsentient>	zmClass;

	private static boolean						registered	= false;

	private RegisterEntities(String name, int id, EntityType entityType, Class<? extends EntityInsentient> nmsClass, Class<? extends EntityInsentient> zmClass) {
		this.name = name;
		this.id = id;
		this.entityType = entityType;
		this.nmsClass = nmsClass;
		this.zmClass = zmClass;
	}

	@SuppressWarnings("unchecked")
	public static void registerEntities() {
		if (registered) {
			ZombieMod.logger.warning("Already Registered");
			return;
		}

		Map<String, Class<?>> nameMap;
		Map<Integer, Class<?>> idMap;

		try {
			nameMap = ReflectionUtils.getFieldValue(EntityTypes.class, "c", Map.class, null);
			idMap = ReflectionUtils.getFieldValue(EntityTypes.class, "e", Map.class, null);
		}
		catch (Exception e) {
			ZombieMod.logger.warning("Failed to get existing entity maps");
			return;
		}

		for (RegisterEntities entity : values()) {
			try {
				nameMap.remove(entity.getName());
				idMap.remove(entity.getID());

				ReflectionUtils.invokeMethod(EntityTypes.class, "a", Void.class, null, new Class<?>[] { Class.class, String.class, int.class }, new Object[] { entity.getZmClass(), entity.getName(), entity.getID() });
			}
			catch (Exception e) {
				ZombieMod.logger.warning("Failed to call EntityTypes.a() for " + entity.getName());
				return;
			}
		}

		for (BiomeBase biomeBase : BiomeBase.getBiomes()) {
			if (biomeBase == null) {
				break;
			}

			for (String field : new String[] { "as", "at", "au", "av" }) {
				try {
					List<BiomeMeta> mobList = ReflectionUtils.getFieldValue(BiomeBase.class, field, List.class, biomeBase);

					for (BiomeMeta meta : mobList) {
						for (RegisterEntities entity : values()) {
							if (entity.getNMSClass().equals(meta.b)) {
								meta.b = entity.getZmClass();
							}
						}
					}
				}
				catch (Exception e) {
					ZombieMod.logger.warning("Failed to modify biome data field " + field);
					return;
				}
			}
		}

		registered = true;
	}

	public String getName() {
		return this.name;
	}

	public int getID() {
		return this.id;
	}

	public EntityType getEntityType() {
		return this.entityType;
	}

	public Class<? extends EntityInsentient> getNMSClass() {
		return this.nmsClass;
	}

	public Class<? extends EntityInsentient> getZmClass() {
		return this.zmClass;
	}

	private EntityInsentient createEntity(World world) {
		try {
			return this.getZmClass().getConstructor(World.class).newInstance(world);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void spawnEntity(Location location) {
		World world = ((CraftWorld) location.getWorld()).getHandle();

		EntityInsentient entity = this.createEntity(world);
		entity.setPositionRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		entity.prepare((GroupDataEntity) null);
		world.addEntity(entity, SpawnReason.CUSTOM);
		entity.p();
	}

}
