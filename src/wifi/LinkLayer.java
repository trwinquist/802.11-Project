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
	private int debug = 1;
	public boolean maxCW;
	
	private int status;

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
		this.output = output;
		try {
			theRF = new RF(null, null);
			//rf initialization success
			status = 1;
		} catch (Exception e){
			//RF_INIT_FAILED
			status = 3;
			System.out.println(e);
		}
		sendQueue = new LinkedBlockingQueue(4);
		recvQueue = new LinkedBlockingQueue(4);
		ackQueue = new LinkedBlockingQueue(4);
		Integer statusObj = new Integer(status);
		seqNums = new Hashtable<Short, Short>();
		Sender transmitter = new Sender(sendQueue, ackQueue, theRF, seqNums, statusObj);
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
		//bad argument status check
		if(dest < -1 || len < -1){
			//illegal arguments found
			status = 9;
		}
		Packet packet = new Packet(dest, ourMAC, data);
		//
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
				//successfully put packet on queue
				status = 1;
			} else {
				//return zero as per specification, as it didn't transmit .
				len = 0;
				status = 10;
			}

		} catch (Exception e){
			System.out.println("something went wrong adding the packet to the sendQueue");
			status = 2;
		}
		return len;
	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
	    if(t == null){
	        //illegal argument
	        status = 9;
        }

        if (this.recvQueue.isEmpty()) {
            output.println("Receive is being blocked, waiting for data.");
        }
        
        try {
            Packet p = (Packet)this.recvQueue.take();
            byte[] data = p.getData();
            t.setSourceAddr(p.getSrcShort());
            t.setDestAddr(p.getDestShort());
            t.setBuf(data);
            status = 1;
            //if we receive an ack
			if(p.getFrameType() == (byte) 32) {
				//last transmission was acknowledged
				status = 4;
				System.out.println(status());
			}
            return data.length;
        } catch (InterruptedException e) {
            System.err.println("Interrupted while de-queueing the incoming data!");
            e.printStackTrace();
            //unspecified failure
			status = 2;
            return -1;
        } 
	}

	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return status;
	}

	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		Packet p;
		Packet beacon;
		int bug;
		
	    if(cmd > 3 || cmd < 0){
	        //illegal argument
            status = 9;
        }
		output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		return 0;   
		
	    switch (cmd) {
	    case 0:
	        this.output.println("-------------- Commands -----------------");
	        this.output.println("Command #0: Display commands and their settings.");
	        this.output.println("Command #1: Set debug level.  Currently at: " + this.debug +  "\n Use 0 for full debug output, and any other number for none.");
	        this.output.println("Command #2: Set slot selection method.  Currently " + (this.maxCW ? "max" : "random") + 
	            "\n        Use 0 for random selection, and anything > 0 for max.");
	        this.output.println("Command #3: Set beacon interval.  Currently at " + this.theMAC.getBeaconInterval() + " seconds" + 
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
	
	/**
	 * 
	 * @param printThis The string to print out.
	 */
	public void debugs(String printThis) {
		if(this.debug == 0) {
			this.output.print(printThis);
		}
	}
}
