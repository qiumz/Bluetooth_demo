package win.lioil.bluetooth.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import win.lioil.bluetooth.util.Util;

public class AcceptThread extends Thread {

    private String uuid;
    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isRunning = true;
    static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public AcceptThread(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;

        try {
            // 开启服务端蓝牙通道
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("blue_service",SPP_UUID);
        } catch (IOException e) {

        }

    }
    private List<BluetoothSocket> sockets=new ArrayList<>();
    private static final String TAG = "AcceptThread";
    @Override
    public void run() {

        BluetoothSocket socket = null;

        while (isRunning) {
            try {
                // 阻塞线程
                socket = serverSocket.accept();
                Log.d(TAG, "run: ================");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 处理客户端socket

            if (socket != null) {
                // 处理逻辑
                sockets.add(socket);
                // socket是服务端和客户端的通道

                // 获取客户端
                //socket.getInputStream()

                // 输出到客户端
                //socket.getOutputStream().write();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancle() {
        isRunning = false;
    }

    private static final int FLAG_MSG = 0;
    public void sendMsg(String msg) {
        for(BluetoothSocket socket:sockets){

            try {
                if(socket.isConnected()) {
                    DataOutputStream mOut = new DataOutputStream(socket.getOutputStream());
                    mOut.writeInt(FLAG_MSG); //消息标记
                    mOut.writeUTF(msg);
                    mOut.flush();
                }

            } catch (Throwable e) {
                try {
                    socket.close();
                    sockets.remove(socket);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }


    }
    private static final int FLAG_FILE = 1; //文件标记

    public void sendFile(final String filePath){
        Util.EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                for (BluetoothSocket socket : sockets) {
                    try {
                        if(socket.isConnected()) {
                            DataOutputStream mOut = new DataOutputStream(socket.getOutputStream());
                            FileInputStream in = new FileInputStream(filePath);
                            File file = new File(filePath);
                            mOut.writeInt(FLAG_FILE); //文件标记
                            mOut.writeUTF(file.getName()); //文件名
                            mOut.writeLong(file.length()); //文件长度
                            int r;
                            byte[] b = new byte[4 * 1024];
                            Log.d(TAG, "run: \"正在发送文件(\" + filePath + \"),请稍后...\"");
                            while ((r = in.read(b)) != -1)
                                mOut.write(b, 0, r);
                            mOut.flush();
                            Log.d(TAG, "run: \"文件发送完成.\"");
                        }
                    } catch (Throwable e) {
                        try {
                            socket.close();
                            sockets.remove(socket);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                }
            }
        });
    }
}
