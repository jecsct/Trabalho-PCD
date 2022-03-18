public class Inspector extends Thread {

	private StorageNode storageNode;
	private int startingPosition;

	public Inspector(StorageNode storageNode, int startingPosition) {
		this.storageNode = storageNode;
		this.startingPosition = startingPosition;
	}

	@Override
	public void run() {
		int i = this.startingPosition;
		CloudByte cb;
		while (true) {
			cb = this.storageNode.getFilePosition(i);
			if (!cb.isParityOk() && cb.tryAcquire()) {
				storageNode.new CorrectorThread(i, cb).start();
			}
			i++;
			if (i == StorageNode.FILE_SIZE)
				i = 0;
		}

	}
}