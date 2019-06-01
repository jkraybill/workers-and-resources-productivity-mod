package com.jk.workersandresources;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {

	private static SecureRandom r = new SecureRandom();

	/**
	 * Returns a random number between the min and max (inclusive).
	 */
	public static int get(int min, int max) {
		if (max == 0) {
			return 0;
		} else {
			return (int) r.nextInt(max - min + 1) + min;
		}
	}

	/**
	 * Returns a random number between the min and max (inclusive), using a seed.
	 */
	public static int get(int min, int max, long seed) {
		return get(min, max, new Random(seed * 4096));
	}
	
	/**
	 * Returns a random string from an array.
	 */
	public static String get(String[] src) {
		return src[get(0, src.length - 1)];
	}

	/**
	 * Returns a random number between the min and max (inclusive), using a specified (probably seeded) Random instance.
	 */
	public static int get(int min, int max, Random sr) {
		if (max == 0) {
			return 0;
		} else {
			return (int) sr.nextInt(max - min + 1) + min;
		}
	}

	/**
	 * Returns a random double betweein min (inclusive) and max (exclusive).
	 */
	public static double getDouble(double min, double max) {
		if (min == max)
			return min;
		//System.out.println(min + ":" + max);
		return ThreadLocalRandom.current().nextDouble(min, max);
	}

	/**
	 * Returns a random float betweein min (inclusive) and max (exclusive).
	 */
	public static float getFloat(float min, float max) {
		return new Random().nextFloat() * (max - min) + min;
	}

	/**
	 * Returns a normally-distributed number, where stdevUnit is the number of units above/below the mean that are 1 sdev.
	 */
	public static double gaussian(double mean, double stdevUnit) {
		return mean + (r.nextGaussian() * stdevUnit);
	}

}
