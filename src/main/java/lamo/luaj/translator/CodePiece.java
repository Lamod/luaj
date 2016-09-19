package lamo.luaj.translator;

import lamo.luaj.vm.Instruction;

import java.util.ArrayList;

class CodePiece {

	CodePiece prev, next;

	ArrayList<Instruction> code = new ArrayList<>();

	CodePiece(CodePiece prev) {
		this.prev = prev;
		if (prev != null) {
			prev.next = this;
		}
	}

	void pieceUp() {
		if (this.prev == null) {
			return;
		}

		this.prev.code.addAll(this.code);
		this.code.clear();
	}

}
