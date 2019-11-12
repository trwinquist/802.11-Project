package wifi;
//import java.lang;

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
    //private final Byte frameTypeByte = new Byte(224);
    //private final byte retryByte = (byte)16;
    //private final byte seqNumByte = (byte)15;
    private final int destIndex = 2;
    private final int srcIndex = 4;
    private final int dataIndex = 6;
    public int crcIndex = 2044;
    public boolean isRetry;
    public String controlField;

    public Packet(byte[] byteArray){
        myBytes = byteArray;
    }
    //data.len = 2038 src.len = 2 dest.len = 2 seqNum.len = 2
    public Packet(byte[] seqNum, byte[] dest, byte[] src, byte[] data){
        this.myBytes = new byte[10+data.length];
        setDest(dest);
        setSrc(src);
        setData(data);
    }
    /*
    public byte[] getControlField(){
        byte[] controlField = new byte[controlFieldIndex];
        for(int i = 0; i < controlFieldIndex; i++){
            controlField[i] = myBytes[i];
        }
        return controlField;
    }

    public byte getFrameType(){
        //byte frameType = getControlField()[0];
        //frame data is controlField & 11100000
        frameType = frameTypeByte & frameType;
        return frameType;
    }

    public byte getRetry(){
        byte retry = getControlField()[0];
        retry = retry & retryByte;
        if(retry == 0){
            return retry;
        }
        return 1;
    }

    public byte[] getSeqNum(){
        byte [] controlField = getControlField();
        return new byte[]{seqNumByte & controlField[0], controlField[1]};
    }
    */
    public byte[] getPacket(){
        return myBytes;
    }

    public byte[] getDest(){
        return new byte[]{myBytes[destIndex], myBytes[destIndex+1]};
    }

    public short getDestShort(){
        return bytesToShort(getDest()[0],getDest()[1]);
    }

    public void setDest(byte[] newDest){
        myBytes[destIndex] = newDest[0];
        myBytes[destIndex+1] = newDest[1];
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
    }
    
    public byte[] getCRC(){
        byte[] crc = new byte[4];
        for(int i = 0; i < crc.length; i++){
            crc[i] = myBytes[crcIndex+i];
        }
        return crc;
    }

    public byte[] getData(){
        byte[] data = new byte[myBytes.length-10];
        for(int i = 0; i < data.length; i++){
            data[i] = myBytes[dataIndex+i];
        }
        return data;
    }

    public void setData(byte[] data){
        for(int i = 0; i < data.length; i++){
            myBytes[dataIndex+i] = data[i];
        }
        crcIndex = dataIndex + data.length;
    }

    private short bytesToShort(byte b1, byte b2) {
        int temp = b1 & 0xFF;
        temp = temp << 8 | b2 & 0xFF;
        return (short)temp;
    }

    
    public static void main(String[] args){
        byte[] data = new byte[3];
        byte[] src = new byte[2];
        Packet packet = new Packet(src, src, src, data);
        System.out.print(packet.getData()+""+packet.getDest()+" "+packet.getSrc()+" "+packet.getCRC());
        byte[] packetArr = packet.getPacket();
        System.out.println("fuck "+packetArr.length);
        for(int i = 0; i < packet.getPacket().length; i++){
            System.out.println(packetArr[i]);
        }
    }
}
    