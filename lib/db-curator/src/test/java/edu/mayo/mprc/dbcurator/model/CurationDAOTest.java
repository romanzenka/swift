package edu.mayo.mprc.dbcurator.model;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class CurationDAOTest {

	@Test
	public void shouldExtractUniqueName() {
		Assert.assertEquals(CurationDaoImpl.extractShortName("${DBPath:hello}"), "hello");
		Assert.assertEquals(CurationDaoImpl.extractShortName("${DB:this is a test 123}"), "this is a test 123");
		Assert.assertEquals(CurationDaoImpl.extractShortName("{DBPath:aaaa}"), "{DBPath:aaaa}");
		Assert.assertEquals(CurationDaoImpl.extractShortName("${DBPath:aaaa_LATEST}"), "aaaa_LATEST");
	}

	@Test
	public void shouldExtractShortName() {
		Assert.assertEquals(CurationDaoImpl.extractShortname("${DBPath:hello}"), "${DBPath:hello}");
		Assert.assertEquals(CurationDaoImpl.extractShortname("${DB:this is a test 123_LATEST}"), "this is a test 123");
		Assert.assertEquals(CurationDaoImpl.extractShortname("database20090102A.fasta"), "database");
		Assert.assertEquals(CurationDaoImpl.extractShortname("Sprot2219920304C.FASTA"), "Sprot22");
	}
}
