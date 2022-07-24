public class Test {

	public static String reverseOnlyLetters(String s) {
		// a-bC-dEf-ghIj
		int left = 0;
		int right = s.length() - 1;
		final char[] chars = s.toCharArray();
		while (left < right) {
			if (!checkChar(chars[left])) {
				left++;
				continue;
			}
			if (!checkChar(chars[right])) {
				right--;
				continue;
			}
			if (checkChar(chars[left]) && checkChar(chars[right])) {
				Character tempLeft = new Character(chars[left]);
				Character tempRight = new Character(chars[right]);
				chars[right] = tempLeft;
				chars[left] = tempRight;
				left++;
				right--;
			}
		}
		return String.valueOf(chars);


	}

	public static boolean checkChar(char tempChar) {
		if ((tempChar >= 'A' && tempChar <= 'Z') || (tempChar >= 'a' && tempChar <= 'z')) {
			return true;
		}
		return false;
	}

}
