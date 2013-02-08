package edu.mayo.mprc.fasta.filter;

import java.util.Random;

/**
 * A StringManipulator that takes a String and randomizes it so that the returned string a s completely randomized string
 * that is made up of the same characters as the original string just in a random order.
 */
public final class ScrambleStringManipulator implements StringManipulator {
	/**
	 * Random number generator. Settable for reproducibility in tests.
	 */
	private Random random = new Random();

	public Random getRandom() {
		return random;
	}

	public void setRandom(final Random random) {
		this.random = random;
	}

	/**
	 * Creates a Scrambled or Randomizeation of the characters that make up the given toManipulate String.
	 *
	 * @param toManipulate the String you want to create a manipulation of, will obviously be unchanged
	 * @return the manipulated String
	 */
	public String manipulateString(final String toManipulate) {
		final StringBuilder builder = new StringBuilder(toManipulate);

		for (int i = builder.length() - 1; i >= 0; i--) {
			final int randomIndex = getRandomInteger(0, builder.length() - 1);

			final char swapA = builder.charAt(i);
			final char swapB = builder.charAt(randomIndex);

			builder.setCharAt(i, swapB);
			builder.setCharAt(randomIndex, swapA);
		}
		return builder.toString();
	}

	/**
	 * A short (single word?) description of what this manipulator does.  This is needed to insert into the meta data of the
	 * manipulated String to let people know what we are dong
	 *
	 * @return a single String identifying what this does
	 */
	public String getDescription() {
		return "Random";
	}

	/**
	 * gets a random integer between the two specified integers (inclusive)
	 *
	 * @param min the minimum number to allow
	 * @param max the maxiumum number to allow
	 * @return a random integer between the two given integers
	 */
	private int getRandomInteger(final int min, final int max) {
		return (int) Math.round(getRandom().nextDouble() * (max - min)) + min;
	}

}
