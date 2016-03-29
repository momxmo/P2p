package com.colorcloud.wifichat;

import android.content.Context;

import com.mo.p2p.tool.L;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * 这个类封装了NIO缓冲和NIO通道上的套接字。这都是abt NIO的风格。
 * SSLServerSocketChannel ServerSocketChannel SocketChannel,选择器,ByteBuffer,等等。
 * NIO缓冲区(ByteBuffer)在写作模式或在阅读模式。在阅读或写作之前需要翻转模式。
 * <p/>
 * 你知道当一个套接字通道断开当你读1或写例外。你需要应用水平ACK。
 */
public class ConnectionManager {

    private final String TAG = "PTP_ConnMan";

    private Context mContext;
    ConnectionService mService;
    WiFiDirectApp mApp;

    // 服务器知道所有客户。关键是ip addr,值是套接字通道。
    // 当远程客户端屏幕上,一个新的连接相同的ip addr建立。
    private Map<String, SocketChannel> mClientChannels = new HashMap<String, SocketChannel>();

    // global selector and channels
    private Selector mClientSelector = null;
    private Selector mServerSelector = null;
    private ServerSocketChannel mServerSocketChannel = null;
    private SocketChannel mClientSocketChannel = null;
    String mClientAddr = null;
    String mServerAddr = null;

    /**
     * constructor
     */
    public ConnectionManager(ConnectionService service) {
        mService = service;
        mApp = (WiFiDirectApp) mService.getApplication();
    }

    public void configIPV4() {
        // 默认情况下选择器试图IPv6堆栈。
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
    }

    /**
     * 创建一个服务器套接字通道听传入连接的端口。
     */
    public static ServerSocketChannel createServerSocketChannel(int port) throws IOException {
        // 创建一个非阻塞套接字通道
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ServerSocket serverSocket = ssChannel.socket();
        serverSocket.bind(new InetSocketAddress(port));  // bind to the port to listen.
        return ssChannel;
    }

    /**
     * 创建一个非阻塞套接字通道连接到指定的主机名和端口。
     * 连接()返回新通道之前。
     */
    public static SocketChannel createSocketChannel(String hostName, int port) throws IOException {
        // 创建一个非阻塞套接字通道
        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(false);

        // 向服务器发送一个连接请求,这方法是阻塞的
        sChannel.connect(new InetSocketAddress(hostName, port));
        return sChannel;
    }


    /**
     * 创建一个套接字通道和连接到主机。
     * 返回后,套接字通道保证连接。
     */
    public SocketChannel connectTo(String hostname, int port) throws Exception {
        SocketChannel sChannel = null;

        sChannel = createSocketChannel(hostname, port);  // connect to the remote host, port

        // 套接字可用之前,必须完成的连接。finishConnect()。
        while (!sChannel.finishConnect()) {
            // 阻止自旋锁
        }

        //套接字通道现在可以使用了
        return sChannel;
    }

    /**
     * 客户端,p2p连接可用之后,连接组所有者并选择监测插座。
     * 开始以一种异步阻塞器监测任务,无限循环
     */
    public int startClientSelector(String host) {
        closeServer();   // close linger server.

        if (mClientSocketChannel != null) {
            L.d(TAG, "startClientSelector : 客户端已经连接到服务器: " + mClientSocketChannel.socket().getLocalAddress().getHostAddress());
            return -1;
        }

        try {
            // 连接到服务器上启动客户端。
            SocketChannel sChannel = connectTo(host, 1080);

            mClientSelector = Selector.open();
            mClientSocketChannel = sChannel;
            mClientAddr = mClientSocketChannel.socket().getLocalAddress().getHostName();
            sChannel.register(mClientSelector, SelectionKey.OP_READ);
            mApp.setMyAddr(mClientAddr);
            mApp.clearMessages();
            L.d(TAG, "startClientSelector : started: " + mClientSocketChannel.socket().getLocalAddress().getHostAddress());

            // 开始选择器监控、阻塞
            new SelectorAsyncTask(mService, mClientSelector).execute();
            return 0;

        } catch (Exception e) {
            L.e(TAG, "startClientSelector : exception: " + e.toString());
            mClientSelector = null;
            mClientSocketChannel = null;
            mApp.setMyAddr(null);

            return -1;
        }
    }

    /**
     * 创建一个选择器来管理一个服务器套接字通道
     * 注册过程收益率对象称为选择关键标识选择器/套接字通道
     */
    public int startServerSelector() {
        closeClient();   // close linger client, if exists.

        try {
            // 创建服务器套接字并注册到选择器听OP_ACCEPT事件
            ServerSocketChannel sServerChannel = createServerSocketChannel(1080); // BindException如果已绑定。
            mServerSocketChannel = sServerChannel;
            mServerAddr = mServerSocketChannel.socket().getInetAddress().getHostAddress();
            if ("0.0.0.0".equals(mServerAddr)) {
                mServerAddr = "Master";
            }
            ((WiFiDirectApp) mService.getApplication()).setMyAddr(mServerAddr);

            mServerSelector = Selector.open();
            SelectionKey acceptKey = sServerChannel.register(mServerSelector, SelectionKey.OP_ACCEPT);
            acceptKey.attach("accept_channel");
            mApp.mIsServer = true;

            //SocketChannel sChannel = createSocketChannel("hostname.com", 80);
            //sChannel.register(selector, SelectionKey.OP_CONNECT);  // listen to connect event.
            L.d(TAG, "startServerSelector : started: " + sServerChannel.socket().getLocalSocketAddress().toString());

            new SelectorAsyncTask(mService, mServerSelector).execute();
            return 0;

        } catch (Exception e) {
            L.e(TAG, "startServerSelector : exception: " + e.toString());
            return -1;
        }
    }

    /**
     * 处理选择错误,重启
     */
    public void onSelectorError() {
        L.e(TAG, " onSelectorError : do nothing for now.");
        // new SelectorAsyncTask(mService, mSelector).execute();
    }

    public void closeServer() {
        if (mServerSocketChannel != null) {
            try {
                mServerSocketChannel.close();
                mServerSelector.close();
            } catch (Exception e) {

            } finally {
                mApp.mIsServer = false;
                mServerSocketChannel = null;
                mServerSelector = null;
                mServerAddr = null;
                mClientChannels.clear();
            }
        }
    }

    public void closeClient() {
        if (mClientSocketChannel != null) {
            try {
                mClientSocketChannel.close();
                mClientSelector.close();
            } catch (Exception e) {

            } finally {
                mClientSocketChannel = null;
                mClientSelector = null;
                mClientAddr = null;
            }
        }
    }

    /**
     * read out -1, connection broken, remove it from clients collection
     * 宣读1、连接坏了,将它从客户收集
     */
    public void onBrokenConn(SocketChannel schannel) {
        try {
            String peeraddr = schannel.socket().getInetAddress().getHostAddress();
            if (mApp.mIsServer) {
                mClientChannels.remove(peeraddr);
                L.d(TAG, "onBrokenConn : client down: " + peeraddr);
            } else {
                L.d(TAG, "onBrokenConn : set null client channel after server down: " + peeraddr);
                closeClient();
            }
            schannel.close();
        } catch (Exception e) {
            L.e(TAG, "onBrokenConn: close channel: " + e.toString());
        }
    }

    /**
     * 服务器处理新客户进来。
     */
    public void onNewClient(SocketChannel schannel) {
        String ipaddr = schannel.socket().getInetAddress().getHostAddress();
        L.d(TAG, "onNewClient : server added remote client: " + ipaddr);
        mClientChannels.put(ipaddr, schannel);
    }

    /**
     * 客户的成功连接到服务器
     */
    public void onFinishConnect(SocketChannel schannel) {
        String clientaddr = schannel.socket().getLocalAddress().getHostAddress();
        String serveraddr = schannel.socket().getInetAddress().getHostAddress();
        L.d(TAG, "onFinishConnect : client connect to server succeed : " + clientaddr + " -> " + serveraddr);
        mClientSocketChannel = schannel;
        mClientAddr = clientaddr;
        ((WiFiDirectApp) mService.getApplication()).setMyAddr(mClientAddr);
    }

    /**
     * 客户端发送数据到服务器,服务器发送 所有的客户。
     */
    public void onDataIn(SocketChannel schannel, String data) {
        L.d(TAG, "connection onDataIn : " + data);
        if (mApp.mIsServer) {  // push all _other_ clients if the device is the server
            pubDataToAllClients(data, schannel);
        }
    }

    /**
     * 字节缓冲区写入套接字通道。
     */
    private int writeData(SocketChannel sChannel, String jsonString) {
        byte[] buf = jsonString.getBytes();
        ByteBuffer bytebuf = ByteBuffer.wrap(buf);  // 将缓冲区的字节缓冲区
        int nwritten = 0;
        try {
            //bytebuf.flip();  // 没有抛在创建包。
            L.d(TAG, "writeData: start:limit = " + bytebuf.position() + " : " + bytebuf.limit());
            nwritten = sChannel.write(bytebuf);
        } catch (Exception e) {
            // 连接可能已被关闭
            L.e(TAG, "writeData: exception : " + e.toString());
            onBrokenConn(sChannel);
        }
        L.d(TAG, "writeData: content: " + new String(buf) + "  : len: " + nwritten);
        return nwritten;
    }

    /**
     * 所有连接的客户机服务器发布数据
     */
    private void pubDataToAllClients(String msg, SocketChannel incomingChannel) {
        L.d(TAG, "pubDataToAllClients : isServer ? " + mApp.mIsServer + " msg: " + msg);
        if (!mApp.mIsServer) {
            return;
        }

        for (SocketChannel s : mClientChannels.values()) {
            if (s != incomingChannel) {
                String peeraddr = s.socket().getInetAddress().getHostAddress();
                L.d(TAG, "pubDataToAllClients : Server pub data to:  " + peeraddr);
                writeData(s, msg);
            }
        }
    }

    /**
     * 设备要推出数据。
     *如果设备是客户端,服务器的唯一通道。
     *如果设备服务器,它只是现在酒吧所有
     */
    public int pushOutData(String jsonString) {
        if (!mApp.mIsServer) {   // device is client, can only send to server
            sendDataToServer(jsonString);
        } else {
            // 服务器发送给所有客户,消息已经附加了发送方addr内部发送按钮的处理程序。
            pubDataToAllClients(jsonString, null);
        }
        return 0;
    }

    /**
     * 每当客户端写入服务器,携带“client_addr:msg”的格式
     */
    private int sendDataToServer(String jsonString) {
        if (mClientSocketChannel == null) {
            L.d(TAG, "sendDataToServer: channel not connected ! waiting...");
            return 0;
        }
        L.d(TAG, "sendDataToServer: " + mClientAddr + " -> " + mClientSocketChannel.socket().getInetAddress().getHostAddress() + " : " + jsonString);
        return writeData(mClientSocketChannel, jsonString);
    }
}
