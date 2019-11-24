package wifi;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface 
{
	private RF theRF;           // You'll need one of these eventually
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	private BlockingQueue<Packet> recvQueue;
	private BlockingQueue<Packet> sendQueue;

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;      
		theRF = new RF(null, null);
		sendQueue = new LinkedBlockingQueue(4);
		recvQueue = new LinkedBlockingQueue(4);
		Sender transmitter = new Sender(sendQueue, theRF);
		Receiver getter = new Receiver(recvQueue, sendQueue, theRF);
		(new Thread(transmitter)).start();
		(new Thread(getter)).start();
		output.println("LinkLayer: Constructor ran.");
	}

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		output.println("LinkLayer: Sending "+len+" bytes to "+dest);
		Packet packet = new Packet(dest, ourMAC, data);
		try {
			sendQueue.put(packet);
		} catch (Exception e){
			System.out.println("something went wrong adding the packet to the sendQueue");
		}
		return len;
	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
        if (this.recvQueue.isEmpty()) {
            output.println("Receive is being blocked, waiting for data.");
        }
        
        try {
            Packet p = (Packet)this.recvQueue.take();
            byte[] data = p.getData();

            t.setSourceAddr(p.getSrcShort());
            t.setDestAddr(p.getDestShort());
            t.setBuf(data);
            return data.length;
        } catch (InterruptedException e) {
            System.err.println("Interrupted while dequeueing the incoming data!");
            e.printStackTrace();

            return -1;
        } 
	}

	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		return 0;
	}
}
