package top.modelx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * ocpp 1。6
 * @author hua
 */
@SpringBootApplication
public class OcppApplication {

    private static final Logger logger = LogManager.getLogger(OcppApplication.class);

    public static ConfigurableApplicationContext applicationContext;

    @Value("${dev.port:9016}")
    private int port;

    public static void main(String[] args) {
        Long startTime = System.currentTimeMillis();
        applicationContext = SpringApplication.run(OcppApplication.class, args);
        Long endTime = System.currentTimeMillis();

        logger.info("应用启动耗时: {} 秒", (endTime - startTime) / 1000.0);
    }

    @PostConstruct
    public void startTcpServer() {
        logger.info("--- config port: {} --- ", port);

        // 打印 JVM 信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        logger.info("JVM Info: Name={}, Spec Name={}, Spec Vendor={}, Spec Version={}, VM Name={}, VM Vendor={}, VM Version={}",
                runtimeMXBean.getName(),
                runtimeMXBean.getSpecName(),
                runtimeMXBean.getSpecVendor(),
                runtimeMXBean.getSpecVersion(),
                runtimeMXBean.getVmName(),
                runtimeMXBean.getVmVendor(),
                runtimeMXBean.getVmVersion());

        // 启动 TCP 服务
//        TcpServer tcpServer = TcpServer.getInstance(port);
//        try {
//            tcpServer.run();
//            PackageHandlerImpl packageHandler = PackageHandlerImpl.getInstance();
//            packageHandler.addHandle(new PackageHandlerByMQ());
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt(); // 恢复中断状态
//            logger.error("设备通讯链错误", e);
//            throw new RuntimeException(e);
//        }
    }
}


