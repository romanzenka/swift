package edu.mayo.mprc.swift.search.task;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class TestDistinctFiles {

	@Test
	public void shouldMakeDistinct() {
		final DistinctFiles d = new DistinctFiles();
		check(d, "test.txt", "test.txt");
		check(d, "hello.txt", "hello.txt");
		check(d, "test.txt", "test_2.txt");
		check(d, "test_2.txt", "test_2_2.txt");
		check(d, "hello.txt", "hello_2.txt");
		check(d, "dir/hello.txt", "dir/hello.txt");
		check(d, "dir/hello.tar.gz", "dir/hello.tar.gz");
		check(d, "dir/hello.tar.gz", "dir/hello_2.tar.gz");
	}

	/**
	 * A particular path to a file can have "meaning". If we request a file of the same name
	 * with the same meaning, we should get the same file.
	 * <p/>
	 * A meaning is simply an object that is checked for equality. If the meaning equals previous meaning,
	 * same file can be provided.
	 * <p/>
	 * A meaning of 'null' is always different.
	 */
	@Test
	public void shouldSupportFileMeanings() {
		final DistinctFiles d = new DistinctFiles();
		check(d, "test.txt", 1, "test.txt");
		check(d, "hello.txt", null, "hello.txt");
		check(d, "test.txt", 1, "test.txt");
		check(d, "test_2.txt", null, "test_2.txt");
		check(d, "hello.txt", null, "hello_2.txt");
		check(d, "dir/hello.txt", null, "dir/hello.txt");
		check(d, "dir/hello.tar.gz", 1, "dir/hello.tar.gz");
		check(d, "dir/hello.tar.gz", 1, "dir/hello.tar.gz");
		check(d, "dir/hello.tar.gz", "hi", "dir/hello_2.tar.gz");
		check(d, "test.txt", 2, "test_2_2.txt");
	}

	private void check(final DistinctFiles distinctFiles, final String file, final String expected) {
		Assert.assertEquals(distinctFiles.getDistinctFile(new File(file)), new File(expected).getAbsoluteFile());
	}

	private void check(final DistinctFiles distinctFiles, final String file, final Object meaning, final String expected) {
		Assert.assertEquals(distinctFiles.getDistinctFile((File) new File(file), meaning), new File(expected).getAbsoluteFile());
	}

}
