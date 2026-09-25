package com.ngoducduy.celestialarts.mixin;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.RealmPassives;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.skill.cast.ShieldCast;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two small hooks:
 * <ul>
 *   <li>Frozen entities cannot jump.</li>
 *   <li>Skill damage dealt by a player scales with their realm; damage taken by a player is
 *       first absorbed by an active Huyền Vũ shield, and frozen targets take extra damage.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "jump", at = @At("HEAD"), cancellable = true)
	private void celestialarts$noJumpWhileFrozen(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.hasStatusEffect(ModEffects.FROZEN)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
	private float celestialarts$modifyDamage(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.getWorld().isClient) return amount;

		// Skill damage grows with the caster's realm.
		if (amount > 0 && source.getAttacker() instanceof ServerPlayerEntity attacker && attacker != self
				&& ModDamageTypes.isSkillDamage(source)) {
			amount *= RealmPassives.skillDamageMultiplier(QiHolder.get(attacker).getRealm());
		}

		if (self instanceof ServerPlayerEntity player && celestialarts$wouldVanillaAccept(self, source, amount)) {
			PlayerQi qi = QiHolder.get(player);
			ShieldCast shield = ShieldCast.find(qi);
			if (shield != null) {
				amount = shield.absorb(source, amount);
			}
		}

		if (amount > 0 && self.hasStatusEffect(ModEffects.FROZEN) && !source.isOf(net.minecraft.entity.damage.DamageTypes.FREEZE)) {
			amount *= 1.25f;
		}
		return amount;
	}

	@Shadow
	protected float lastDamageTaken;

	/**
	 * Mirrors the early-outs of {@code LivingEntity.damage} so the shield is not drained (nor its hit
	 * sound/particles played) by calls vanilla would ignore anyway: invulnerability, dead entities,
	 * fire resistance and the 10-tick damage cooldown (e.g. every burning tick).
	 */
	@Unique
	private boolean celestialarts$wouldVanillaAccept(LivingEntity self, DamageSource source, float amount) {
		if (self.isInvulnerableTo(source) || self.isDead()) return false;
		if (source.isIn(DamageTypeTags.IS_FIRE) && self.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) return false;
		if (self.timeUntilRegen > 10.0F && !source.isIn(DamageTypeTags.BYPASSES_COOLDOWN) && amount <= this.lastDamageTaken) return false;
		return true;
	}
}
