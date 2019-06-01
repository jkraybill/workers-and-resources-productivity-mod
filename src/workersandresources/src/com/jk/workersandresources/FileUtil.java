package com.jk.workersandresources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class FileUtil {

	public static String readFile(String toRead) throws IOException {
		return readFile(new File(toRead));
	}

	public static String readFile(File toRead) throws IOException {
		return readFile(toRead, Charset.defaultCharset().name());
	}

	public static String readFile(File toRead, String encoding) throws IOException {
		FileInputStream stream = new FileInputStream(toRead);
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			/* Instead of using default, pass in a decoder. */
			String ret = Charset.forName(encoding).decode(bb).toString();
			bb = null;
			fc = null;
			return ret;
		} finally {
			stream.close();
		}
	}

	public static String hash(String toHash) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(toHash.getBytes("UTF-8")); // Change this to "UTF-16" if needed
		byte[] hash = md.digest();
		BigInteger bigInt = new BigInteger(1, hash);
		return bigInt.toString(16);
	}

}
