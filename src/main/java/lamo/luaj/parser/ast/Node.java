package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil.Serializer;

abstract public class Node {

	final static int INDENT_SIZE = 4;
	final static public Serializer CODE_SERIALIZER = new Serializer() {
		public String serialize(Object o) {
			if (o instanceof Node) {
				return ((Node)o).toCode();
			} else {
				return o.toString();
			}
		}
	};

	abstract public String toCode();

}
