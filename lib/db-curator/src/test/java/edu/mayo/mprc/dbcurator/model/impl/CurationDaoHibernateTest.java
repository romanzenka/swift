package edu.mayo.mprc.dbcurator.model.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class CurationDaoHibernateTest {

	@Test
	public void shouldExtractUniqueName() {
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DBPath:hello}"), "hello");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DB:this is a test 123}"), "this is a test 123");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("{DBPath:aaaa}"), "{DBPath:aaaa}");
		Assert.assertEquals(CurationDaoHibernate.extractShortName("${DBPath:aaaa_LATEST}"), "aaaa_LATEST");
	}

	@Test
	public void shouldExtractShortName() {
		Assert.assertEquals(CurationDaoHibernate.extractShortname("${DBPath:hello}"), "${DBPath:hello}");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("${DB:this is a test 123_LATEST}"), "this is a test 123");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("database20090102A.fasta"), "database");
		Assert.assertEquals(CurationDaoHibernate.extractShortname("Sprot2219920304C.FASTA"), "Sprot22");
	}
}
