
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.RuntimeErrorException;

/**
 * @brief Class to represent 7-bit bytes. Extra bit is for parity, and is set in
 *        constructor.
 * 
 * @author luismota
 *
 */




//TODO ler ficheiro "justifica��o.pdf" com a justifica��o das altera��es feitas nesta classe





public class CloudByte implements Serializable {

	byte value;
	Semaphore semaphore;

	public CloudByte(byte value) {
		if (value > 127 || value < 0)
			throw new IllegalArgumentException("Invalid value for CloudByte");
		if (countOnes(value) % 2 == 0)
			this.value = value;
		else
			this.value = (byte) -value;
		semaphore = new Semaphore(1);
	}

	public synchronized boolean tryAcquire() {
		return this.semaphore.tryAcquire();
	}

	public synchronized void release() {
		this.semaphore.release();
	}

	 public byte getValue() {
		return (byte) Math.abs(value);
	}

	/**
	 * Test if parity bit has expected value
	 * 
	 * @return parity checks
	 */
	public boolean isParityOk() {
		if (value < 0)
			return countOnes(getValue()) % 2 == 1;
		else
			return countOnes(getValue()) % 2 == 0;
	}

	/**
	 * Only for testing: force parity to be incorrect
	 */
	public void makeByteCorrupt() {
		// general inversion did not work for 0...
		if (value == 0)
			value = 1;// 1 is invalid, should be -1
		else
			value = (byte) -value;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CloudByte other = (CloudByte) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CloudByte [value=" + getValue() + "]" + (!isParityOk() ? "->ERROR" : "");
	}

	private static byte countOnes(byte value) {
		byte count = 0;
		while (value > 0) {
			count += value % 2;
			value /= 2;
		}
		return count;
	}
}