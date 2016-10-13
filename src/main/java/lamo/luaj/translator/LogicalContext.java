package lamo.luaj.translator;

import lamo.luaj.util.ArrayUtil;
import lamo.luaj.vm.Instruction;

import java.util.ArrayList;

class LogicalContext {

	ArrayList<Integer> ts, fs;
	int reg;
	boolean needValue;

	LogicalContext() {
		this.reg = Instruction.NO_REG;
	}

	LogicalContext(int reg) {
		this.reg = reg;
	}

	ArrayList<Integer> get(boolean ts) {
		return ts ? this.ts : this.fs;
	}

	void add(int jmp, boolean ts) {
		if (jmp == Instruction.NO_JUMP) {
			return;
		}
		create(ts).add(jmp);
	}

	void merge(LogicalContext another, boolean ts) {
		ArrayList<Integer> list = another.get(ts);
		if (ArrayUtil.isEmpty(list)) {
			return;
		}
		create(ts).addAll(list);
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
			this.ts = list;
		} else {
			this.fs = list;
		}
		return list;
	}

}
