package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Reciever implements Runnable {
    BlockingQueue<Packet> recvQueue;
    BlockingQueue<Packet> sendQueue;
    RF theRF;
    public Reciever(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> sendQueue, RF theRF){
        this.recvQueue = theQueue;
        this.sendQueue = sendQueue;
        this.theRF = theRF;
    }
    
    //
    public void run(){
        while(true) {
            try {
            	byte[] buffer = theRF.receive();
                System.out.println("Received a packet");
                if(buffer.length > 0) {
                	Packet recvPacket = new Packet(buffer);
                    recvQueue.put(recvPacket);
                    
                    //now that we have recieved a packet we need to acknowledge that we got it
                    Thread.Sleep(theRF.aSIFSTime);
                    byte[] data = new byte[2048];
                    Packet ack1 = new Packet(data);
                    ack1.setFrameType((byte) 001);
                    ack1.setDest(recvPacket.getSrc());
                    sendQueue.put(ack1);
                }
                	
            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
}