package wifi;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.text.html.HTMLDocument.BlockElement;

import rf.RF;

public class Receiver implements Runnable {
    BlockingQueue<Packet> recvQueue;
    BlockingQueue<Packet> sendQueue;
    BlockingQueue<Packet> ackQueue;
    Hashtable<Short, Short> seqNums;
    RF theRF;
    short localMac;

    public Receiver(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> sendQueue, BlockingQueue<Packet> ackQueue, short ourMac, RF theRF, Hashtable<Short,Short> seqNums){
        this.recvQueue = theQueue;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.theRF = theRF;
        this.localMac = ourMac;
        this.seqNums = seqNums;
    }

    //
    public void run(){
        while(true) {
            try {
            	byte[] buffer = theRF.receive();
                //System.out.println("Received a packet");
                if(buffer.length > 0) {
                    Packet recvPacket = new Packet(buffer);
                   // System.out.println("rec. packet dest: " + recvPacket.getDestShort());
                    //save dms and broadcasts to queue
                    if(recvPacket.getDestShort() == localMac || recvPacket.getDestShort() == -1) {
                        // ignore any packets if we have more than 4 packets in our recvQueue.
                        if(recvQueue.size() < 4) {
                            recvQueue.put(recvPacket);
                        }
                        //we want to make sure that we are not acknowledging acks and broadcasts. 
                        if(recvPacket.getFrameType() == (byte) 32) {
                            //acks
                            // System.out.println("Received Ack!");
                            ackQueue.put(recvPacket);
                            //System.out.println("Ackqueue size: " + ackQueue.size());
                        }else if(recvPacket.getDestShort() == localMac && recvQueue.size() < 4) {
                            //only respond to packets sent to us
                            System.out.println("Received message for us!");
                            //if we are receiving a brand new packet from a new destination, we set that seq num to zero.
                            if(!seqNums.containsKey(recvPacket.getSrcShort())){
                                seqNums.put(recvPacket.getSrcShort(), (short) 0);
                            }
                            //check if the packet is out of order
                            if(recvPacket.getSeqNumShort() >= seqNums.get(recvPacket.getSrcShort())+1){
                                //System.out.println("recieved an out of order packet.");
                            }
                            System.out.println("this packet's frame type: " + recvPacket.getFrameType());
                            //now that we have recieved a packet we need to acknowledge that we got it
                            int length = 2048;
                            byte[] data = new byte[length];
                            for(int i = 0; i < length;i++){
                                data[i] = 0;
                            }
                            String ackMsg = "";
                            byte[] msg = ackMsg.getBytes();
                            Packet ack1 = new Packet(recvPacket.getSrcShort(), localMac, msg);
                            ack1.setFrameType((byte) 1);
                            ack1.setSeqNum(recvPacket.getSeqNumShort());
                            //ack1.setSeqNum((short) 1);
                            //ack1.setData(msg);
                            //ack1.setData(recvPacket.getData());
                            System.out.println("about to put ack on send queue stack");
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
