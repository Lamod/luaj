package lamo.luaj.util;

import java.util.List;

public class ArrayUtil {

	public interface Serializer {
		String serialize(Object o);
	}

	public interface Mapper<T, U> {
		T map(U u);
	}

	public interface Enumerator<E> {
		boolean enumerate(int idx, E e);
	}

	static public String join(Object[] a, String sep) {
		Serializer ser = o -> o.toString();
		return join(a, ser, sep);
	}

	static public String join(Object[] a, Serializer ser, String sep) {
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
			sb.append(ser.serialize(o));
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

	static public <T> T get(T[] a, int i) {
		if (i < 0) {
			i = a.length + i;
		}
		return (i >= 0 && i < a.length) ? a[i] : null;
	}

	static public <T> T get(List<T> l, int i) {
		if (i < 0) {
			i = l.size() + i;
		}
		return (i >= 0 && i < l.size()) ? l.get(i) : null;
	}

	static public <T> boolean isEmpty(T[] a) {
		return a == null || a.length == 0;
	}

	static public <T> boolean isEmpty(List<T> l) {
		return l == null || l.size() == 0;
	}

	static public <T> int sizeOf(T[] a) {
		if (a == null) {
			return 0;
		} else {
			return a.length;
		}
	}

	static public <T> int sizeOf(List<T> l) {
		if (l == null) {
			return 0;
		} else {
			return l.size();
		}
	}

	static public <T, U> T[] map(U[] us, Mapper<T, U> mapper, T[] ts) {
		if (isEmpty(us) || mapper == null) {
			return null;
		}
		if (isEmpty(ts)) {
			return ts;
		}
		for (int i = 0; i < Math.min(us.length, ts.length); ++i) {
			ts[i] = mapper.map(us[i]);
		}
		return ts;
	}

	static public <E> void enumerate(E[] a, Enumerator<E> enumerator) {
		if (isEmpty(a) || enumerator == null) {
			return;
		}

		for (int i = 0; i < a.length; ++i) {
			if (enumerator.enumerate(i, a[i])) {
				break;
			}
		}
	}

	static public <E> void enumerate(List<E> l, Enumerator<E> enumerator) {
		if (isEmpty(l) || enumerator == null) {
			return;
		}

		for (int i = 0; i < l.size(); ++i) {
			if (enumerator.enumerate(i, l.get(i))) {
				break;
			}
		}
	}

}
