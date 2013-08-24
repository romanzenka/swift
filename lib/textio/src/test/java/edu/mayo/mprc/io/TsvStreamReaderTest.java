package edu.mayo.mprc.io;

import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public final class TsvStreamReaderTest {

	@Test
	public void testReader() throws IOException {
		final TsvStreamReader reader = new TsvStreamReader(TestingUtilities.getTempFileFromResource(getClass(), "/tsvFile.tsv", true, null));
		Assert.assertTrue(reader.hasLine(), "Reader must report lines");
		final String header = reader.nextLine();
		Assert.assertEquals(header, "header1\theader2\theader3\theader4\theader5");
		final int[] columnIndices = new int[]{0, 2, 4, 3};
		final char[] columnTypes = new char[]{'f', 'i', 's', 's'};
		final float[] floats = new float[5];
		final int[] integers = new int[5];
		final String[] strings = new String[5];

		Assert.assertTrue(reader.nextValues(columnIndices, columnTypes, floats, integers, strings));
		Assert.assertEquals(floats[0], 1.0f);
		Assert.assertEquals(integers[0], 3);
		Assert.assertEquals(strings[0], "5");
		Assert.assertEquals(strings[1], "4");

		Assert.assertTrue(reader.nextValues(columnIndices, columnTypes, floats, integers, strings));
		Assert.assertEquals(floats[0], 6.0f);
		Assert.assertEquals(integers[0], 8);
		Assert.assertEquals(strings[0], "10");
		Assert.assertEquals(strings[1], "9");

		Assert.assertTrue(reader.nextValues(columnIndices, columnTypes, floats, integers, strings));
		Assert.assertEquals(floats[0], 11.0f);
		Assert.assertEquals(integers[0], 13);
		Assert.assertEquals(strings[0], "15");
		Assert.assertEquals(strings[1], "14");

		Assert.assertFalse(reader.nextValues(columnIndices, columnTypes, floats, integers, strings));

		reader.close();
	}
}
