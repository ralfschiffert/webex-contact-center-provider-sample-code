package com.cisco.wccai.grpc.server;

import com.cisco.wccai.grpc.server.interceptors.AuthorizationServerInterceptor;
import com.cisco.wccai.grpc.server.interceptors.ServiceExceptionHandler;
import com.cisco.wccai.grpc.utils.LoadProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * The type Grpc server.
 */
public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);
    private static final int PORT = 8086;
    private static final Properties properties = LoadProperties.loadProperties();
    @Getter @Setter
    private static boolean serverIsRunning;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     */
    public static void main(String [] args) throws IOException, InterruptedException {

        String port = System.getenv("PORT");
        int listeningPort;

        if (port == null || port.isEmpty()) {
            // Try to read from config.properties if environment variable not set
            String configPort = properties.getProperty("PORT");
            if (configPort != null && !configPort.trim().isEmpty()) {
                listeningPort = Integer.parseInt(configPort.trim());
            } else {
                listeningPort = PORT;
            }
        } else {
            listeningPort = Integer.parseInt(port);
        }

        LOGGER.info("the environment variable port is : {}", port);

       Server server = ServerBuilder.forPort(listeningPort)
                                    .intercept(new ServiceExceptionHandler())
                                    .addService(new VoiceVAImpl())
                                    .addService(new ConversationAudioForkServiceImpl())
                                    .addService(ProtoReflectionService.newInstance())
                                    .addService(new com.cisco.wccai.grpc.server.HealthCheckImpl())
                                    .intercept(new AuthorizationServerInterceptor())
                                    .build()
                                    .start();

        LOGGER.info("server started at port : {}", listeningPort );

        serverIsRunning = true;

        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            LOGGER.info("Received Shutdown Request");
            server.shutdown();
            LOGGER.info("Successfully Stopped, Shutting down the server");
            serverIsRunning = false;
        }));


        // await for Termination of Program
        server.awaitTermination();
    }
}
