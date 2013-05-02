package edu.mayo.mprc.unimod;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class SpecificityTest {

	@Test
	public void shouldConvertToMascot() {
		final ModBuilder builder = new ModBuilder();
		builder.setTitle("Phospho");
		builder.addSpecificityFromUnimod("S", "Anywhere", false, "...", 2);
		builder.addSpecificityFromUnimod("T", "Anywhere", false, "...", 2);
		final Mod mod = builder.build();

		Assert.assertEquals(mod.getModSpecificities().iterator().next().toMascotString(), "Phospho (ST)");
	}

	@Test
	public void shouldConvertCTerm() {
		Assert.assertEquals(makeSpecificity("Phospho", "", Terminus.Cterm, true, true).toMascotString(), "Phospho (Protein C-term)");
	}

	@Test
	public void shouldConvertAnyCTerm() {
		Assert.assertEquals(makeSpecificity("Phospho", "", Terminus.Cterm, false, true).toMascotString(), "Phospho (C-term)");
	}

	@Test
	public void shouldConvertAAAnyCTerm() {
		Assert.assertEquals(makeSpecificity("Phospho", "A", Terminus.Cterm, false, true).toMascotString(), "Phospho (C-term A)");
	}

	@Test
	public void shouldConvertAAProteinNTerm() {
		Assert.assertEquals(makeSpecificity("Phospho", "A", Terminus.Nterm, true, true).toMascotString(), "Phospho (Protein N-term A)");
	}

	@Test
	public void shouldConvertAANoGroup() {
		Assert.assertEquals(makeSpecificity("Phospho", "C", Terminus.Anywhere, true, false).toMascotString(), "Phospho (C)");
	}

	@Test
	public void shouldConvertSpecialChars() {
		Assert.assertEquals(makeSpecificity("Phospho<&>", "C", Terminus.Anywhere, true, false).toMascotString(), "Phospho&lt;&amp;&gt; (C)");
	}

	private ModSpecificity makeSpecificity(final String title, final String site, final Terminus terminus, final boolean proteinOnly, final boolean setGroup) {
		final ModBuilder builder = new ModBuilder();
		builder.setTitle(title);
		builder.addSpecificity(title, site, terminus, proteinOnly, setGroup ? 1 : null);
		final Mod mod = builder.build();
		return mod.getModSpecificities().iterator().next();
	}
}
