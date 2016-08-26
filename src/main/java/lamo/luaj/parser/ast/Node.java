package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil.Serializor;

abstract public class Node {

	final static public Serializor CODE_SERIALIZOR = new Serializor() {
		public String serialize(Object o) {
			if (o instanceof Node) {
				return ((Node)o).toCode();
			} else {
				return o.toString();
			}
		}
	};

	private Block owner;

	public Block getOwner() {
		return this.owner;
	}

	public void setOwner(Block owner) {
		this.owner = owner;
	}

	abstract public String toCode();

	public String getIntend() {
		return this.owner == null ? "" : this.owner.getIntend();
	}

}
