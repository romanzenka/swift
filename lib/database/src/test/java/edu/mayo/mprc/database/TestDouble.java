package edu.mayo.mprc.database;

/**
 * A double holder.
 *
 * @author Roman Zenka
 */
public class TestDouble extends PersistableBase {
	private double value1;
	private double value2;

	public TestDouble() {
	}

	public TestDouble(final double value1, final double value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public double getValue1() {
		return value1;
	}

	public void setValue1(final double value1) {
		this.value1 = value1;
	}

	public double getValue2() {
		return value2;
	}

	public void setValue2(final double value2) {
		this.value2 = value2;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof TestDouble)) {
			return false;
		}

		final TestDouble that = (TestDouble) o;

		if (Math.abs(that.value1 - value1) >= 0.01 || Double.isNaN(that.value1) != Double.isNaN(value1)) {
			return false;
		}
		if (Math.abs(that.value2 - value2) >= 0.01 || Double.isNaN(that.value2) != Double.isNaN(value2)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = value1 != +0.0d ? Double.doubleToLongBits(value1) : 0L;
		result = (int) (temp ^ (temp >>> 32));
		temp = value2 != +0.0d ? Double.doubleToLongBits(value2) : 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
