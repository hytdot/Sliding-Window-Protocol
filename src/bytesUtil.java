public class bytesUtil {
    //short转换为byte[2]
    public byte[] shortToByte2(short num) {
        byte[] result = new byte[2];
        result[0] = (byte)(num >>> 8);
        result[1] = (byte)num;
        return result;
    }

    //byte[2]转换为int
    public int byte2ToInt(byte[] bytes) {
        int result = 0;
        byte[] intBytes = new byte[4];
        intBytes[0] = 0;
        intBytes[1] = 0;
        intBytes[2] = bytes[0];
        intBytes[3] = bytes[1];
        result = byte4ToInt(intBytes);
        return result;
    }

    //int转换为byte[4]
    public byte[] intToByte4(int num) {
        byte[] result = new byte[4];
        result[0] = (byte)(num >>> 24);
        result[1] = (byte)(num >>> 16);
        result[2] = (byte)(num >>> 8);
        result[3] = (byte)num;
        return result;
    }

    //byte[4]转换为int
    public int byte4ToInt(byte[] bytes) {
        int result = 0;
        result = bytes[0] & 0xff;
        result = result << 8 | bytes[1] & 0xff;
        result = result << 8 | bytes[2] & 0xff;
        result = result << 8 | bytes[3] & 0xff;
        return result;
    }

    //byte[]转换为TCP报文段
    public TCPsegment byteToTcp(byte[] data) {
        TCPsegment tcpSeg = new TCPsegment();
        int num = 0;
        for (int i = 0; i < tcpSeg.src_port.length; i++) {
            tcpSeg.src_port[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.dest_port.length; i++) {
            tcpSeg.dest_port[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.seq.length; i++) {
            tcpSeg.seq[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.ack.length; i++) {
            tcpSeg.ack[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.offset.length; i++) {
            tcpSeg.offset[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.wnd.length; i++) {
            tcpSeg.wnd[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.checksum.length; i++) {
            tcpSeg.checksum[i] = data[num];
            num++;
        }
        for (int i = 0; i < tcpSeg.Upointer.length; i++) {
            tcpSeg.Upointer[i] = data[num];
            num++;
        }
        if (data.length == 21) {
            tcpSeg.setData(data[num]);
        }
        else {
            tcpSeg.setData((byte) 0);
        }
        return tcpSeg;
    }

    //TCP报文段转换为byte[]
    public byte[] tcpToBytes(TCPsegment tcpSeg, int len) {
        byte[] result = new byte[len];
        int num = 0;
        for (int i = 0; i < tcpSeg.src_port.length; i++) {
            result[num] = tcpSeg.src_port[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.dest_port.length; i++) {
            result[num] = tcpSeg.dest_port[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.seq.length; i++) {
            result[num] = tcpSeg.seq[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.ack.length; i++) {
            result[num] = tcpSeg.ack[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.offset.length; i++) {
            result[num] = tcpSeg.offset[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.wnd.length; i++) {
            result[num] = tcpSeg.wnd[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.checksum.length; i++) {
            result[num] = tcpSeg.checksum[i];
            num++;
        }
        for (int i = 0; i < tcpSeg.Upointer.length; i++) {
            result[num] = tcpSeg.Upointer[i];
            num++;
        }
        if (len == 21) {
            for (int i = 0; i < tcpSeg.data.length; i++) {
                result[num] = tcpSeg.data[i];
                num++;
            }
        }
        return result;
    }

    //伪首部转换为byte[]
    public byte[] pHeaderTobytes(pseudoHeader pHeader) {
        byte[] result = new byte[12];
        int num = 0;
        for (int i = 0; i < 4; i++) {
            result[num] = pHeader.saddr[i];
            num++;
        }
        for (int i = 0; i < 4; i++) {
            result[num] = pHeader.daddr[i];
            num++;
        }
        for (int i = 0; i < 1; i++) {
            result[num] = pHeader.mbz[i];
            num++;
        }
        for (int i = 0; i < 1; i++) {
            result[num] = pHeader.ptcl[i];
            num++;
        }
        for (int i = 0; i < 2; i++) {
            result[num] = pHeader.tcpl[i];
            num++;
        }
        return result;
    }
/*
    public static void main(String[] args) {
        bytesUtil util = new bytesUtil();
        byte[] bytes = new byte[2];
        bytes[0] = 127;
        bytes[1] = 0;
        int result = util.byte2ToInt(bytes);
        System.out.println(result);
    }*/
}
