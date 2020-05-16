import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class client {
    public JTextArea textPrint;

    public void open(JTextArea textArea) {
        this.textPrint = textArea;
        Socket socket;
        String id = "192.168.0.102";
        int port = 5555;
        buffers bufs = new buffers();

        try {
            //创建socket并连接127.0.0.1的5555端口
            socket=new Socket(id, port);
            textPrint.append("请求连接......\n");
            textPrint.append("连接成功\n");
          //  System.out.println("连接成功");

            C_sender send = new C_sender(socket, bufs, textPrint);
            C_receiver receive = new C_receiver(socket, bufs, textPrint);
            send.start();
            receive.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Socket socket;
        String id = "192.168.0.102";//"127.0.0.1";
        int port = 5555;
        buffers bufs = new buffers();
        JTextArea textPrint = new JTextArea();
        try {
            //创建socket并连接127.0.0.1的5555端口
            socket=new Socket(id, port);
            System.out.println("连接成功");

            C_sender send = new C_sender(socket, bufs, textPrint);
            C_receiver receive = new C_receiver(socket, bufs, textPrint);
          //  send.setPriority(5);
           // receive.setPriority(10);
            send.start();
            receive.start();
/*            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] test1 = new byte[10];
            byte[] test2 = new byte[10];
            in.read(test1);
            in.read(test2);
            System.out.println(Arrays.toString(test1));
            System.out.println(Arrays.toString(test2));*/
            //socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//向服务器发送数据
class C_sender extends Thread {
    public Socket socket;
    public DataOutputStream out;
    public buffers bufs;//缓冲区
    public pseudoHeader pHeader = new pseudoHeader();    //TCP伪首部
    public TCPsegment tcpSeg = new TCPsegment();
    public bytesUtil util = new bytesUtil();
    public Random random = new Random();
    public JTextArea textPrint;

    public C_sender(Socket socket, buffers bufs, JTextArea textPrint) {
        this.socket = socket;
        this.bufs = bufs;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取TCP伪首部
        InetAddress ip1 = socket.getLocalAddress(); //获取发送ip
        InetAddress ip2 = socket.getInetAddress();  //获取目的ip
        this.pHeader.setSaddr(ip1.getAddress());
        this.pHeader.setDaddr(ip2.getAddress());
        this.pHeader.setTcpl(util.shortToByte2((short) 20));
        //设置TCP报文段端口
        int p1 = socket.getLocalPort();  //发送端口
        int p2 = socket.getPort();       //目的端口
        this.tcpSeg.setSrc_port(util.shortToByte2((short)p1));
        this.tcpSeg.setDest_port(util.shortToByte2((short)p2));
        this.textPrint = textPrint;
    }

    //打包数据
    public void tcpPacket(int seq, byte sign) {
        tcpSeg.setSeq(util.intToByte4(seq));
        tcpSeg.setAck(util.intToByte4(bufs.win_2));
        tcpSeg.setOffset(sign); //ACK=1,FIN=0是16;
        tcpSeg.setChecksum(util.shortToByte2((short) 0));
        tcpSeg.setData((byte) 0);
        tcpSeg.CheckSum(pHeader, tcpSeg);
    }

    public void run() {
        while (true) {
            //需要发送应答帧
            if (bufs.flag == true) {
                double error_case =  Math.random();
                //double error_case = 0.5;
                int seq = bufs.num;
                bufs.setNum(seq+1);
                //发送应答帧之后移动窗口
                bufs.move(bufs.win_2-bufs.win_1);  //TODO：问题三：要用synchronized锁住同时执行
                /*if (seq == 3) {
                    error_case = 0.9;
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
                //成功发送应答帧
                if (error_case < 0.8) {
                    boolean success = false;
                    bufs.setFlag(false);           //TODO：问题四：这个要在发送数据之前，不然会死锁
                    if (bufs.end+1 == bufs.win_2) {       //发送最后一个应答帧
                        tcpPacket(seq, (byte)17);
                        bufs.off = true;
                    }
                    else {
                        tcpPacket(seq, (byte)16);
                    }
                    byte[] tcp = util.tcpToBytes(tcpSeg, 20);
                    try {
                        textPrint.append("发送"+seq+"号应答帧成功"+" 期望收到的数据序号:"+bufs.win_2+"\n");
                      //  System.out.println("发送"+seq+"号应答帧成功"+" 期望收到的数据序号:"+bufs.win_2);
                        out.write(tcp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
					bufs.setFlag(false);
                    textPrint.append("发送"+seq+"号应答帧丢失"+"\n");
                 //   System.out.println("发送"+seq+"号应答帧丢失");
                }
            }
            if (bufs.off == true) {
                break;
            }
        }
    }
}

//从服务器接收数据
class C_receiver extends Thread {
    public Socket socket;
    public buffers bufs;     //缓冲区
    public DataInputStream in;
    public pseudoHeader pHeader = new pseudoHeader();    //TCP伪首部
    public TCPsegment tcpSeg;
    public bytesUtil util = new bytesUtil();
    public Random random = new Random();
    public byte[] datas = new byte[30];    //接收到的数据
    public int p_data = 0;
    public int recv_num = 0;
    public JTextArea textPrint;

    public C_receiver(Socket socket, buffers bufs, JTextArea textPrint) {
        this.socket = socket;
        this.bufs = bufs;
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取TCP伪首部
        InetAddress ip1 = socket.getLocalAddress(); //获取客户端ip
        InetAddress ip2 = socket.getInetAddress();  //获取服务器ip
        this.pHeader.setSaddr(ip2.getAddress());
        this.pHeader.setDaddr(ip1.getAddress());
        this.textPrint = textPrint;
    }

    //public boolean errorFlag1 = false;
    //public boolean errorFlag2 = false;
    //public boolean errorFlag3 = false;

    public void run() {
        while (true) {
            //应用程序接收数据
            while (bufs.p_buf != 0) {
                datas[p_data] = bufs.bufferList.get(0).data;
                if (p_data == bufs.end){
                    bufs.off2 = true;
                    break;
                }
                p_data++;
                bufs.bufferList.remove(0);
                bufs.p_buf--;
            }
            //接收数据
            int error_case =  random.nextInt(2);    //出错情况
           // int error_case = 0;
            try {
                byte[] recv = new byte[21];
                int len = in.read(recv);
                //收到了数据
                if (len != 0) {
                    pHeader.setTcpl(util.shortToByte2((short)len));
                    tcpSeg = util.byteToTcp(recv);

                    int seq = util.byte4ToInt(tcpSeg.seq);
                    buffer buf = new buffer();
                    buf.data = tcpSeg.data[0];
                    buf.num1 = seq;
                   // buf.num2 = util.byte4ToInt(tcpSeg.ack);
                 /*   if (seq == 3 && errorFlag1 == false) {
                        error_case = 1;
                        errorFlag1 = true;
                    }
                    if (seq == 4 && errorFlag2 == false) {
                        error_case = 2;
                        errorFlag2 = true;
                    }
                    if (seq == 16 && errorFlag3 == false) {
                        error_case = 1;
                        errorFlag3 = true;
                    }*/
                    //正常
                    if (error_case == 0) {
                        recv_num++;
                        //确认帧没错
                        if (tcpSeg.testCheckSum(pHeader, tcpSeg) == true) {
                            if (tcpSeg.offset[1] == 17) {    //FIN标志位为1，最后一帧
                                bufs.end = seq;
                            }
                            boolean isRepeat = false;
                            //重复收到的帧
                            if (seq < bufs.win_2) {
                                isRepeat = true;
                            }
                            //按序收到的帧
                            else if (seq == bufs.win_2) {
                                buf.flag1 = true;
                                buf.flag2 = true;
                                //缓存中没有未按序收到的数据
                                int x = bufs.p_buf+bufs.win_2-bufs.win_1;
                                if (bufs.bufferList.size() == x) {
                                    bufs.bufferList.add(buf);
                                    bufs.win_2++;
                                }
                                //缓存中有未按序收到的数据
                                else {
                                    x = bufs.p_buf+bufs.win_2-bufs.win_1;
                                    bufs.bufferList.set(x, buf);
                                    bufs.win_2++;
                                    for (int i = x+1; i < bufs.bufferList.size(); i++) {
                                        if (bufs.bufferList.get(i).flag2 == true) {
                                            bufs.win_2++;
                                        }
                                        else break;
                                    }
                                    textPrint.append("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat+"\n");
                                  //  System.out.println("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat);
                                    recv_num = 0;
                                    bufs.setFlag(true);
                                    continue;
                                }
                            }
                            //未按序收到的帧
                            else if (bufs.win_2 < seq && seq <= bufs.win_3) {
                                buf.flag1 = false;      //不按序
                                buf.flag2 = true;       //收到
                                //填充空的缓存，未收到
                                int x = seq-bufs.win_1+bufs.p_buf+1;
                                int y = bufs.bufferList.size();
                                if (x > y) {           //第一次收到不按序的帧
                                    boolean addFlag = false;
                                    buffer temp = new buffer();
                                    for (int i = y; i < x-1; i++) {
                                        bufs.bufferList.add(temp);
                                        addFlag = true;
                                    }
                                    bufs.bufferList.add(buf);
                                    if (addFlag == true) {
                                        textPrint.append("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat+"\n");
                                      //  System.out.println("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat);
                                        recv_num = 0;
                                        bufs.setFlag(true);
                                        continue;
                                    }
                                }
                                else {                 //已经有不按序的帧
                                    bufs.bufferList.set(x-1, buf);
                                }
                            }
                            //不在接收窗口内的帧
                            else {
                                continue;
                            }
                            textPrint.append("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat+"\n");
                          //  System.out.println("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"正常"+" 是否重复:"+isRepeat);
                        }
                        //设置每收到五个帧进行一次应答
                        if (recv_num%5 == 0) {
                            recv_num = 0;
                            bufs.setFlag(true);
                            continue;
                        }
                    }
                    //帧丢失
                    else if (error_case == 1) {
                        textPrint.append("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"帧丢失\n");
                       // System.out.println("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"帧丢失");
                        continue;
                    }
                    //帧出错，发送应答帧要求重发
                    else if (error_case == 2) {
                        textPrint.append("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"帧出错"+" 要求重发\n");
                    //    System.out.println("接收数据:"+buf.data+" 本次帧序号:"+buf.num1+" 出错情况:"+"帧出错"+" 要求重发");
                        recv_num = 0;
                        bufs.setFlag(true);
                        continue;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bufs.off2 == true) {
                textPrint.append("传输结束\n");
                System.out.println(Arrays.toString(datas));
                break;
            }
        }
    }
}