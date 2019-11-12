package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Reciever implements Runnable {
    BlockingQueue<Packet> recvQueue;
    RF theRF;
    public Reciever(BlockingQueue<Packet> theQueue, RF theRF){
        this.recvQueue = theQueue;
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
                }
                Thread.sleep(7000);
                	
            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
}