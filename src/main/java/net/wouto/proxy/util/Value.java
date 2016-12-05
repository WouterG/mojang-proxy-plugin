package net.wouto.proxy.util;

public class Value<T> {

	private T original;
	private T replacement;

	public Value(T original) {
		this.original = original;
	}

	public Value(T original, T replacement) {
		this.original = original;
		this.replacement = replacement;
	}

	public T get() {
		if (this.replacement != null) {
			return this.replacement;
		}
		return this.original;
	}

	public T getOriginal() {
		return original;
	}

	public void setOriginal(T original) {
		this.original = original;
	}

	public T getReplacement() {
		return replacement;
	}

	public void setReplacement(T replacement) {
		this.replacement = replacement;
	}
}
