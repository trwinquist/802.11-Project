package wifi;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.text.html.HTMLDocument.BlockElement;

import rf.RF;

public class Receiver implements Runnable {
    BlockingQueue<Packet> recvQueue;
    BlockingQueue<Packet> sendQueue;
    BlockingQueue<Packet> ackQueue;
    RF theRF;
    short localMac;

    public Receiver(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> sendQueue, BlockingQueue<Packet> ackQueue, short ourMac, RF theRF){
        this.recvQueue = theQueue;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
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
                        if(recvPacket.getFrameType() == (byte) 32) {
                            System.out.println("Received Ack!");
                            ackQueue.put(recvPacket);
                        }else if(recvPacket.getDestShort() == -1){
                            System.out.println("Received Broadcast!");
                        }else{
                            System.out.println("this packet's frame type: " + recvPacket.getFrameType());
                            //now that we have recieved a packet we need to acknowledge that we got it
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
