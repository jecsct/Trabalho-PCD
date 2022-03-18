import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class GUI {

	private JFrame gui;
	private JButton consultarBtn;
	private JTextField txtPosicaoConsultar;
	private JTextField txtComprimento;
	private JTextArea respostas;
	private boolean textAreaUsed = false;
	
	public GUI(JButton consultarBtn) {
		if (consultarBtn.equals(null)) {
			System.err.println("Erro na criacao do cliente. Nao foi atribuido nenhum button");
			return;
		}
		this.consultarBtn = consultarBtn;
		gui = new JFrame("Client");
		gui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addFrameContent();
		gui.pack();
	}

	private void addFrameContent() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 7));

		JLabel vazio1 = new JLabel();
		panel.add(vazio1);

		JLabel lblPosicaoConsultar = new JLabel("Posicao a consultar:");
		panel.add(lblPosicaoConsultar, BorderLayout.EAST);

		txtPosicaoConsultar = new JTextField();
		panel.add(txtPosicaoConsultar);

		JLabel lblComprimento = new JLabel("Comprimento:");
		panel.add(lblComprimento, BorderLayout.EAST);

		txtComprimento = new JTextField();
		panel.add(txtComprimento);

		panel.add(consultarBtn);

		JLabel vazio2 = new JLabel();
		panel.add(vazio2);

		gui.add(panel, BorderLayout.NORTH);

		respostas = new JTextArea("As respostas aparecerao aqui");
		gui.add(respostas, BorderLayout.CENTER);
	}

	public String getTxtPosicaoConsultar() {
		return txtPosicaoConsultar.getText();
	}

	public String getTxtComprimento() {
		return txtComprimento.getText();
	}

	public void setRespostasTxtArea(String resposta) {
		if (!textAreaUsed) {
			respostas.setText("[" + resposta + "]");
			textAreaUsed = true;
			return;
		}
		String novoTxt = respostas.getText().concat("\n[" + resposta + "]");
		respostas.setText(novoTxt);
	}

	public void open() {
		gui.setVisible(true);
	}
	
}