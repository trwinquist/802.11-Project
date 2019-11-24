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
                    System.out.println("rec. packet dest: " + recvPacket.getDestShort());
                    //save dms and broadcasts to queue
                    if(recvPacket.getDestShort() == localMac || recvPacket.getDestShort() == -1) {
                        recvQueue.put(recvPacket);
                        
                        //we want to make sure that we are not acknowledging acks and broadcasts. 
                        if(recvPacket.getFrameType() == (byte) 32 || recvPacket.getDestShort() == -1){
                            System.out.println("we received an acknowledgement or a broadcast, no ack back!");
                        }else{
                            System.out.println("this packet's frame type: " + recvPacket.getFrameType());
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
                            //ack1.setSeqNum(recvPacket.getSeqNumShort());
                            ack1.setSeqNum((short) 1);
                            //ack1.setData(msg);
                            //ack1.setData(recvPacket.getData());
                            sendQueue.put(ack1);
                            System.out.println("finished putting ack on the stack");
                        }
                	} else {
                		System.out.println("Received a packet meant for " + recvPacket.getDestShort());
                	}
                }

            } catch (Exception e){
                System.out.println("getting the packet from the queue failed");
            }

        }
    }
}
