package com.github.jochenw.icm.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IcmChangeNumber {
	private final String numberString;
	private final int[] numbers;

	private IcmChangeNumber(String pVersionString, int[] pNumbers) {
		numberString = pVersionString;
		numbers = pNumbers;
	}
	
	public String toString() {
		return numberString;
	}

	public int[] getNumbers() {
		return numbers;
	}

	public static IcmChangeNumber valueOf(String pNumberString) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(pNumberString, "Change Number");
		final int[] numbers = parse(pNumberString);
		if (numbers == null) {
			throw new IllegalArgumentException("Invalid change number string: " + pNumberString);
		}
		return new IcmChangeNumber(pNumberString, numbers);
	}

	private static int[] parse(String pNumberString) {
		final List<Integer> numbers = new ArrayList<>();
		final String ver = pNumberString.trim();
		final StringBuilder sb = new StringBuilder();
		for	 (int i = 0;  i < ver.length();  i++) {
			final char c = ver.charAt(i);
			if (Character.isDigit(c)) {
				sb.append(c);
			} else if (c == '.') {
				if (sb.length() == 0) {
					return null;
				} else {
					try {
						numbers.add(Integer.valueOf(sb.toString()));
						sb.setLength(0);
					} catch (NumberFormatException e) {
						return null;
					}
				}
			} else {
				return null;
			}
		}
		if (sb.length() == 0) {
			return null;
		} else {
			try {
				numbers.add(Integer.valueOf(sb.toString()));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (numbers.isEmpty()) {
			return null;
		}
		final int[] nums = new int[numbers.size()];
		for (int i = 0;  i < nums.length;  i++) {
			nums[i] = numbers.get(i).intValue();
		}
		return nums;
	}
}
