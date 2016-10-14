package lamo.luaj.translator;

import lamo.luaj.util.ArrayUtil;
import lamo.luaj.vm.Instruction;

import java.util.ArrayList;

class LogicalContext {

	ArrayList<Integer> tlist, flist;
	int reg;
	boolean needValue;

	LogicalContext() {
		this.reg = Instruction.NO_REG;
	}

	LogicalContext(int reg) {
		this.reg = reg;
	}

	ArrayList<Integer> get(boolean ts) {
		return ts ? this.tlist : this.flist;
	}

	void add(int jmp, boolean t) {
		if (jmp == Instruction.NO_JUMP) {
			return;
		}
		create(t).add(jmp);
	}

	void merge(LogicalContext another, boolean t) {
		ArrayList<Integer> list = another.get(t);
		if (ArrayUtil.isEmpty(list)) {
			return;
		}
		create(t).addAll(list);
	}

	void merge(LogicalContext another) {
		merge(another, true);
		merge(another, false);

		this.needValue = this.needValue || another.needValue;
	}

	private ArrayList<Integer> create(boolean ts) {
		ArrayList<Integer> list = get(ts);
		if (list != null) {
			return list;
		}

		list = new ArrayList<>();
		if (ts) {
			this.tlist = list;
		} else {
			this.flist = list;
		}
		return list;
	}

}
