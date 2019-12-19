
package wifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
//import java.lang;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Packet{
    /*
        Packet Class 
        
        Fields
            Control field : 2 bytes
                Frame type: 3 bits
                Retry: 1 bit
                Sequence Num: 12 bits
            Destination address : 2 bytes
            Source Address : 2 bytes
            Data : 2038 bytes lmao
            CRC : 4 bytes
            ByteArray : 2048 array of all bytes

        
        Methods
            toString()
                returns ByteArray to send on rf layer
            setters and getters for all fields
            
    */
    public byte[] myBytes;
    private final int controlFieldIndex = 2;
    private final Byte frameTypeByte = new Byte((byte) 224);
    private final Byte retryByte = new Byte((byte) 16);
    private final Byte seqNumByte = new Byte((byte) 15);
    private final int destIndex = 2;
    private final int srcIndex = 4;
    private final int dataIndex = 6;
    public Checksum crc;
    private byte[] dumbCrc = new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    public int crcIndex = 6;
    public boolean isRetry;
    

    public Packet(byte[] byteArray){
        myBytes = byteArray;
        crcIndex = myBytes.length - 4;
        crc = new CRC32();
        setCRC();
    }
    
    //data.len = 2038 src.len = 2 dest.len = 2 seqNum.len = 2
    public Packet(byte[] seqNum, byte[] dest, byte[] src, byte[] data){
        this.myBytes = new byte[10+data.length];
        setDest(dest);
        setSrc(src);
        crc = new CRC32();
        setCRC();
        setData(data);
        //setDumbCrc();
    }

    public Packet(short dest, short ourMac, byte[] data){
        myBytes = new byte[10+data.length];
        setDest(shortToBytes(dest));
        setSrc(shortToBytes(ourMac));
        crc = new CRC32();
        setCRC();
        setData(data); 
    }

    public Packet(short dest, short ourMac){
        myBytes = new byte[10];
        setDest(shortToBytes(dest));
        setSrc(shortToBytes(ourMac));
        crc = new CRC32();
        setCRC();
        setData(new byte[]{});
        
    }
    
    public byte[] getControlField(){
        byte[] controlField = new byte[controlFieldIndex];
        for(int i = 0; i < controlFieldIndex; i++){
            controlField[i] = myBytes[i];
        }
        return controlField;
    }

    public byte getFrameType(){
        byte frameType = getControlField()[0];
        //frame data is controlField & 11100000
        frameType = (byte) (frameTypeByte & frameType);
        return frameType;
    }

    public void setFrameType(byte typeByte){
        //& first byte of CF with 00011111 to reset frame type
        myBytes[0] = typeByte;
        myBytes[0] = (byte)(myBytes[0] & (byte)31);
        myBytes[0] = (byte)(myBytes[0] | (byte)(typeByte << 5));
        setCRC();
    }

    public byte getRetry(){
        byte retry = getControlField()[0];
        return (byte) (retry & retryByte);
    }
    public void setRetry(){
        myBytes[0] = (byte) (myBytes[0] & (byte) 16);
        setCRC();
    }

    public short getSeqNumShort(){
        byte [] controlField = getControlField();
        controlField = new byte[]{ (byte) (seqNumByte.byteValue() & controlField[0]), controlField[1]};
        return bytesToShort(controlField[0], controlField[1]);
    }

    public byte[] getSeqNum(){
        byte [] controlField = getControlField();
        return new byte[]{ (byte) (seqNumByte.byteValue() & controlField[0]), controlField[1]};
    }

    //takes in seq num as short, translates to 2 byte array
    public void setSeqNum(short seqNum){
        byte[] seqNumBytes = shortToBytes(seqNum);
        byte controlByte = (byte)(getSeqNum()[0] & new Byte((byte) 240));
        seqNumBytes[0] = (byte) (myBytes[0] & seqNumByte.byteValue());
        seqNumBytes[0]  = (byte) (seqNumBytes[0] & controlByte);
        myBytes[0] = seqNumBytes[0];
        myBytes[1] = seqNumBytes[1];
        setCRC();
    }
    
    public byte[] getPacket(){
        return myBytes;
    }

    public void resetPacket(byte[] newBytes){
        myBytes = newBytes;
    }

    public byte[] getDest(){
        return new byte[]{myBytes[destIndex], myBytes[destIndex+1]};
    }

    public short getDestShort(){
        return bytesToShort(getDest()[0],getDest()[1]);
    }

    public void setDest(byte[] newDest){
        byte[] newEstDest = shortToBytes((short)788);
        myBytes[destIndex] = newDest[0];
        myBytes[destIndex+1] = newDest[1];
        setCRC();
    }

    public byte[] getSrc(){
        return new byte[]{myBytes[srcIndex], myBytes[srcIndex+1]};
    }

    public short getSrcShort(){
        return bytesToShort(getSrc()[0],getSrc()[1]);
    }

    public void setSrc(byte[] newSrc){
        myBytes[srcIndex] = newSrc[0];
        myBytes[srcIndex+1] = newSrc[1];
        setCRC();
    }
    
    public byte[] getCRC(){
        byte[] crc = new byte[4];
        for(int i = 0; i < crc.length; i++){
            crc[i] = myBytes[crcIndex+i];
        }
        return crc;
    }
    
    public void setCRC(){
        for(int i = 1; i <= 4; i++){
            myBytes[myBytes.length - i] = (byte)0;
        }
        int crcIdx = this.myBytes.length - 4;
        int crcInt = calcCRC();
        this.myBytes[crcIdx + 0] = (byte)(crcInt >>> 24 & 0xFF);
        this.myBytes[crcIdx + 1] = (byte)(crcInt >>> 16 & 0xFF);
        this.myBytes[crcIdx + 2] = (byte)(crcInt >>> 8 & 0xFF);
        this.myBytes[crcIdx + 3] = (byte)(crcInt & 0xFF);
        
    }

    public int calcCRC(){
        CRC32 anotherCRC = new CRC32();
        anotherCRC.update(this.myBytes, 0, this.myBytes.length - 4);
        return (int)anotherCRC.getValue();
    }

    public void setDumbCrc(){
        for(int i = 0; i < dumbCrc.length; i++){
            myBytes[crcIndex+i] = dumbCrc[i];
        }
    }

    public byte[] getData(){
        byte[] data = new byte[myBytes.length-10];
        for(int i = 0; i < data.length; i++){
            data[i] = myBytes[dataIndex+i];
        }
        return data;
    }

    public void setData(byte[] data){
        byte[] tempBytes = getPacket();
        int dataDiff = Math.abs(data.length - getData().length);
        byte[] newMyBytes = new byte[tempBytes.length + dataDiff];
        //overwrite old data and dest/src info
        for(int i = 0; i < dataIndex; i++){
            newMyBytes[i] = myBytes[i];
        }
        for(int i = 0; i < data.length; i++){
            newMyBytes[dataIndex+i] = data[i];
        }
        resetPacket(newMyBytes);
        //reset crcIndex
        crcIndex = myBytes.length - 4;
        setCRC();
    }

    public short bytesToShort(byte b1, byte b2) {
        return (short) (((b1 << 8)) | ((b2 & 0xff)));
    }

    public byte[] shortToBytes(short s){
        return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
    }

    public long bytesToLong(byte[] by){
        long value = 0;
            for (int i = 0; i < by.length; i++){
                value += ((long) by[i] & 0xffL) << (8 * i);
        }  
        return value; 
    }

    public byte[] longToBytes(long x){
        byte[] bytes = new byte[8];
        for(int i = bytes.length-1; i >= 0; i--){
            bytes[i] = (byte) (x >> (3-i)*8);
        }
        return bytes;
    }

    public byte[] intToBytes(int x){
        byte[] bytes = new byte[4];
        for(int i = bytes.length-1; i >= 0; i--){
            bytes[i] = (byte) (x >> (3-i)*8);
        }
        return bytes;
    }

    private int calculateChecksumOLD() {
        int remainder = -1;
         
        int nBytes = this.myBytes.length - 4;
         
        for (int bytes = 0; bytes < nBytes; bytes++) {
          
         remainder ^= this.myBytes[bytes] << 248;
          
         for (int bit = 8; bit > 0; bit--) {
           
          if ((remainder & 0x80000000) != 0) {
            
           remainder = remainder << 1 ^ 0x4C11DB7;
          }
          else {
            
           remainder <<= 1;
          } 
         } 
        } 
         
        return remainder ^ 0xFFFFFFFF;
    }

    public String toString(){
        String toString = "";
        toString += "Frame Type: " + getFrameType() + "\n";
        toString += "Retry: " + getRetry() + "\n";
        toString += "Sequence Num: "+getSeqNumShort()+"\n";
        toString += "Destination Address: " + getDestShort() + "\n";
        toString += "Source Address: " + getSrcShort() + "\n";
        toString += "Data: ";
        for(int i = 0; i < getData().length; i++){
            toString += getData()[i]; 
        }
        toString += "\n";
        toString += "CRC: ";
        for(int i = 0; i < getCRC().length; i++){
            toString += getCRC()[i];
        }

        toString += "\n";
        for(int i = 0; i < myBytes.length; i++){
            toString += myBytes[i];
        }
        return toString;

    }

    public boolean test() {
        this.myBytes = "1234567890000".getBytes();
        crc.update(myBytes, 0, myBytes.length-4);
        int crcVal = (int)crc.getValue();
        System.out.println(crcVal);
        this.myBytes = "1234567890000".getBytes();
        CRC32 calc = new CRC32();
        calc.update(myBytes, 0, myBytes.length - 4);
        int butts = (int)calc.getValue();
        System.out.println(butts);
        return (crcVal == butts);
    }
    
    public static void main(String[] args){
        byte[] data = new byte[3];
        byte[] src = new byte[2];
        System.out.println("new default packet");
        Packet packet = new Packet(src, src, src, data);
        //tests
        packet.setDest(new byte[] {1,1});
        //System.out.println(packet.toString());
        packet.setData(new byte[20]);
        //System.out.println(packet.toString());
        packet.setData("".getBytes());
        //packet.setCRC();
        System.out.println(packet.toString());  
        //0000011000001001
        packet.setSrc(new byte[]{6,9});
        //System.out.println(packet.toString());  
        packet.setSeqNum((short)80);
        System.out.println(packet.toString());
        packet.setFrameType((byte)100);
        packet.setSeqNum(packet.getSeqNumShort());
        System.out.println(packet.toString());
        Packet timePacket = new Packet((byte)-1, (short) 1);
        timePacket.setData(new byte[]{6,9});
        timePacket.setFrameType((byte)2);
        timePacket.setRetry();
        System.out.println(timePacket.toString());
        System.out.println(timePacket.getControlField());
        System.out.println(timePacket.getFrameType());
        System.out.println(timePacket.getRetry());
        System.out.println(timePacket.getSeqNumShort());
        System.out.println(timePacket.getSeqNum());
        timePacket.setSeqNum((short)1);
        System.out.println(timePacket.getPacket());
        timePacket.resetPacket(timePacket.getPacket());
        System.out.println(timePacket.getDest());
        System.out.println(timePacket.getDestShort());
        System.out.println(timePacket.getSrc());
        System.out.println(timePacket.getSrcShort());
        System.out.println(timePacket.getCRC());
        timePacket.setCRC();
        System.out.println();
        byte[] testBytes = timePacket.longToBytes((long)257);
        byte[] intTestBytes = timePacket.intToBytes(257);
        System.out.println("byte1: "+testBytes[0]+" byte2: "+testBytes[1]+" byte3: "+testBytes[2]+" byte4: "+testBytes[3]);
        System.out.println("byte1: "+intTestBytes[0]+" byte2: "+intTestBytes[1]+" byte3: "+intTestBytes[2]+" byte4: "+intTestBytes[3]);

        int oldChecksum = timePacket.calculateChecksumOLD();
        int newChecksum = (int)timePacket.crc.getValue();
        System.out.println(oldChecksum + " " + newChecksum);
        System.out.println(timePacket.test());
    }
}
    