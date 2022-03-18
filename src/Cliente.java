import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JButton;

public class Cliente {

	private InetAddress nodeIP;
	private int nodePort;
	private GUI gui;

	private Socket socket;
	private ObjectInputStream inObj;
	private ObjectOutputStream outObj;

	public Cliente(String nodeIP, String nodePort) {
		try {
			this.nodeIP = InetAddress.getByName(nodeIP);
			this.nodePort = Integer.parseInt(nodePort);
		} catch (Exception e) {
			System.err.println("Erro na criacao do cliente. Address ou Port mal atribuidos");
			e.printStackTrace();
			return;
		}

		gui = new GUI(createButton());
		gui.open();
		System.out.println("Criei o cliente");
	}

	public void startClient() {
		try {
			connectToNode();
		} catch (IOException e) {
			System.err.println("erro no connect to StorageNode");
			e.printStackTrace();
		}
	}

	private JButton createButton() {
		JButton consultarBtn = new JButton("Consultar");
		consultarBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					outObj.writeObject(new ByteBlockRequest(Integer.parseInt(gui.getTxtPosicaoConsultar()) - 1,
							Integer.parseInt(gui.getTxtComprimento())));
					new ReceiveContent().start();
				} catch (Exception e1) {
					System.err.println("CreateButton : erro no pedir conteudo");
				}
			}
		});
		return consultarBtn;
	}

	public void connectToNode() throws IOException {
		socket = new Socket(nodeIP, nodePort);
		System.out.println("Socket:" + socket);
		outObj = new ObjectOutputStream(socket.getOutputStream());
		inObj = new ObjectInputStream(socket.getInputStream());
	}

	private class ReceiveContent extends Thread {

		@Override
		public void run() {
			String response = "";
			try {
				response = getResult();
			} catch (Exception e) {
				System.err.println("erro no receiveContent");
			}
			System.out.println(response);
			gui.setRespostasTxtArea(response);
		}

		private String getResult() {
			try {
				String response = "";
				CloudByte[] tempCBA = (CloudByte[]) inObj.readObject();
				for (int i = 0; i < Integer.parseInt(gui.getTxtComprimento()); i++) {
					if (tempCBA[i].isParityOk())
						response = response.concat("" + tempCBA[i].toString() + " Parity OK, ");
					else
						response = response.concat("" + tempCBA[i].toString() + " Parity NOT OK, ");
				}
				return response;
			} catch (ClassNotFoundException | IOException e) {
				System.err.println("Dei erro no getResult");
				e.printStackTrace();
			}
			return null;
		}
	}

	public static void main(String[] args) {
		new Cliente(args[0], args[1]).startClient();
	}

}
