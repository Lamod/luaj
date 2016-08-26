package lamo.luaj.util;

public class ArrayUtil {

	public interface Serializor {
		String serialize(Object o);
	}

	static public String join(Object[] a, String sep) {
		Serializor sor = new Serializor() {
			public String serialize(Object o) {
				return o.toString();
			}
		};
		return join(a, sor, sep);
	}

	static public String join(Object[] a, Serializor sor, String sep) {
		if (a == null || a.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object o : a) {
			if (first) {
				first = false;
			} else {
				sb.append(sep);
			}
			sb.append(sor.serialize(o));
		}

		return sb.toString();
	}

	static public <T> boolean contains(T[] a, T e) {
		for (T t : a) {
			if (t == e || t.equals(e)) {
				return true;
			}
		}

		return false;
	}

}
