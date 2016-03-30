package com.colorcloud.wifichat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mo.p2p.tool.L;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import static com.colorcloud.wifichat.Constants.MSG_BROKEN_CONN;
import static com.colorcloud.wifichat.Constants.MSG_FINISH_CONNECT;
import static com.colorcloud.wifichat.Constants.MSG_NEW_CLIENT;
import static com.colorcloud.wifichat.Constants.MSG_PULLIN_DATA;
import static com.colorcloud.wifichat.Constants.MSG_SELECT_ERROR;


/**
 * 选择器只有显示OP_CONNECT和OP_READ。不监控OP_WRITE通道都是可写的。
 * 在事件,接受一个连接,或者从通道读取数据。
 * 写入通道连接服务主线程中完成。
 */

public class SelectorAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "PTP_SEL";

    private ConnectionService mConnService;
    private Selector mSelector;

    public SelectorAsyncTask(ConnectionService connservice, Selector selector) {
        mConnService = connservice;
        mSelector = selector;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        select();
        return null;
    }

    private void select() {
        // 轮询等待消息
        while (true) {
            try {
                L.d(TAG, "select : selector monitoring: ");
                mSelector.select();   // 阻塞等待事件

                L.d(TAG, "select : selector evented out: ");
                // 得到的列表选择键,等待事件和处理它。
                Iterator<SelectionKey> keys = mSelector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    // 选择键,从列表中删除它表明它正在处理
                    SelectionKey selKey = keys.next();
                    keys.remove();
                    L.d(TAG, "select : selectionkey: " + selKey.attachment());

                    try {
                        processSelectionKey(mSelector, selKey);  // 处理选着的key
                    } catch (IOException e) {
                        selKey.cancel();
                        L.e(TAG, "select : io exception in processing selector event: " + e.toString());
                    }
                }
            } catch (Exception e) {  // catch all exception in select() and the following ops in mSelector.
                L.e(TAG, "Exception in selector: " + e.toString());
                notifyConnectionService(MSG_SELECT_ERROR, null, null);
                break;
            }
        }
    }

    /**
     * process the event popped to the selector
     */
    public void processSelectionKey(Selector selector, SelectionKey selKey) throws IOException {
        if (selKey.isValid() && selKey.isAcceptable()) {  // 有一个连接到服务器套接字通道
            ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
            SocketChannel sChannel = ssChannel.accept();  // 接受连接,得到一个新的套接字通道。
            sChannel.configureBlocking(false);

            // 让选择器监控读/写接受连接。
            SelectionKey socketKey = sChannel.register(selector, SelectionKey.OP_READ);
            socketKey.attach("accepted_client " + sChannel.socket().getInetAddress().getHostAddress());
            L.d(TAG, "processSelectionKey : accepted a client connection: " + sChannel.socket().getInetAddress().getHostAddress());
            notifyConnectionService(MSG_NEW_CLIENT, sChannel, null);

        } else if (selKey.isValid() && selKey.isConnectable()) {   // 客户端连接到服务器响应。
            SocketChannel sChannel = (SocketChannel) selKey.channel();

            boolean success = sChannel.finishConnect();
            if (!success) {
                // 一个错误发生,注销通道。
                selKey.cancel();
                L.e(TAG, " processSelectionKey : 连接失败 !");
            }
            L.d(TAG, "processSelectionKey : 客户端远程连接成功: ");
            notifyConnectionService(MSG_FINISH_CONNECT, sChannel, null);

        } else if (selKey.isValid() && selKey.isReadable()) {    //读取数据
            // 读取字节通道
            SocketChannel sChannel = (SocketChannel) selKey.channel();
            L.d(TAG, "processSelectionKey : 远程客户端读数据: " + selKey.attachment());
            doReadable(sChannel);

        } else if (selKey.isValid() && selKey.isWritable()) {
            // 没有选择可写的…无尽的循环
            SocketChannel sChannel = (SocketChannel) selKey.channel();
        }
    }

    /**
     * 处理的可读的事件选择器
     */
    public void doReadable(SocketChannel schannel) {
        String data = readData(schannel);
        if (data != null) {
            Bundle b = new Bundle();
            b.putString("DATA", data);
            notifyConnectionService(MSG_PULLIN_DATA, schannel, b);    //通知接收到数据
        }
    }

    /**
     * 读取数据
     */
    public String readData(SocketChannel sChannel) {
        ByteBuffer buf = ByteBuffer.allocate(1024 * 4);   // let's cap json string to 4k for now.
        byte[] bytes = null;
        String jsonString = null;

        try {
            buf.clear();  // Clear the buffer and read bytes from socket
            int numBytesRead = sChannel.read(buf);
            if (numBytesRead == -1) {
                // read -1 means socket channel is broken. remove it from the selector
                L.e(TAG, "readData : channel closed due to read -1: ");
                sChannel.close();  // close the channel.
                notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
                // sChannel.close();
            } else {
                L.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
                buf.flip();  // make buffer ready for read by flipping it into read mode.
                L.d(TAG, "readData: bufpos: limit : " + buf.position() + ":" + buf.limit() + " : " + buf.capacity());
                bytes = new byte[buf.limit()];  // use bytes.length will cause underflow exception.
                buf.get(bytes);
                // while ( buf.hasRemaining() ) buf.get();
                jsonString = new String(bytes);  // convert byte[] back to string.
            }
        } catch (Exception e) {
            L.e(TAG, "readData : exception: " + e.toString());
            notifyConnectionService(MSG_BROKEN_CONN, sChannel, null);
        }

        L.d(TAG, "readData: content: " + jsonString);
        return jsonString;
    }

    /**
     * notify connection manager event
     */
    private void notifyConnectionService(int what, Object obj, Bundle data) {
        Handler hdl = mConnService.getHandler();
        Message msg = hdl.obtainMessage();
        msg.what = what;

        if (obj != null) {
            msg.obj = obj;
        }
        if (data != null) {
            msg.setData(data);
        }
        hdl.sendMessage(msg);
    }

}
