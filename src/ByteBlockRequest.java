import java.io.Serializable;

public class ByteBlockRequest implements Serializable{
	
	private int startingIndex;
	private int length;

	public ByteBlockRequest(int startindex, int length) {
		this.startingIndex = startindex;
		this.length = length;
	}

	public int getStartingIndex() {
		return startingIndex;
	}

	public int getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "ByteBlockRequest [startindex=" + startingIndex + ", length=" + length + "]";
	}

}


