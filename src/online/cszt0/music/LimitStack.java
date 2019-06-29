package online.cszt0.music;

/**
 * @author 初始状态0
 * @date 2019/6/4 0:14
 */
@SuppressWarnings("WeakerAccess")
public class LimitStack<T> {
	private Object[] stack;
	private int pointer;

	public LimitStack() {
		stack = new Object[10];
	}

	public void push(T obj) {
		resetPointIfNeed();
		stack[pointer++] = obj;
	}

	@SuppressWarnings("unchecked")
	public T pop() {
		int p = pointer - 1;
		if (p < 0) {
			p = stack.length - 1;
		}
		return (T) stack[p];
	}

	@SuppressWarnings("unchecked")
	public T peek() {
		int p = pointer - 1;
		if (p < 0) {
			p = stack.length - 1;
		}
		pointer = p;
		return (T) stack[p];
	}

	private void resetPointIfNeed() {
		if (pointer >= stack.length) {
			pointer = 0;
		}
	}
}
