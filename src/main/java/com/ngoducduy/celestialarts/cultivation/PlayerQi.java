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
 * Per-player cultivation data: Qi pool (linh lực), realm and minor stage (cảnh giới), cultivation
 * experience (tu vi) inside the current stage, the innate spirit root and talent, learned skills,
 * hot-bar skill slots, cooldowns and any currently running multi-tick skill casts.
 *
 * <p>The same class is used on both sides; the server is authoritative and pushes
 * a snapshot to the owning client whenever something changes.</p>
 */
public class PlayerQi {
	public static final int SLOT_COUNT = 6;

	private float qi;
	private Realm realm = Realm.QI_REFINING;
	private Stage stage = Stage.EARLY;
	private int exp;
	/** Fractional cultivation gain from meditation waiting to become a whole point. */
	private float expBuffer;
	/** Null until the root awakens on the first join. */
	@Nullable
	private SpiritRoot root;
	private Talent talent = Talent.MORTAL_BODY;
	/** Server: sitting on a meditation seat. Synced so the HUD can show the trance. */
	private boolean meditating;
	/** Spiritual qi density at the player's position, sampled by the server every second. */
	private float spiritQi = 1.0F;
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
		return CultivationStats.maxQi(this);
	}

	/** Qi regenerated per tick out of combat (before meditation / spirit-qi bonuses). */
	public float getRegenPerTick() {
		return CultivationStats.regenPerTick(this);
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

	/** Qi a skill costs this cultivator: cheaper for elements the spirit root is attuned to. */
	public float qiCost(Skill skill) {
		return skill.getQiCost() * CultivationStats.qiCostMultiplier(this, skill.getElement());
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
			this.qi = Math.min(this.qi, getMaxQi());
			dirty = true;
		}
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		if (this.stage != stage) {
			this.stage = stage;
			this.qi = Math.min(this.qi, getMaxQi());
			dirty = true;
		}
	}

	/** Realm and stage as one number: 1 (Luyện Khí sơ kỳ) … 24 (Độ Kiếp viên mãn). */
	public int getRank() {
		return (realm.getLevel() - 1) * Stage.values().length + stage.getIndex() + 1;
	}

	/** True at the very top: Độ Kiếp viên mãn. */
	public boolean isAtPeakOfCultivation() {
		return realm.isMax() && stage.isPeak();
	}

	@Nullable
	public SpiritRoot getRoot() {
		return root;
	}

	public boolean hasAwakened() {
		return root != null;
	}

	public void setRoot(@Nullable SpiritRoot root) {
		this.root = root;
		this.qi = Math.min(this.qi, getMaxQi());
		dirty = true;
	}

	public Talent getTalent() {
		return talent;
	}

	public void setTalent(Talent talent) {
		if (this.talent != talent) {
			this.talent = talent;
			this.qi = Math.min(this.qi, getMaxQi());
			dirty = true;
		}
	}

	public boolean isMeditating() {
		return meditating;
	}

	public void setMeditating(boolean meditating) {
		if (this.meditating != meditating) {
			this.meditating = meditating;
			dirty = true;
		}
	}

	public float getSpiritQi() {
		return spiritQi;
	}

	public void setSpiritQi(float spiritQi) {
		if (Math.abs(this.spiritQi - spiritQi) > 0.005F) {
			this.spiritQi = spiritQi;
			dirty = true;
		}
	}

	/** Adds fractional cultivation gain; whole points are moved into {@link #getExp()}. */
	public void addExpFraction(float amount) {
		expBuffer += amount;
		if (expBuffer >= 1.0F) {
			int whole = (int) expBuffer;
			expBuffer -= whole;
			addExp(whole);
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

	/** Exp needed to leave the current stage, or -1 at Độ Kiếp viên mãn. */
	public int getExpForBreakthrough() {
		return isAtPeakOfCultivation() ? -1 : realm.getStageExp();
	}

	public boolean canBreakthrough() {
		return !isAtPeakOfCultivation() && exp >= realm.getStageExp();
	}

	/** Whether the next breakthrough is the great one (peak stage → next realm, with a tribulation). */
	public boolean nextBreakthroughIsTribulation() {
		return stage.isPeak() && !realm.isMax();
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
		nbt.putInt("Stage", stage.getIndex());
		nbt.putInt("Exp", exp);
		nbt.putFloat("ExpBuffer", expBuffer);
		if (root != null) nbt.put("Root", root.writeNbt(new NbtCompound()));
		nbt.putString("Talent", talent.getKey());
		nbt.putBoolean("Meditating", meditating);
		nbt.putFloat("SpiritQi", spiritQi);
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
		stage = Stage.byIndex(nbt.getInt("Stage"));
		root = nbt.contains("Root", NbtElement.COMPOUND_TYPE) ? SpiritRoot.readNbt(nbt.getCompound("Root")) : null;
		talent = nbt.contains("Talent", NbtElement.STRING_TYPE) ? Talent.byKey(nbt.getString("Talent")) : Talent.MORTAL_BODY;
		meditating = nbt.getBoolean("Meditating");
		spiritQi = nbt.contains("SpiritQi", NbtElement.FLOAT_TYPE) ? nbt.getFloat("SpiritQi") : 1.0F;
		exp = nbt.getInt("Exp");
		expBuffer = nbt.getFloat("ExpBuffer");
		qi = MathHelper.clamp(nbt.getFloat("Qi"), 0f, getMaxQi());
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
