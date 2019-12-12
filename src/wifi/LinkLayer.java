package wifi;
import java.io.PrintWriter;
import java.util.Hashtable;
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
	private BlockingQueue<Packet> ackQueue;
	private Hashtable<Short, Short> seqNums;
	private int debug;
	public boolean maxCW;
	

	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
	 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output; 
		this.maxCW = false;
		theRF = new RF(null, null);
		sendQueue = new LinkedBlockingQueue(4);
		recvQueue = new LinkedBlockingQueue(4);
		ackQueue = new LinkedBlockingQueue(4);
		seqNums = new Hashtable<Short, Short>();
		Sender transmitter = new Sender(sendQueue, ackQueue, theRF, seqNums);
		Receiver getter = new Receiver(recvQueue, sendQueue, ackQueue, ourMAC, theRF, seqNums);
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
		if(seqNums.containsKey(dest)){
			seqNums.put(dest, (short) (seqNums.get(dest)+1));

		} else {
			seqNums.put(dest,(short) 0);
		}
		packet.setSeqNum(seqNums.get(dest));
		try {
			//only puts packet on the send queue if there aren't more that 4.
			if( sendQueue.size() < 4) {
				sendQueue.put(packet);
			} else {
				//return zero as per specification, as it didn't transmit .
				len = 0;
			}
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
		Packet p;
		Packet beacon;
		int bug;
		
		
		
	    switch (cmd) {
	    case 0:
	        this.output.println("-------------- Commands and Settings -----------------");
	        this.output.println("Cmd #0: Display command options and current settings");
	        this.output.println("Cmd #1: Set debug level.  Currently at " + this.debug + 
	            "\n        Use -1 for full debug output, 0 for no output");
	        this.output.println("Cmd #2: Set slot selection method.  Currently " + (this.maxCW ? "max" : "random") + 
	            "\n        Use 0 for random slot selection, any other value to use maxCW");
	        this.output.println("Cmd #3: Set beacon interval.  Currently at " + this.theMAC.getBeaconInterval() + " seconds" + 
	            "\n        Value specifies seconds between the start of beacons; -1 disables");

	        
	        this.output.println("------------------------------------------------------");
	        
	    case 1:
	    	bug = this.debug;
	    	this.debug = val;
	    	this.output.println("Setting debug to: " + val);
	    	return bug;
	    
	    case 2:
	    	if(val != 0) {
	    		this.maxCW = true;
	    	}
	    	if(this.maxCW) {
	    		this.output.print("Using max collision window.");
	    	} else this.output.print("Using a random collision window.");
	    	
	    case 3:
	    	if(val < 0) {
	    		this.output.print("Frames will never be set.");
	    		//this.theMAC.setBeaconInterval(3600);
	    	} else {
	    		this.output.print("Frames will be sent every " + val + " seconds.");
	    		//this.theMAC.setBeaconInterval(val);
	    	}
	    	
	    	
	    
	        
	        
	        
	        
	        
	    }
	}
}
