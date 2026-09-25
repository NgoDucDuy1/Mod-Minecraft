package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player cultivation data: Qi pool (linh lực), realm (cảnh giới), cultivation
 * experience (tu vi), learned skills, hot-bar skill slots, cooldowns and any
 * currently running multi-tick skill casts.
 *
 * <p>The same class is used on both sides; the server is authoritative and pushes
 * a snapshot to the owning client whenever something changes.</p>
 */
public class PlayerQi {
	public static final int SLOT_COUNT = 6;

	private float qi;
	private Realm realm = Realm.QI_REFINING;
	private int exp;
	private final Set<Identifier> learned = new LinkedHashSet<>();
	private final Identifier[] slots = new Identifier[SLOT_COUNT];
	private final Map<Identifier, Integer> cooldowns = new HashMap<>();
	private final Map<Identifier, Integer> maxCooldowns = new HashMap<>();

	/** Server only: running casts (channels, multi-hit skills, delayed strikes). */
	private final List<ActiveCast> activeCasts = new ArrayList<>();
	/** Skill ids with a running cast, as last synced from the server (client-side view). */
	private final Set<Identifier> syncedActive = new HashSet<>();

	/** Ticks the player has continuously meditated (sneaking still). */
	private int meditateTicks;
	private boolean dirty = true;

	// ------------------------------------------------------------------ qi

	public float getQi() {
		return qi;
	}

	public float getMaxQi() {
		return realm.getMaxQi();
	}

	public void setQi(float value) {
		float clamped = MathHelper.clamp(value, 0f, getMaxQi());
		if (clamped != qi) {
			qi = clamped;
			dirty = true;
		}
	}

	public void addQi(float amount) {
		setQi(qi + amount);
	}

	public boolean hasQi(float amount) {
		return qi >= amount;
	}

	public boolean consumeQi(float amount) {
		if (qi < amount) return false;
		setQi(qi - amount);
		return true;
	}

	// --------------------------------------------------------------- realm

	public Realm getRealm() {
		return realm;
	}

	public void setRealm(Realm realm) {
		if (this.realm != realm) {
			this.realm = realm;
			this.qi = Math.min(this.qi, realm.getMaxQi());
			dirty = true;
		}
	}

	public int getExp() {
		return exp;
	}

	public void setExp(int exp) {
		int v = Math.max(0, exp);
		if (v != this.exp) {
			this.exp = v;
			dirty = true;
		}
	}

	public void addExp(int amount) {
		setExp(exp + amount);
	}

	/** Exp needed to reach the next realm, or -1 when at the highest realm. */
	public int getExpForBreakthrough() {
		return realm.isMax() ? -1 : realm.next().getRequiredExp();
	}

	public boolean canBreakthrough() {
		return !realm.isMax() && exp >= realm.next().getRequiredExp();
	}

	public int getMeditateTicks() {
		return meditateTicks;
	}

	public void setMeditateTicks(int ticks) {
		this.meditateTicks = ticks;
	}

	// -------------------------------------------------------------- skills

	public Set<Identifier> getLearned() {
		return learned;
	}

	public boolean hasLearned(Identifier skillId) {
		return learned.contains(skillId);
	}

	public boolean learn(Identifier skillId) {
		if (SkillRegistry.get(skillId) == null) return false;
		if (learned.add(skillId)) {
			// Auto-equip into the first empty slot for convenience.
			for (int i = 0; i < SLOT_COUNT; i++) {
				if (slots[i] == null) {
					slots[i] = skillId;
					break;
				}
			}
			dirty = true;
			return true;
		}
		return false;
	}

	public void forget(Identifier skillId) {
		if (learned.remove(skillId)) {
			for (int i = 0; i < SLOT_COUNT; i++) {
				if (skillId.equals(slots[i])) slots[i] = null;
			}
			dirty = true;
		}
	}

	@Nullable
	public Identifier getSlot(int index) {
		if (index < 0 || index >= SLOT_COUNT) return null;
		return slots[index];
	}

	@Nullable
	public Skill getSlotSkill(int index) {
		Identifier id = getSlot(index);
		return id == null ? null : SkillRegistry.get(id);
	}

	public void setSlot(int index, @Nullable Identifier skillId) {
		if (index < 0 || index >= SLOT_COUNT) return;
		if (skillId != null && !learned.contains(skillId)) return;
		// A skill may only occupy one slot.
		if (skillId != null) {
			for (int i = 0; i < SLOT_COUNT; i++) {
				if (i != index && skillId.equals(slots[i])) slots[i] = null;
			}
		}
		slots[index] = skillId;
		dirty = true;
	}

	// ------------------------------------------------------------ cooldowns

	public int getCooldown(Identifier skillId) {
		return cooldowns.getOrDefault(skillId, 0);
	}

	public int getMaxCooldown(Identifier skillId) {
		return maxCooldowns.getOrDefault(skillId, 0);
	}

	public boolean isOnCooldown(Identifier skillId) {
		return getCooldown(skillId) > 0;
	}

	public void setCooldown(Identifier skillId, int ticks) {
		if (ticks <= 0) {
			cooldowns.remove(skillId);
			maxCooldowns.remove(skillId);
		} else {
			cooldowns.put(skillId, ticks);
			maxCooldowns.put(skillId, ticks);
		}
		dirty = true;
	}

	/** Cooldown progress from 1 (just used) to 0 (ready). */
	public float getCooldownProgress(Identifier skillId) {
		int max = getMaxCooldown(skillId);
		if (max <= 0) return 0f;
		return MathHelper.clamp(getCooldown(skillId) / (float) max, 0f, 1f);
	}

	/** Decrements cooldowns. Safe to call on both sides (client predicts). */
	public void tickCooldowns() {
		if (cooldowns.isEmpty()) return;
		Iterator<Map.Entry<Identifier, Integer>> it = cooldowns.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Identifier, Integer> e = it.next();
			int v = e.getValue() - 1;
			if (v <= 0) {
				it.remove();
				maxCooldowns.remove(e.getKey());
				dirty = true;
			} else {
				e.setValue(v);
			}
		}
	}

	// ---------------------------------------------------------- active casts

	public List<ActiveCast> getActiveCasts() {
		return activeCasts;
	}

	public void addActiveCast(ActiveCast cast) {
		activeCasts.add(cast);
	}

	public boolean hasActiveCast(Identifier skillId) {
		for (ActiveCast c : activeCasts) {
			if (c.getSkillId().equals(skillId) && !c.isFinished()) return true;
		}
		return syncedActive.contains(skillId);
	}

	@Nullable
	public ActiveCast getActiveCast(Identifier skillId) {
		for (ActiveCast c : activeCasts) {
			if (c.getSkillId().equals(skillId) && !c.isFinished()) return c;
		}
		return null;
	}

	/** True while any channelled skill is running (prevents starting another one). */
	public boolean isChanneling() {
		for (ActiveCast c : activeCasts) {
			if (c.isChannel() && !c.isFinished()) return true;
		}
		return false;
	}

	public void interruptAllCasts() {
		for (ActiveCast c : activeCasts) {
			c.cancel();
		}
	}

	// ---------------------------------------------------------------- dirty

	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		dirty = true;
	}

	public void clearDirty() {
		dirty = false;
	}

	// ----------------------------------------------------------------- nbt

	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putFloat("Qi", qi);
		nbt.putInt("Realm", realm.getLevel());
		nbt.putInt("Exp", exp);
		NbtList learnedList = new NbtList();
		for (Identifier id : learned) {
			learnedList.add(NbtString.of(id.toString()));
		}
		nbt.put("Learned", learnedList);
		NbtList slotList = new NbtList();
		for (int i = 0; i < SLOT_COUNT; i++) {
			slotList.add(NbtString.of(slots[i] == null ? "" : slots[i].toString()));
		}
		nbt.put("Slots", slotList);
		NbtCompound cd = new NbtCompound();
		for (Map.Entry<Identifier, Integer> e : cooldowns.entrySet()) {
			cd.putInt(e.getKey().toString(), e.getValue());
		}
		nbt.put("Cooldowns", cd);
		NbtCompound mcd = new NbtCompound();
		for (Map.Entry<Identifier, Integer> e : maxCooldowns.entrySet()) {
			mcd.putInt(e.getKey().toString(), e.getValue());
		}
		nbt.put("MaxCooldowns", mcd);
		NbtList active = new NbtList();
		for (ActiveCast c : activeCasts) {
			if (!c.isFinished()) active.add(NbtString.of(c.getSkillId().toString()));
		}
		nbt.put("Active", active);
		return nbt;
	}

	public void readNbt(NbtCompound nbt) {
		realm = Realm.byLevel(nbt.getInt("Realm"));
		qi = MathHelper.clamp(nbt.getFloat("Qi"), 0f, realm.getMaxQi());
		exp = nbt.getInt("Exp");
		learned.clear();
		NbtList learnedList = nbt.getList("Learned", NbtElement.STRING_TYPE);
		for (int i = 0; i < learnedList.size(); i++) {
			Identifier id = Identifier.tryParse(learnedList.getString(i));
			if (id != null) learned.add(id);
		}
		NbtList slotList = nbt.getList("Slots", NbtElement.STRING_TYPE);
		for (int i = 0; i < SLOT_COUNT; i++) {
			slots[i] = null;
			if (i < slotList.size()) {
				String s = slotList.getString(i);
				if (!s.isEmpty()) slots[i] = Identifier.tryParse(s);
			}
		}
		cooldowns.clear();
		maxCooldowns.clear();
		NbtCompound cd = nbt.getCompound("Cooldowns");
		for (String key : cd.getKeys()) {
			Identifier id = Identifier.tryParse(key);
			if (id != null) cooldowns.put(id, cd.getInt(key));
		}
		NbtCompound mcd = nbt.getCompound("MaxCooldowns");
		for (String key : mcd.getKeys()) {
			Identifier id = Identifier.tryParse(key);
			if (id != null) maxCooldowns.put(id, mcd.getInt(key));
		}
		syncedActive.clear();
		NbtList active = nbt.getList("Active", NbtElement.STRING_TYPE);
		for (int i = 0; i < active.size(); i++) {
			Identifier id = Identifier.tryParse(active.getString(i));
			if (id != null) syncedActive.add(id);
		}
		dirty = true;
	}

	/** Copies persistent state (used on respawn / dimension change). */
	public void copyFrom(PlayerQi other) {
		NbtCompound nbt = other.writeNbt(new NbtCompound());
		readNbt(nbt);
	}
}
