package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.skill.Element;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Linh căn – the spiritual root a cultivator is born with. Made of one to three {@link Kind}s
 * (a single pure root is the coveted "Thiên Linh Căn", three roots are a muddled "Tam Linh Căn")
 * and a {@link #getGrade() grade} from 1 (Phàm phẩm) to 5 (Thiên phẩm).
 *
 * <p>The root decides which skill {@link Element elements} the cultivator is attuned to (more
 * damage, cheaper qi), how fast qi flows (purer roots regenerate faster) and, together with the
 * {@link Talent}, the overall aptitude that scales power and breakthrough odds.</p>
 */
public final class SpiritRoot {
	public static final int MIN_GRADE = 1;
	public static final int MAX_GRADE = 5;

	/** The five phases plus two rare mutated roots. */
	public enum Kind {
		METAL("metal", 0xE8ECF2, Formatting.WHITE, false, Element.SWORD),
		WOOD("wood", 0x7CDC7C, Formatting.GREEN, false, Element.WIND),
		WATER("water", 0x6FB8FF, Formatting.BLUE, false, Element.ICE),
		FIRE("fire", 0xFF7A3A, Formatting.RED, false, Element.FIRE),
		EARTH("earth", 0xD9B36A, Formatting.YELLOW, false, Element.EARTH),
		THUNDER("thunder", 0xC98BFF, Formatting.LIGHT_PURPLE, true, Element.LIGHTNING),
		DARK("dark", 0x8A3BD6, Formatting.DARK_PURPLE, true, Element.VOID);

		private final String key;
		private final int rgb;
		private final Formatting formatting;
		private final boolean mutation;
		private final Element element;

		Kind(String key, int rgb, Formatting formatting, boolean mutation, Element element) {
			this.key = key;
			this.rgb = rgb;
			this.formatting = formatting;
			this.mutation = mutation;
			this.element = element;
		}

		public String getKey() {
			return key;
		}

		public int getRgb() {
			return rgb;
		}

		public Formatting getFormatting() {
			return formatting;
		}

		/** Mutated roots (Lôi, Ám) are rare variants that attune to lightning / void arts. */
		public boolean isMutation() {
			return mutation;
		}

		/** The skill element this root is attuned to. */
		public Element getElement() {
			return element;
		}

		public Text getName() {
			return Text.translatable("root.celestialarts." + key).formatted(formatting);
		}

		public static Kind byKey(String key) {
			for (Kind k : values()) {
				if (k.key.equals(key)) return k;
			}
			return null;
		}
	}

	private final List<Kind> kinds;
	private final int grade;

	public SpiritRoot(List<Kind> kinds, int grade) {
		if (kinds.isEmpty()) throw new IllegalArgumentException("a spirit root needs at least one kind");
		List<Kind> ordered = new ArrayList<>(new LinkedHashSet<>(kinds));
		this.kinds = Collections.unmodifiableList(ordered.subList(0, Math.min(3, ordered.size())));
		this.grade = Math.max(MIN_GRADE, Math.min(MAX_GRADE, grade));
	}

	public List<Kind> getKinds() {
		return kinds;
	}

	public Kind getPrimary() {
		return kinds.get(0);
	}

	/** 1 = Phàm phẩm … 5 = Thiên phẩm. */
	public int getGrade() {
		return grade;
	}

	public boolean has(Kind kind) {
		return kinds.contains(kind);
	}

	public boolean hasMutation() {
		for (Kind k : kinds) {
			if (k.isMutation()) return true;
		}
		return false;
	}

	/** True when one of the roots is attuned to the element. */
	public boolean favors(Element element) {
		for (Kind k : kinds) {
			if (k.getElement() == element) return true;
		}
		return false;
	}

	/** Skill damage multiplier for a skill of this element: up to +20% for a heaven-grade root. */
	public float damageMultiplier(Element element) {
		return favors(element) ? 1.0F + 0.04F * grade : 1.0F;
	}

	/** Qi cost multiplier for a skill of this element: down to −15% for a heaven-grade root. */
	public float qiCostMultiplier(Element element) {
		return favors(element) ? 1.0F - 0.03F * grade : 1.0F;
	}

	/** Purer roots draw qi faster: single 1.30, double 1.15, triple 1.00. */
	public float purityRegenMultiplier() {
		return 1.0F + 0.15F * (3 - kinds.size());
	}

	/** Colour used for auras and UI – the primary root's colour. */
	public int getRgb() {
		return getPrimary().getRgb();
	}

	/** Root type: Thiên / Song / Tam Linh Căn, prefixed with "Biến Dị" when a mutated root is present. */
	public Text getTypeName() {
		String type = switch (kinds.size()) {
			case 1 -> "single";
			case 2 -> "double";
			default -> "triple";
		};
		MutableText name = Text.translatable("root.celestialarts.type." + type);
		if (hasMutation()) name = Text.translatable("root.celestialarts.type.mutation").append(" ").append(name);
		return name.formatted(getPrimary().getFormatting());
	}

	public Text getGradeName() {
		return Text.translatable("root.celestialarts.grade." + grade).formatted(gradeFormatting(grade));
	}

	public static Formatting gradeFormatting(int grade) {
		return switch (grade) {
			case 1 -> Formatting.GRAY;
			case 2 -> Formatting.WHITE;
			case 3 -> Formatting.GREEN;
			case 4 -> Formatting.AQUA;
			default -> Formatting.GOLD;
		};
	}

	/** "Kim · Hỏa" style list of the root kinds. */
	public Text getKindsText() {
		MutableText t = Text.empty();
		for (int i = 0; i < kinds.size(); i++) {
			if (i > 0) t.append(Text.literal(" · ").formatted(Formatting.GRAY));
			t.append(kinds.get(i).getName());
		}
		return t;
	}

	// ------------------------------------------------------------------ roll

	/**
	 * Rolls a random root. Roughly: 12% single, 38% double, 50% triple roots; 6% carry a mutated
	 * root. Grades: 22% Phàm, 33% Hạ, 28% Trung, 13% Thượng, 4% Thiên – a single root is never
	 * below Trung phẩm (heaven does not hand out a pure root and then waste it).
	 */
	public static SpiritRoot roll(Random random) {
		float c = random.nextFloat();
		int count = c < 0.12F ? 1 : c < 0.50F ? 2 : 3;
		List<Kind> pool = new ArrayList<>(List.of(Kind.METAL, Kind.WOOD, Kind.WATER, Kind.FIRE, Kind.EARTH));
		List<Kind> picked = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			picked.add(pool.remove(random.nextInt(pool.size())));
		}
		if (random.nextFloat() < 0.06F) {
			picked.set(random.nextInt(picked.size()), random.nextBoolean() ? Kind.THUNDER : Kind.DARK);
		}
		float g = random.nextFloat();
		int grade = g < 0.22F ? 1 : g < 0.55F ? 2 : g < 0.83F ? 3 : g < 0.96F ? 4 : 5;
		if (count == 1) grade = Math.max(grade, 3);
		return new SpiritRoot(picked, grade);
	}

	// ------------------------------------------------------------------- nbt

	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList list = new NbtList();
		for (Kind k : kinds) list.add(NbtString.of(k.getKey()));
		nbt.put("Kinds", list);
		nbt.putInt("Grade", grade);
		return nbt;
	}

	public static SpiritRoot readNbt(NbtCompound nbt) {
		NbtList list = nbt.getList("Kinds", NbtElement.STRING_TYPE);
		Set<Kind> kinds = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); i++) {
			Kind k = Kind.byKey(list.getString(i));
			if (k != null) kinds.add(k);
		}
		if (kinds.isEmpty()) return null;
		return new SpiritRoot(new ArrayList<>(kinds), nbt.getInt("Grade"));
	}

	@Override
	public String toString() {
		return kinds + "/" + grade;
	}
}
