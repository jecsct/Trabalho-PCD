import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.RuntimeErrorException;

public class StorageNode {

	private String filePath;
	private CloudByte[] fileContents;

	private boolean inscrito;
	private boolean completo;

	private BlockingQueue<ByteBlockRequest> pedidos;

	private InetAddress ipDiretorio;
	private int portoDiretorio;
	private int portoProprio;

	Socket directorySocket;
	private BufferedReader inText;
	private PrintWriter outText;

	public static final int FILE_SIZE = 1000000;
	public static final int BLOCK_SIZE = 100;

	/* Node Constructors */
	public StorageNode(String ipDiretorio, String portoDiretorio, String portoProprio, String filePath)
			throws FileNotFoundException {
		if (ipDiretorio == null || portoDiretorio == null || portoProprio == null || filePath == null) {
			System.err.println("Erro na criacao do no");
			throw new IllegalArgumentException();
		} else {
			if (!new File(filePath).exists()) {
				System.err.println("Erro na criacao completa do no! Ficheiro especificado nao existe");
				throw new FileNotFoundException();
			} else {
				try {
					this.filePath = filePath;
					this.fileContents = new CloudByte[1000000];
					this.ipDiretorio = InetAddress.getByName(ipDiretorio);
					this.portoDiretorio = Integer.parseInt(portoDiretorio);
					this.portoProprio = Integer.parseInt(portoProprio);
					this.inscrito = false;
				} catch (IllegalArgumentException | UnknownHostException e) {
					System.err.println(
							"Erro com o ip ou porto fornecidos! Verificar se estam bem escritos(ip -> ipv4 ; porto -> int)");
					throw new IllegalArgumentException();
				}
				try {
					readBytes();
					System.out.println("Li o conteúdo de um ficheiro local com sucesso");
					this.completo = true;
				} catch (IOException e) {
					System.err.println("O conteudo do ficheiro dado nao e valido");
					this.completo = false;
				}
			}
		}
	}

	private void readBytes() throws IOException {
		byte[] fileContentsTemp = Files.readAllBytes(new File(filePath).toPath());
		for (int i = 0; i < fileContentsTemp.length; i++) {
			fileContents[i] = new CloudByte(fileContentsTemp[i]);
		}
	}

	public StorageNode(String ipDiretorio, String portoDiretorio, String portoProprio) {
		fileContents = new CloudByte[1000000];
		if (ipDiretorio == null || portoDiretorio == null || portoProprio == null) {
			System.err.println("Erro na criacao incompleta do no");
		} else {
			try {
				this.ipDiretorio = InetAddress.getByName(ipDiretorio);
				this.portoDiretorio = Integer.parseInt(portoDiretorio);
				this.portoProprio = Integer.parseInt(portoProprio);
			} catch (Exception e) {
				System.err.println("Os argumentos dados nao sao validos");
				throw new IllegalArgumentException();
			}
		}
	}

	public void runNo() {
		connectToDirectory();
		if (!completo) {
			try {
				getContents();
			} catch (IOException | InterruptedException e) {
				System.err.println("erro no getContents");
				e.printStackTrace();
			}
		}
		try {
			inscrever();
			new Listener(this).start();
			new Inspector(this, 0).start();
			new Inspector(this, FILE_SIZE / 2).start();
			startServing();
		} catch (IOException e) {
			System.err.println("Erro no startServing");
			e.printStackTrace();
		}
	}

	/*
	 * vai buscar aos outros nos o conteudo necessario para preencher o fileContents
	 */
	public void getContents() throws IOException, InterruptedException {

		this.pedidos = producePedidos();
		if (this.pedidos == null) {
			System.err.println("Algo correu mal na criacao da lista de pedidos");
			throw new IllegalArgumentException();
		}

		ArrayList<String> nodeList = getNodeList(this.inText, this.outText);
		if (nodeList == null) {
			System.err.println("Algo correu mal no getting as portas dos nodes");
			return;
		}
		System.out.println("Nós de onde vou descarregar o conteúdo : " + nodeList);

		ArrayList<Thread> threads = new ArrayList<Thread>();
		Thread t;
		String[] currentContent;
		for (String i : nodeList) {
			currentContent = i.split(" ");
			t = new GetContents(this, InetAddress.getByName(currentContent[0]), Integer.parseInt(currentContent[1]));
			threads.add(t);
			t.start();
		}

		for (Thread thread : threads)
			thread.join();
	}

	/*
	 * Devolve um ArrayList de todos os pedidos necessarios para obter os dados do
	 * ficheiro
	 */
	private BlockingQueue<ByteBlockRequest> producePedidos() {
		BlockingQueue<ByteBlockRequest> listaPedidos = new ArrayBlockingQueue<ByteBlockRequest>(FILE_SIZE / BLOCK_SIZE);
		for (int i = 0; i < (FILE_SIZE / BLOCK_SIZE); i++) {
			listaPedidos.add(new ByteBlockRequest(i * BLOCK_SIZE, BLOCK_SIZE));
		}
		return listaPedidos;
	}

	/* Server Set Up */
	private void startServing() throws IOException {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(this.portoProprio);
			System.out.println("aguardando ligacoes");

			while (true) {
				Socket socket = ss.accept();
				new DealWithConnections(this, socket).start();
			}
		} catch (Exception e) {
			throw new RuntimeErrorException(null);
		} finally {
			ss.close();
		}
	}

	/*
	 * Comunicacao com o diretorio
	 */

	/* Connects to diretorio */
	private void connectToDirectory() {
		try {
			directorySocket = new Socket(this.ipDiretorio, this.portoDiretorio);
			inText = new BufferedReader(new InputStreamReader(directorySocket.getInputStream()));
			outText = new PrintWriter(new BufferedWriter(new OutputStreamWriter(directorySocket.getOutputStream())),
					true);
		} catch (IOException e) {
			System.err.println("Erro na ligacao ao diretorio");
			e.printStackTrace();
		}
	}

	/* inscreve o Node no directory */
	private void inscrever() {
		try {
			outText.println("INSC " + InetAddress.getLocalHost().getHostAddress() + " " + this.portoProprio);
			this.inscrito = true;
		} catch (UnknownHostException e) {
			System.err.println("Deu erro no envio da mensagem de inscricao");
			e.printStackTrace();
			throw new RuntimeErrorException(null);
		}
	}

	/* Devolve todos os nodes do diretorio */
	public ArrayList<String> getNodeList(BufferedReader in, PrintWriter out) {
		ArrayList<String> nodeList = new ArrayList<String>();
		try {
			out.println("nodes");

			String msg = in.readLine();
			while (!msg.contentEquals("END")) {
				nodeList = addNodeToList(nodeList, msg);
				msg = in.readLine();
			}
			if (inscrito)
				return cleanNodeList(nodeList);
			return nodeList;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Receive Nodes : fechei porque ocorreu um erro");
			throw new RuntimeErrorException(null);
		}
	}

	private ArrayList<String> cleanNodeList(ArrayList<String> nodeList) throws UnknownHostException {
		nodeList.remove(InetAddress.getLocalHost().getHostAddress() + " " + portoProprio);
		return nodeList;
	}

	/* Adiciona um no a lista exceto se for o nosso */
	public static ArrayList<String> addNodeToList(ArrayList<String> nodeList, String msg) {
		String[] contents = msg.split(" ");
		nodeList.add(contents[1] + " " + contents[2]);
		return nodeList;
	}

	/* injeta erro na posicao especificada */
	public void injectError(int byteAlvo) {
		fileContents[byteAlvo - 1].makeByteCorrupt();
		System.out.println(
				"Erro injetado no byte " + (byteAlvo - 1) + ". Novo valor do byte: " + getFilePosition(byteAlvo - 1));

	}

	/* Recebe os dados vindos do node e escreve os logo no fileContents */
	public void updateFileContentsData(int position, int size, CloudByte[] content) {
		for (int i = 0; (i + position) < position + size; i++)
			fileContents[i + position] = content[i];
	}

	/* devolve o cloudByte na posicao do fileContents dada como argumento */
	public CloudByte getFilePosition(int position) {
		return fileContents[position];
	}

	public BlockingQueue<ByteBlockRequest> getPedidos() {
		return pedidos;
	}

	// -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public class CorrectorThread extends Thread {

		private int byteBlockPosition;
		private CountDownLatch countDownLatch;
		private BlockingQueue<CloudByte> potencialCorrections;
		CloudByte cb;

		public CorrectorThread(int byteBlockPosition, CloudByte cb) {
			this.byteBlockPosition = byteBlockPosition;
			this.cb = cb;
		}

		@Override
		public void run() {
			System.out.println("E necessario corrigir o byte " + this.byteBlockPosition + ", o seu valor é  "
					+ fileContents[this.byteBlockPosition]);
			try {
				Socket directorySocket = new Socket(ipDiretorio, portoDiretorio);
				BufferedReader in = new BufferedReader(new InputStreamReader(directorySocket.getInputStream()));
				PrintWriter out = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(directorySocket.getOutputStream())), true);

				ArrayList<String> nodeList = getNodeList(in, out);

				out.close();
				in.close();
				directorySocket.close();

				this.potencialCorrections = new ArrayBlockingQueue<CloudByte>(nodeList.size());
				System.out.println("Estes são os nodes que vão ser usados para corrigir o conteúdo: " + nodeList);

				this.countDownLatch = new CountDownLatch(2);
				String[] currentContent;
				for (String i : nodeList) {
					currentContent = i.split(" ");
					new getByteBlockThread(this.byteBlockPosition, InetAddress.getByName(currentContent[0]),
							Integer.parseInt(currentContent[1])).start();
				}

				this.countDownLatch.await();
				CloudByte answer1 = this.potencialCorrections.poll();
				CloudByte answer2 = this.potencialCorrections.poll();
				CloudByte[] cbv = new CloudByte[1];

				if (answer1.isParityOk() && answer2.isParityOk()) {
					cbv[0] = answer1;
					updateFileContentsData(this.byteBlockPosition, 1, cbv);
					System.out.println(
							"O byte foi corrigido com sucesso.\nNovo valor é " + fileContents[this.byteBlockPosition]);
				}

				cb.release();
			} catch (Exception e) {
				System.err.println("Erro algures no getByteBlock");
				e.printStackTrace();
			}
		}

		private class getByteBlockThread extends Thread {
			private InetAddress nodeIP;
			private int nodePorto;
			private int byteBlockPosition;

			public getByteBlockThread(int byteBlockPosition, InetAddress nodeIP, int nodePorto) {
				this.byteBlockPosition = byteBlockPosition;
				this.nodeIP = nodeIP;
				this.nodePorto = nodePorto;
			}

			@Override
			public void run() {
				try {
					/* Conexao ao node */
					Socket getContentsSocket = new Socket(this.nodeIP, this.nodePorto);
					ObjectOutputStream outObject = new ObjectOutputStream(getContentsSocket.getOutputStream());
					ObjectInputStream inObject = new ObjectInputStream(getContentsSocket.getInputStream());

					outObject.writeObject(new ByteBlockRequest(byteBlockPosition, 1));

					CloudByte[] newByte = (CloudByte[]) inObject.readObject();
					potencialCorrections.add(newByte[0]);
					countDownLatch.countDown();

					outObject.close();
					inObject.close();
					getContentsSocket.close();

				} catch (Exception e) {
					System.err.println("Erro no getByteBlockThread");
					e.printStackTrace();
				}
			}
		}
	}

	public CloudByte[] getFileContents() {
		return fileContents;
	}

	public void setFileContents(CloudByte[] fileContents) {
		this.fileContents = fileContents;
	}

	public int getPortoProprio() {
		return portoProprio;
	}

	public static void main(String[] args) {
		if (args.length == 4) {
			try {
				new StorageNode(args[0], args[1], args[2], args[3]).runNo();
			} catch (FileNotFoundException e) {
				return;
			}
		} else {
			new StorageNode(args[0], args[1], args[2]).runNo();
		}

	}

}
