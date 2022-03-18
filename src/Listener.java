import java.util.Scanner;

public class Listener extends Thread {

	private StorageNode storageNode;
	
	public Listener(StorageNode storageNode) {
		this.storageNode=storageNode;
	}
	
	@Override
	public void run() {
		Scanner s = new Scanner(System.in);
		String in;
		while (true) {
			in = s.nextLine();
			try {
				if (in != null && in.split(" ")[0].equals("ERROR") && in.split(" ").length == 2
						&& Integer.parseInt(in.split(" ")[1]) < 1000001 && Integer.parseInt(in.split(" ")[1]) > 0) {
					storageNode.injectError(Integer.parseInt(in.split(" ")[1]));
				} else {
					System.err.println("Valores introduzidos não são aceites.");
				}
			} catch (Exception e) {
				System.err.println("o segundo argumento tem de ser um numero inteiro em [1,1000000]");
			}
		}
	}

}
