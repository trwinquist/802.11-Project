package wifi;
import java.util.concurrent.*;
import rf.RF;

public class Reciever implements Runnable {
    BlockingQueue<Packet> recvQueue;
    RF theRF;
    short ourMAC;
    public Reciever(BlockingQueue<Packet> theQueue, RF theRF, short ourMAC){
        this.recvQueue = theQueue;
        this.theRF = theRF;
        this.ourMAC = ourMAC;
    }
    
    //
    public void run(){
        while(true) {
            try {
            	byte[] buffer = theRF.receive();
                System.out.println("Received a packet");
                if(buffer.length > 0) {
                	Packet recvPacket = new Packet(buffer);
                	short destMAC = recvPacket.getDestShort();
                	if(destMAC == ourMAC) {
                    	recvQueue.put(recvPacket);
                	} else {
                		System.out.println("Received a packet meant for " + destMAC);
                	}
                }
                	
            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
}