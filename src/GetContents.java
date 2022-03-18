
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import javax.management.RuntimeErrorException;

/* Classe que trata de ir buscar conteudo do file Contents a outros nos */
public class GetContents extends Thread {

	private StorageNode storageNode;
	private InetAddress nodeIP;
	private int nodePorto;
	private int contagem;

	public GetContents(StorageNode storageNode, InetAddress nodeIP, int nodePorto) {
		this.storageNode = storageNode;
		this.nodeIP = nodeIP;
		this.nodePorto = nodePorto;
		this.contagem = 0;
	}

	@Override
	public void run() {
		try {
			/* Conexao ao node */
			Socket getContentsSocket = new Socket(this.nodeIP, this.nodePorto);
			System.out.println("Socket:" + getContentsSocket);
			ObjectOutputStream outObject = new ObjectOutputStream(getContentsSocket.getOutputStream());
			ObjectInputStream inObject = new ObjectInputStream(getContentsSocket.getInputStream());
			System.out.println("get Contents : criei a socket");
			/**/

			/*
			 * pegar e enviar um pedido e receber os dados vindos do node que com que
			 * estamos conectados
			 */
			ByteBlockRequest pedido = null;
			while (true) {
				try {
					pedido = storageNode.getPedidos().poll();
					if (pedido == null)
						break;
					outObject.writeObject(pedido);
					storageNode.updateFileContentsData(pedido.getStartingIndex(), StorageNode.BLOCK_SIZE,
							(CloudByte[]) inObject.readObject());
				} catch (Exception e) {
					try {
						System.out.println("adicionei o pedido " + pedido.getStartingIndex() + " ha lista");
						storageNode.getPedidos().put(pedido);
					} catch (InterruptedException e1) {
						throw new RuntimeErrorException(null);
					}
					return;
				}
				contagem++;
			}
			System.out.println("Contagem do node " + this.nodeIP + " " + this.nodePorto + " : " + this.contagem);
			/**/
			outObject.close();
			inObject.close();
			getContentsSocket.close();
			System.out.println("Conclui a transferencia de dados");
		} catch (IOException e) {
			System.err.println("dei erro no pedido de transferencia de dados");
			throw new RuntimeErrorException(null);
		}
	}
}