package info.deskchan.talking_system;

class TextBooleanSet {
	public int offset = 0;
	boolean[] set;
	
	public TextBooleanSet(int length) {
		set = new boolean[length];
		for (int i = 0; i < length; i++) {
			set[i] = true;
		}
	}
	
	public void set(int index, boolean value) {
		if (index - offset < set.length && index >= 0) {
			set[index - offset] = value;
		}
	}
	
	public boolean get(int index) {
		return set[index - offset];
	}
	
	public boolean full() {
		for (int i = 0; i < set.length; i++) {
			if (!set[i]) {
				return false;
			}
		}
		return true;
	}
	
	public void fillFromString(String text) {
		if (text == null || text.length() == 0) {
			for (int i = 0; i < set.length; i++) {
				set[i] = true;
			}
			return;
		}
		if (text.charAt(0) == 'x' || text.charAt(0) == '_') {
			for (int i = 0; i < set.length; i++) {
				set[i] = (i < text.length() && text.charAt(i) == 'x' ? true : false);
			}
			return;
		}
		for (int i = 0; i < set.length; i++) {
			set[i] = false;
		}
		String[] ar = text.split(" ");
		for (String di : ar) {
			if (di.contains("-")) {
				String[] di2 = di.split("-");
				try {
					int n1 = Integer.valueOf(di2[0]) - offset;
					int n2 = Integer.valueOf(di2[di2.length - 1]) - offset;
					for (int i = n1; i != n2; i = (i + 1) % set.length) {
						set[i] = true;
					}
				} catch (Exception e) {
				}
			} else {
				try {
					int n = Integer.valueOf(di);
					set[n - offset] = true;
				} catch (Exception e) {
				}
			}
		}
	}
	
	public String toString() {
		String s = "";
		for (int i = 0; i < set.length; i++) {
			s += (set[i] ? 'x' : '_');
		}
		return s;
	}
}
