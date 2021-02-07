package net.patchworkmc.patcher.transformer;

import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

final class NewPushFinder extends Analyzer<SourceValue> {
	private final MethodInsnNode target;

	NewPushFinder(MethodInsnNode target) {
		super(new SourceInterpreter());
		this.target = target;
	}

	private class InternalFrame extends Frame<SourceValue> {
		private AbstractInsnNode current;
		boolean capture;
		int depth;

		InternalFrame(int numLocals, int numStack) {
			super(numLocals, numStack);
		}

		@Override
		public void execute(AbstractInsnNode insn, Interpreter<SourceValue> interpreter) throws AnalyzerException {
			this.current = insn;
			super.execute(insn, interpreter);
		}

		@Override
		public void push(SourceValue value) {
			depth++;
			super.push(value);
		}

		@Override
		public SourceValue pop() {
			depth--;

			if (depth == 0 && current == target) {
				SourceValue it = super.pop();
				throw new FoundException(it.insns);
			}

			return super.pop();
		}
	}

	@Override
	protected Frame<SourceValue> newFrame(int numLocals, int numStack) {
		return new InternalFrame(numLocals, numStack);
	}

	public class FoundException extends RuntimeException {
		public final Set<AbstractInsnNode> insns;

		FoundException(Set<AbstractInsnNode> insns) {
			this.insns = insns;
		}
	}
}