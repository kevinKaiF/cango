package com.bella.cango.server.startUp;

import com.bella.cango.service.CangoManagerService;
import com.bella.cango.utils.PropertiesFileLoader;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.JreMemoryLeakPreventionListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;

/**
 * embed tomcat
 *
 * @author kevin
 * @date 2016/8/22
 */
public class TomcatBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatBootstrap.class);

    public static void main(String[] args) throws LifecycleException, ServletException {
        setSystemProperties();
        int port = Integer.parseInt(System.getProperty("cango.port", "8080"));
        String contextPath = System.getProperty("cango.contextPath", "/cango");
        String docBase = System.getProperty("cango.docBase", getDefaultDocBase());
        LOGGER.info("server port : {}, context path : {}, docBase : {}", port, contextPath, docBase);
        final Tomcat tomcat = createTomcat(port, contextPath, docBase);
        tomcat.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    closeTomcatGracefully(tomcat);
                } catch (LifecycleException e) {
                    LOGGER.error("stop tomcat error : ", e);
                }
            }
        });
        tomcat.getServer().await();
    }

    private static void setSystemProperties() {
        System.setProperty("tomcat.util.scan.StandardJarScanFilter.jarsToSkip", "*.jar");
        String logHome = System.getProperty("cango.logHome");
        if (StringUtils.isEmpty(logHome)) {
            System.setProperty("cango.logHome", PropertiesFileLoader.DEFAULT_LOG_HOME);
        }
    }

    /**
     * Close tomcat gracefully.
     *
     * @param tomcat the tomcat
     * @throws LifecycleException the lifecycle exception
     */
    private static void closeTomcatGracefully(Tomcat tomcat) throws LifecycleException {
        // 平滑/优雅地关闭tomcat，防止kafka消息丢失
        Container[] containers = tomcat.getHost().findChildren();
        if (containers != null && containers.length > 0) {
            Container container = containers[0];
            if (container instanceof Context) {
                ServletContext servletContext = ((Context) container).getServletContext();
                WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
                webApplicationContext.getBean(CangoManagerService.class).shutdown();
            }
        }
        tomcat.stop();
    }

    private static Tomcat createTomcat(int port, String contextPath, String docBase) throws ServletException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(tmpDir);
        tomcat.getHost().setAppBase(tmpDir);
        tomcat.getHost().setAutoDeploy(false);
        tomcat.getEngine().setBackgroundProcessorDelay(-1);
        tomcat.setConnector(newNioConnector());
        tomcat.getConnector().setPort(port);
        tomcat.getService().addConnector(tomcat.getConnector());
        Context context = tomcat.addWebapp(contextPath, docBase);
        StandardServer server = (StandardServer) tomcat.getServer();
        server.addLifecycleListener(new AprLifecycleListener());
        server.addLifecycleListener(new JreMemoryLeakPreventionListener());
        return tomcat;
    }

    private static Connector newNioConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        return connector;
    }

    private static String getDefaultDocBase() {
        return new File(PropertiesFileLoader.PROJECT_DIR, "src/main/webapp").getPath();
    }
}
