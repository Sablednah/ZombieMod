package me.sablednah.zombiemod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.LivingEntity;

import net.minecraft.server.v1_7_R4.NBTTagCompound;


public class Equines {

	private NBTTagCompound	nbtTagCompound;

	public HorseType getType(LivingEntity ent) {
		this.nbtTagCompound = NBTUtil.getNBTTagCompound(ent);
		int htid = this.nbtTagCompound.getInt("Type");
		return HorseType.fromId(htid);
	}

	public static enum HorseType {
		NORMAL("normal", 0),
		DONKEY("donkey", 1),
		MULE("mule", 2),
		UNDEAD("undead", 3),
		SKELETAL("skeletal", 4);

		private String									name;
		private int										id;
		private static final Map<String, HorseType>		NAME_MAP;
		private static final Map<Integer, HorseType>	ID_MAP;

		static {
			NAME_MAP = new HashMap<String, HorseType>();
			ID_MAP = new HashMap<Integer, HorseType>();

			for (HorseType effect : values()) {
				NAME_MAP.put(effect.name, effect);
				ID_MAP.put(Integer.valueOf(effect.id), effect);
			}
		}

		private HorseType(String name, int id) {
			this.name = name;
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public int getId() {
			return this.id;
		}

		public static HorseType fromName(String name) {
			if (name == null) {
				return null;
			}
			for (Map.Entry<String, HorseType> e : NAME_MAP.entrySet()) {
				if (((String) e.getKey()).equalsIgnoreCase(name)) {
					return (HorseType) e.getValue();
				}
			}
			return null;
		}

		public static HorseType fromId(int id) {
			return (HorseType) ID_MAP.get(Integer.valueOf(id));
		}
	}

	private static class NBTUtil {

		@SuppressWarnings("rawtypes")
		public static NBTTagCompound getNBTTagCompound(Object entity) {
			try {
				NBTTagCompound nbtTagCompound = new NBTTagCompound();
				for (Method m : entity.getClass().getMethods()) {
					Class[] pt = m.getParameterTypes();
					if ((m.getName().equals("b")) && (pt.length == 1) && (pt[0].getName().contains("NBTTagCompound"))) {
						m.invoke(entity, new Object[] { nbtTagCompound });
					}
				}
				return nbtTagCompound;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
