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
    LinkLayer ll;
    Integer offset;

    public Receiver(BlockingQueue<Packet> theQueue, BlockingQueue<Packet> sendQueue, BlockingQueue<Packet> ackQueue, short ourMac, RF theRF, Hashtable<Short,Short> seqNums, LinkLayer ll){
        this.recvQueue = theQueue;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.theRF = theRF;
        this.localMac = ourMac;
        this.seqNums = seqNums;
        this.ll = ll;
    }


	//
    public void run(){
        while(true) {
            try {
            	byte[] buffer = theRF.receive();
                ll.debugs("Received a packet");
                if(buffer.length > 0) {
                    Packet recvPacket = new Packet(buffer);
                    ll.debugs("Received packet destination: " + recvPacket.getDestShort());
                   // System.out.println("rec. packet dest: " + recvPacket.getDestShort());
                    //save dms and broadcasts to queue
                    if(recvPacket.getDestShort() == localMac || recvPacket.getDestShort() == -1) {
                        // ignore any packets if we have more than 4 packets in our recvQueue.
                        if(recvQueue.size() < 4) {
                            recvQueue.put(recvPacket);
                        }
                        //we want to make sure that we are not acknowledging acks and broadcasts. 
                        if(recvPacket.getFrameType() == (byte) 32 || recvPacket.getFrameType() == (byte) 1) {
                            //acks
                            ll.debugs("Received Ack from: " + recvPacket.getSrcShort());
                            ackQueue.put(recvPacket);
                            //System.out.println("Ackqueue size: " + ackQueue.size());
                        }else if(recvPacket.getDestShort() == localMac && recvQueue.size() < 4) {
                            //only respond to packets sent to us
                            ll.debugs("Received message for us: " + recvPacket.getDestShort());
                            //if we are receiving a brand new packet from a new destination, we set that seq num to zero.
                            if(!seqNums.containsKey(recvPacket.getSrcShort())){
                                seqNums.put(recvPacket.getSrcShort(), (short) 0);
                            }
                            //check if the packet is out of order
                            if(recvPacket.getSeqNumShort() >= seqNums.get(recvPacket.getSrcShort())+1){
                                //System.out.println("recieved an out of order packet.");
                            }
                            ll.debugs("this packet's frame type: " + recvPacket.getFrameType());
                            //now that we have recieved a packet we need to acknowledge that we got it
                            int length = 2048;
                            byte[] data = new byte[length];
                            for(int i = 0; i < length;i++){
                                data[i] = 0;
                            }
                            String ackMsg = "";
                            byte[] msg = ackMsg.getBytes();
                            Packet ack1 = new Packet(recvPacket.getSrcShort(), localMac, msg);
                            //ack1.setFrameType((byte) 1);
                            ack1.setAck();
                            ll.debugs("is ack1 an ack?" + (ack1.getFrameType() == 32));
                            ack1.setCRC();
                            ll.debugs("about to put ack on send queue stack");
                            while(theRF.inUse()){};
                            try{
                                Thread.sleep(theRF.aSIFSTime);
                            } catch (Exception e){
                                ll.debugs("problem sleeping sifs for ack");
                            }
                            theRF.transmit(ack1.myBytes);
                            ll.debugs("sent Ack1");
                           // ll.debugs("finished putting ack on the stack");
                        }
                    } else if(recvPacket.getFrameType() == (byte)2){
                        if(recvPacket.bytesToLong(recvPacket.getData()) > this.theRF.clock()+offset){
                            this.offset = (int) (recvPacket.bytesToLong(recvPacket.getData()) - theRF.clock());
                        }
                    }
                    else{
                    	ll.debugs("Received a packet meant for " + recvPacket.getDestShort());
                	}
                }

            } catch (Exception e){
               ll.debugs("getting the packet from the queue failed");
                ll.debugs(e.toString());
            }

        }
    }
}
