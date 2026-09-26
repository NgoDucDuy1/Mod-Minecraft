package com.ngoducduy.celestialarts.skill;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.Realm;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Base class for every skill (thần thông / công pháp).
 *
 * <p>Skills are stateless singletons. Anything that lasts more than a single tick
 * is expressed through an {@link com.ngoducduy.celestialarts.skill.cast.ActiveCast}
 * that the skill adds to the player's {@link PlayerQi}.</p>
 */
public abstract class Skill {
	/** Static configuration of a skill. */
	public record Settings(Element element, SkillType type, Realm realm, float qiCost, int cooldownTicks) {
		public static Settings of(Element element, SkillType type, Realm realm, float qiCost, int cooldownTicks) {
			return new Settings(element, type, realm, qiCost, cooldownTicks);
		}
	}

	private Identifier id;
	private final Settings settings;

	protected Skill(Settings settings) {
		this.settings = settings;
	}

	/** Called once by {@link SkillRegistry} when the skill is registered. */
	void setId(Identifier id) {
		if (this.id != null) throw new IllegalStateException("Skill already registered: " + this.id);
		this.id = id;
	}

	public final Identifier getId() {
		return id;
	}

	public final Element getElement() {
		return settings.element();
	}

	public final SkillType getType() {
		return settings.type();
	}

	public final Realm getRealm() {
		return settings.realm();
	}

	public float getQiCost() {
		return settings.qiCost();
	}

	public int getCooldownTicks() {
		return settings.cooldownTicks();
	}

	/** Channelled skills keep running while the key is held. */
	public boolean isChannel() {
		return false;
	}

	/** True when the skill can be used while riding a flying sword. */
	public boolean canUseWhileRiding() {
		return true;
	}

	public String getTranslationKey() {
		return "skill." + CelestialArts.MOD_ID + "." + id.getPath();
	}

	public Text getName() {
		return Text.translatable(getTranslationKey()).formatted(getElement().getFormatting());
	}

	public Text getDescription() {
		return Text.translatable(getTranslationKey() + ".desc");
	}

	public Identifier getIconTexture() {
		return CelestialArts.id("textures/gui/skills/" + id.getPath() + ".png");
	}

	/**
	 * Cheap gating that can run on both sides so the client can grey out slots.
	 */
	public boolean isUsable(PlayerEntity player, PlayerQi qi) {
		if (!qi.hasLearned(id)) return false;
		if (qi.getRealm().getLevel() < getRealm().getLevel()) return false;
		if (qi.isOnCooldown(id)) return false;
		if (!qi.hasQi(getQiCost())) return false;
		if (player.hasVehicle() && !canUseWhileRiding()) return false;
		return true;
	}

	/**
	 * Server-side activation. Return true when the cast succeeded so that Qi is
	 * consumed and the cooldown is applied. Implementations should spawn their
	 * own effects (sounds, particles, FX packets, entities, active casts).
	 */
	public abstract boolean activate(SkillContext ctx);

	/** Called when the player releases the key of a channelled skill. */
	public void release(SkillContext ctx) {
	}

	@Override
	public String toString() {
		return "Skill{" + id + "}";
	}
}
