package top.modelx.service.tcpServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP服务端 用于支持各种tcp协议设备接入
 * @author hua
 */
public class TcpServer {

    private Logger log = LoggerFactory.getLogger(getClass());
    //端口号
    private int port = 9015;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private TcpServer(int port) {
        this.port = port;
    }

    private static class SingletonHolder {
        private static final TcpServer INSTANCE = new TcpServer(9015);
    }

    public static TcpServer getInstance(int port) {
        // 若需要每次传port生效，则不能复用单例；否则应移除参数或忽略port变化
        // 此处假设port是首次设置有效，后续调用忽略port参数
        return SingletonHolder.INSTANCE;
    }

    public void run() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(20);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new AION_DecoderV1(), new TcpHandler());
            }
        });

        bootstrap.bind(port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("bind success in port: {}", port);
            } else {
                log.error("Failed to bind on port: " + port, future.cause());
            }
        });

        log.info("server started!");
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("TcpServer shutdown gracefully.");
    }
}

