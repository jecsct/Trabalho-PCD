import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.management.RuntimeErrorException;

public class Diretorio {

	public int PORTO;
	private ArrayList<NoInf> storageNodeList;

	public Diretorio(String porto) {
		storageNodeList = new ArrayList<NoInf>();
		try {
			PORTO = Integer.parseInt(porto);
		} catch (Exception e) {
			System.err.println("o argumento fornecido nao era um inteiro");
			return;
		}
	}

	public void serve() throws IOException {
		ServerSocket ss = new ServerSocket(PORTO);
		try {
			System.out.println("Servidor Lancado\nAguardando ligações.");
			while (true) {
				Socket socket = ss.accept();
				new DealWithNode(socket).start();
			}
		} finally {
			ss.close();
		}
	}

	public synchronized void removeStorageNode(String ip, String porto) {
		for (NoInf ni : storageNodeList)
			if (ni.getIp().contentEquals(ip) && ni.getPorto().contentEquals(porto)) {
				storageNodeList.remove(ni);
				System.out.println("StorageNodeList -> " + storageNodeList);
				return;
			}

	}

	public boolean checkIfNodeIsRegistered(String endereco, String porto) {
		for (NoInf ni : storageNodeList)
			if (ni.getIp().equals(endereco) && ni.getPorto().contentEquals(porto))
				return true;
		return false;
	}

	public synchronized void inscrever(String endereco, String porto, DealWithNode dealWithNode) {
		if (endereco == null || porto == null || dealWithNode == null) {
			System.err.println("Recebi endereco ou porto nulos");
			throw new IllegalArgumentException();
		}
		if (checkIfNodeIsRegistered(endereco, porto)) {
			System.err.println("Este node ja esta registado");
			throw new RuntimeErrorException(null);
		}

		dealWithNode.storageNodeAddress = endereco;
		dealWithNode.storageNodePort = porto;
		NoInf noInf = new NoInf(endereco, porto);

		storageNodeList.add(noInf);
		System.out.println("StorageNodeList -> " + storageNodeList);
	}

	public ArrayList<NoInf> getNos() {
		return storageNodeList;
	}

	private class DealWithNode extends Thread {

		private BufferedReader in;
		private PrintWriter out;
		private Socket socket;

		private String storageNodeAddress;
		private String storageNodePort;

		private boolean inscrito;

		public DealWithNode(Socket socket) throws IOException {
			this.socket = socket;
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("Estabeleci uma conexao: " + socket);
			this.inscrito = false;

		}

		@Override
		public void run() {
			try {
				serveNode();
			} catch (Exception e) {
				System.err.println("O nó " + this.storageNodeAddress + " " + this.storageNodePort + " desconectou-se");
			} finally {
				try {
					if (inscrito)
						removeStorageNode(this.storageNodeAddress, this.storageNodePort);
					in.close();
					out.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private void serveNode() throws IOException {
			while (true) {
				String str = in.readLine();
				if (str == null)
					return;
				System.out.println("Nova mensagem : " + socket + " : " + str);
				dealWithString(str);
				out.println(str);
			}
		}

		private void dealWithString(String s) {
			if (s == null)
				return;
			String[] contents = s.split(" ");
			switch (contents[0]) {
			case "INSC":
				if (contents.length == 3) {
					inscrever(contents[1], contents[2], this);
					this.inscrito = true;
				}
				break;
			case "nodes":
				sendNodes();
				break;
			default:
				break;
			}
		}

		private void sendNodes() {
			for (NoInf ni : storageNodeList) {
				out.println(ni.toString());
			}
			out.println("END");
		}
	}

	public class NoInf {
		private String[] no;

		public NoInf(String ip, String porto) {
			if (ip == null || porto == null) {
				System.err.println("Os argumentos introduzidos sao nulos");
				return;
			} else {
				no = new String[2];
				no[0] = ip;
				no[1] = porto;
			}
		}

		public String getIp() {
			return no[0];
		}

		public String getPorto() {
			return no[1];
		}

		@Override
		public String toString() {
			return "node " + getIp() + " " + getPorto();
		}
	}

	public static void main(String[] args) {
		try {
			new Diretorio(args[0]).serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}