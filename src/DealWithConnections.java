
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;


/* Trata das conexoes que forem feitas ao node */
public class DealWithConnections extends Thread {

	private StorageNode storageNode;
	private Socket nodesSocket;
	private ObjectInputStream inObject;
	private ObjectOutputStream outObject;

	public DealWithConnections(StorageNode storageNode, Socket socket) throws IOException {
		this.storageNode = storageNode;
		this.nodesSocket = socket;
		this.inObject = new ObjectInputStream(socket.getInputStream());
		this.outObject = new ObjectOutputStream(socket.getOutputStream());
		System.out.println("Recebi uma conexao");
	}

	@Override
	public void run() {
		while (true) {
			try {
				Object t = inObject.readObject();
				if (t instanceof ByteBlockRequest) {
					ByteBlockRequest byteBlock = (ByteBlockRequest) t;
					fileContentsCheck(byteBlock);
					sendContent(byteBlock);
				} else {
					System.err.println("ObjectStreamChannel recebeu algo que nao era ByteBlockRequest");
					System.err.println(t.toString());
				}
			} catch (ClassNotFoundException | IOException e) {
				System.err.println("Algo correu mal no DealWitNodes");
				System.err.println("provavelemente o client/node desconectou se");
				break;
			}
		}
		try {
			inObject.close();
			outObject.close();
			nodesSocket.close();
		} catch (IOException e) {
			System.err.println("Algo correu mal a fechar os canais depois da ligacao terminar");
		}
	}

	private void fileContentsCheck(ByteBlockRequest byteBlock) {
		ArrayList<Thread> threads = new ArrayList<Thread>();
		Thread t;
		CloudByte cb;
		for (int i = byteBlock.getStartingIndex(); i < byteBlock.getStartingIndex() + byteBlock.getLength(); i++) {
			cb = storageNode.getFilePosition(i);
			if (!cb.isParityOk() && cb.tryAcquire()) {
				t = storageNode.new CorrectorThread(i,cb);
				threads.add(t);
				t.start();
			}
		}
		for (Thread thread : threads)
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.err.println("problema no join do fileContentsCheck");
			}
	}

	private void sendContent(ByteBlockRequest byteBlock) throws IOException {
		CloudByte[] mail = new CloudByte[byteBlock.getLength()];
		for (int j = 0; (j+byteBlock.getStartingIndex()) < byteBlock.getStartingIndex()
				+ byteBlock.getLength(); j++) {
			mail[j] = storageNode.getFileContents()[j+byteBlock.getStartingIndex()];
		}
		outObject.writeObject(mail);
	}
}