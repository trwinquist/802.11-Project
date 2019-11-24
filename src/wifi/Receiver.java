package wifi;
import java.util.*;
import java.util.concurrent.*;
import rf.RF;

public class Receiver implements Runnable {
    BlockingQueue<Packet> recvQueue;
    BlockingQueue<Packet> sendQueue;
    RF theRF;
    short localMac;
    
    public Receiver(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> sendQueue, short ourMac, RF theRF){
        this.recvQueue = theQueue;
        this.sendQueue = sendQueue;
        this.theRF = theRF;
        this.localMac = ourMac;
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
                    //if we receive a packet after and it is our acknowledgement put it in a box
                    if(recvPacket.getFrameType() == (byte) 001){
                        System.out.println("we received an acknowledgement!");
                    }else{
                        System.out.println("this packet's frame type: " + recvPacket.getFrameType());
                    }


                    //now that we have recieved a packet we need to acknowledge that we got it
                    Thread.sleep(theRF.aSIFSTime);
                    int length = 2048;
                    byte[] data = new byte[length];
                    for(int i = 0; i < length;i++){
                        data[i] = 0;
                    }
                    String ackMsg = "Acknowledged";
                    byte[] msg = ackMsg.getBytes();
                    Packet ack1 = new Packet(recvPacket.getSrcShort(), localMac, msg);
                    ack1.setFrameType((byte) 001);
                    ack1.setSeqNum(recvPacket.getSeqNumShort());
                    //ack1.setData(msg);
                    //ack1.setData(recvPacket.getData());
                    System.out.println(ack1.toString());
                    sendQueue.put(ack1);
                    System.out.println("finished putting ack on the stack");
                }
                	
            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
}