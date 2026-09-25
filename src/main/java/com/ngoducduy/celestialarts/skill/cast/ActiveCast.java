package com.ngoducduy.celestialarts.skill.cast;

import com.ngoducduy.celestialarts.skill.Skill;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * A server-side, multi-tick piece of skill logic owned by a player.
 *
 * <p>Examples: a channelled beam, the nine strikes of a tribulation, a shield
 * that lasts ten seconds, a lotus that charges in the caster's palm.</p>
 */
public abstract class ActiveCast {
	protected final ServerPlayerEntity caster;
	protected final ServerWorld world;
	protected final Skill skill;
	protected final int duration;
	protected int age;
	private boolean finished;
	private boolean cancelled;

	/**
	 * @param duration total ticks, or 0 for "until finished manually".
	 */
	protected ActiveCast(ServerPlayerEntity caster, Skill skill, int duration) {
		this.caster = caster;
		this.world = caster.getServerWorld();
		this.skill = skill;
		this.duration = duration;
	}

	public final void tick() {
		if (finished) return;
		if (!caster.isAlive() || caster.isRemoved() || caster.getServerWorld() != world) {
			cancel();
			return;
		}
		onTick();
		age++;
		if (!finished && duration > 0 && age >= duration) {
			finish();
		}
	}

	/** Called every tick while the cast is running. */
	protected abstract void onTick();

	/** Called exactly once when the cast ends, naturally or by cancel. */
	protected void onEnd(boolean cancelled) {
	}

	/** Called when the player releases the skill key (channels only). */
	public void onRelease() {
	}

	public final void finish() {
		if (finished) return;
		finished = true;
		onEnd(cancelled);
	}

	public final void cancel() {
		if (finished) return;
		cancelled = true;
		finish();
	}

	public boolean isChannel() {
		return false;
	}

	public final boolean isFinished() {
		return finished;
	}

	public final int getAge() {
		return age;
	}

	public final int getDuration() {
		return duration;
	}

	public final Identifier getSkillId() {
		return skill.getId();
	}

	public final Skill getSkill() {
		return skill;
	}

	public final ServerPlayerEntity getCaster() {
		return caster;
	}
}
