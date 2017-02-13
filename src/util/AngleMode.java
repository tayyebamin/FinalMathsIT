package util;

public enum AngleMode {
	DEGREE(1), RADIAN(2), GRADIAN(3);
	private int value;
	private AngleMode(int Value) {
		this.value = Value;
	}
};