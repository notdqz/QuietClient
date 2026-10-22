package com.quiet.module.impl;

import com.mojang.authlib.GameProfile;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Client-only sparring dummy with its own physics, vanilla-style damage (your held weapon's real damage, cooldown,
 * crits, Sharpness, Strength/Weakness, armor), invulnerability frames, knockback (including Knockback Flick),
 * sounds and death/respawn. The server never knows it exists. Attacks are caught in MultiPlayerGameModeMixin.
 */
public class TestDummy extends Module {
	private static TestDummy inst;

	private final ModeSetting movement = mode("Movement", "Stationary", "Stationary", "Walking", "Spinning");
	private final ModeSetting armor = mode("Armor", "None", "None", "Leather", "Iron", "Diamond", "Netherite");
	private final NumberSetting walkSpeed = num("Walk speed", 1.0, 0.3, 1.5, 0.1);

	private RemotePlayer dummy;
	private String armorApplied = "";
	private double ox, oy, oz, ang, stepDist;
	private float yaw, lastRaw, armorPts, toughness, kbResist;
	private int invul, deadFor;
	private Vec3 lastPos = Vec3.ZERO;

	public TestDummy() {
		super("Fake Player", Category.MISC);
		inst = this;
		walkSpeed.visibleWhen(() -> movement.is("Walking"));
	}

	/** MultiPlayerGameModeMixin: returns true if the target was the dummy (the real attack is skipped). */
	public static boolean onAttack(Player attacker, Entity target, float flickOffset) {
		if (inst == null || inst.dummy == null || target != inst.dummy) return false;
		inst.takeHit(Minecraft.getInstance(), flickOffset);
		return true;
	}

	// ------------------------------------------------------------------ combat

	private void sound(Minecraft mc, SoundEvent s, float vol, float pitch) {
		mc.level.playLocalSound(dummy.getX(), dummy.getY(), dummy.getZ(), s, SoundSource.PLAYERS, vol, pitch, false);
	}

	/** Damage of what you're actually holding: base 1 + the item's attack-damage modifiers + effects. */
	private static double weaponDamage(LocalPlayer p) {
		double dmg = p.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		double[] add = {0};
		p.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
			if (attr.is(Attributes.ATTACK_DAMAGE) && mod.operation() == AttributeModifier.Operation.ADD_VALUE)
				add[0] += mod.amount();
		});
		dmg += add[0];
		MobEffectInstance strength = p.getEffect(MobEffects.STRENGTH);
		if (strength != null) dmg += 3.0 * (strength.getAmplifier() + 1);
		MobEffectInstance weakness = p.getEffect(MobEffects.WEAKNESS);
		if (weakness != null) dmg -= 4.0 * (weakness.getAmplifier() + 1);
		return Math.max(dmg, p.getAttributeValue(Attributes.ATTACK_DAMAGE));   // if the attribute already has it, same number
	}

	private void takeHit(Minecraft mc, float flickOffset) {
		LocalPlayer p = mc.player;
		if (dummy.getHealth() <= 0) return;

		float scale = p.getAttackStrengthScale(0.5f);
		int sharp = KnockbackFlick.enchantLevel(p, true);
		float enchant = sharp > 0 ? 0.5f * sharp + 0.5f : 0f;
		float raw = (float) weaponDamage(p) * (0.2f + scale * scale * 0.8f) + enchant * scale;
		boolean strong = scale > 0.9f;
		boolean sprintHit = strong && p.isSprinting();
		boolean crit = strong && p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater()
				&& !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger() && !p.isSprinting();
		if (crit) raw *= 1.5f;
		int kbLevel = KnockbackFlick.enchantLevel(p, false);
		p.resetAttackStrengthTicker();

		// invulnerability frames: during the first 10 ticks only a bigger hit does the difference
		boolean fresh;
		float applyRaw;
		if (invul > 10) {
			if (raw <= lastRaw) { sound(mc, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 1f); return; }
			applyRaw = raw - lastRaw;
			lastRaw = raw;
			fresh = false;
		} else {
			applyRaw = raw;
			lastRaw = raw;
			invul = 20;
			fresh = true;
		}

		float dmg = applyRaw * (1f - Mth.clamp(armorPts - applyRaw / (2f + toughness / 4f), armorPts * 0.2f, 20f) / 25f);

		if (sprintHit || kbLevel > 0) sound(mc, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
		else if (crit) sound(mc, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f);
		else if (strong) sound(mc, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1f);
		else sound(mc, SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1f);
		if (crit) p.crit(dummy);

		if (fresh) {
			dummy.animateHurt(p.getYRot());
			sound(mc, SoundEvents.PLAYER_HURT, 1f, 1f);
			knockback(0.4, p.getX() - dummy.getX(), p.getZ() - dummy.getZ());

			// the extra push goes along the attacker's yaw as the server knows it (bent by Knockback Flick)
			float extra = kbLevel + (sprintHit ? 1f : 0f);
			if (extra > 0) {
				double rad = Math.toRadians(p.getYRot() + (Float.isNaN(flickOffset) ? 0f : flickOffset));
				knockback(extra * 0.5, Math.sin(rad), -Math.cos(rad));
				Vec3 v = p.getDeltaMovement();                // what a real hit like this does to the attacker
				p.setDeltaMovement(v.x * 0.6, v.y, v.z * 0.6);
				p.setSprinting(false);
			}
		}

		float hp = dummy.getHealth() - dmg;
		if (hp <= 0) {
			dummy.setHealth(0f);
			deadFor = 0;
			sound(mc, SoundEvents.PLAYER_DEATH, 1f, 1f);
		} else {
			dummy.setHealth(hp);
		}
	}

	private void knockback(double strength, double x, double z) {
		strength *= 1.0 - kbResist;
		if (strength <= 0) return;
		double len = Math.hypot(x, z);
		if (len < 1.0E-4) { x = Math.random() - 0.5; z = Math.random() - 0.5; len = Math.hypot(x, z); }
		Vec3 v = dummy.getDeltaMovement();
		double vy = dummy.onGround() ? Math.min(0.4, v.y / 2.0 + strength) : v.y;
		dummy.setDeltaMovement(v.x / 2.0 - x / len * strength, vy, v.z / 2.0 - z / len * strength);
	}

	// ------------------------------------------------------------------ spawn / gear

	private void spawn(Minecraft mc) {
		LocalPlayer p = mc.player;
		double rad = Math.toRadians(p.getYRot());
		ox = p.getX() - Math.sin(rad) * 3;
		oy = p.getY();
		oz = p.getZ() + Math.cos(rad) * 3;
		yaw = p.getYRot() + 180f;
		invul = 0; lastRaw = 0; deadFor = 0; stepDist = 0;
		dummy = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Dummy"));
		dummy.setId(Integer.MAX_VALUE - 17);
		dummy.setPos(ox, oy, oz);
		dummy.setYRot(yaw);
		dummy.setYHeadRot(yaw);
		dummy.setYBodyRot(yaw);
		dummy.setHealth(dummy.getMaxHealth());
		mc.level.addEntity(dummy);
		lastPos = dummy.position();
		armorApplied = "";
	}

	private void remove(Minecraft mc) {
		if (dummy != null && mc.level != null) mc.level.removeEntity(dummy.getId(), Entity.RemovalReason.DISCARDED);
		dummy = null;
	}

	private void equip() {
		Item[] set;
		switch (armor.mode()) {
			case "Leather" -> { set = new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS}; armorPts = 7; toughness = 0; kbResist = 0; }
			case "Iron" -> { set = new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}; armorPts = 15; toughness = 0; kbResist = 0; }
			case "Diamond" -> { set = new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}; armorPts = 20; toughness = 8; kbResist = 0; }
			case "Netherite" -> { set = new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}; armorPts = 20; toughness = 12; kbResist = 0.4f; }
			default -> { set = null; armorPts = 0; toughness = 0; kbResist = 0; }
		}
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (int i = 0; i < 4; i++)
			dummy.setItemSlot(slots[i], set == null ? ItemStack.EMPTY : new ItemStack(set[i]));
		armorApplied = armor.mode();
	}

	@Override protected void onDisable() { remove(mc()); }

	// ------------------------------------------------------------------ tick

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (mc.level == null || mc.player == null) return;
		if (dummy != null && (dummy.level() != mc.level || dummy.isRemoved())) dummy = null;
		if (dummy == null) spawn(mc);
		if (!armor.mode().equals(armorApplied)) equip();
		if (invul > 0) invul--;

		boolean dead = dummy.getHealth() <= 0;
		if (dead && ++deadFor > 50) { remove(mc); spawn(mc); return; }

		double ix = 0, iz = 0;
		boolean jump = false;
		if (!dead) {
			switch (movement.mode()) {
				case "Walking" -> {
					ang += 0.05 * walkSpeed.get();
					double dx = ox + Math.cos(ang) * 3 - dummy.getX(), dz = oz + Math.sin(ang) * 3 - dummy.getZ();
					double l = Math.hypot(dx, dz);
					if (l > 0.05) { ix = dx / l * walkSpeed.get(); iz = dz / l * walkSpeed.get(); }
					jump = dummy.horizontalCollision && dummy.onGround();
					Vec3 v = dummy.getDeltaMovement();
					if (Math.hypot(v.x, v.z) > 0.03)
						yaw = Mth.rotLerp(0.3f, yaw, (float) Math.toDegrees(Math.atan2(-v.x, v.z)));
				}
				case "Spinning" -> yaw += 18f;
				default -> {
					LocalPlayer p = mc.player;
					float want = (float) Math.toDegrees(Math.atan2(-(p.getX() - dummy.getX()), p.getZ() - dummy.getZ()));
					yaw = Mth.rotLerp(0.2f, yaw, want);
				}
			}
		}

		// ---- physics (vanilla ground/air numbers)
		Vec3 v = dummy.getDeltaMovement();
		boolean ground = dummy.onGround();
		double slip = ground ? mc.level.getBlockState(dummy.getOnPos()).getBlock().getFriction() * 0.91 : 0.91;
		double accel = ground ? 0.1 * 0.21600002 / (slip * slip * slip) : 0.02;
		dummy.setDeltaMovement(v.x + ix * accel, jump ? 0.42 : v.y, v.z + iz * accel);
		dummy.move(MoverType.SELF, dummy.getDeltaMovement());
		Vec3 a = dummy.getDeltaMovement();
		dummy.setDeltaMovement(a.x * slip, (a.y - 0.08) * 0.98, a.z * slip);

		if (!dead) {
			dummy.setYRot(yaw);
			dummy.setYHeadRot(yaw);
			dummy.setYBodyRot(yaw);
		}

		// ---- footsteps
		Vec3 now = dummy.position();
		if (dummy.onGround()) {
			stepDist += Math.hypot(now.x - lastPos.x, now.z - lastPos.z);
			if (stepDist > 1.6) {
				stepDist = 0;
				BlockState st = mc.level.getBlockState(dummy.getOnPos());
				SoundType sound = st.getSoundType();
				sound(mc, sound.getStepSound(), sound.getVolume() * 0.15f, sound.getPitch());
			}
		}
		lastPos = now;
	}
}
