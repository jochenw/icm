package com.github.jochenw.icm.core.api;

import java.util.Comparator;

public class DefaultIcmChangeNumberHandler implements IcmChangeNumberHandler<IcmChangeNumber> {
	@Override
	public IcmChangeNumber asVersion(String pVersion) {
		return IcmChangeNumber.valueOf(pVersion);
	}

	private static final Comparator<IcmChangeNumber> COMPARATOR = new Comparator<IcmChangeNumber>() {
		@Override
		public int compare(IcmChangeNumber pV1, IcmChangeNumber pV2) {
			final int[] num1 = pV1.getNumbers();
			final int[] num2 = pV2.getNumbers();
			final int length = Math.min(num1.length, num2.length);
			for (int i = 0;  i < length;  i++) {
				final int n = num1[i] - num2[i];
				if (n != 0) {
					return Integer.signum(n);
				}
			}
			final int maxLength = Math.max(num1.length, num2.length);
			for (int i = length;  i < maxLength;  i++) {
				int n1 = num1.length > length ? num1[length] : 0;
				int n2 = num2.length > length ? num2[length] : 0;
				final int n = n1 - n2;
				if (n != 0) {
					return Integer.signum(n);
				}
			}
			return 0;
		}
	};
	
	@Override
	public Comparator<IcmChangeNumber> getComparator() {
		return COMPARATOR;
	}

	@Override
	public String asString(IcmChangeNumber pVersion) {
		return pVersion.toString();
	}
}
