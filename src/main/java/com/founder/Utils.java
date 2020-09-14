package com.founder;

public class Utils {
	public static int findIndex(String arr[], String t) {
		// if array is Null
		if (arr == null) {
			return -1;
		}

		// find length of array
		int len = arr.length;
		int i = 0;

		// traverse in the array
		while (i < len) {

			// if the i-th element is t
			// then return the index
			if (arr[i].equals(t)) {
				return i;
			} else {
				i = i + 1;
			}
		}
		return -1;
	}

	public static boolean isGoodStringArray(String[] arr) {
		if (arr == null)
			return false;
		for (String string : arr) {
			if (string == null)
				return false;
		}
		return true;
	}

	static String utf2hex(String utf) {
		StringBuffer sb = new StringBuffer();
		byte[] bytes = utf.getBytes();
		for (int i = 0; i < bytes.length; i++) {
			String hexString = Integer.toHexString(bytes[i] & 0x0FF);
			sb.append(hexString);
		}
		String result = sb.toString();
		return result;
	}

	static String hex2utf(String hex) {
		char[] charArray = hex.toCharArray();
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < charArray.length; i = i + 2) {
			String st = "" + charArray[i] + "" + charArray[i + 1];
			byte ch = (byte) Integer.parseInt(st, 16);
			bytes[i / 2] = ch;
		}
		String result = new String(bytes);
		return result;
	}
}
