package com.cisco.wccai.grpc.server;

import com.cisco.wccai.grpc.server.interceptors.AuthorizationServerInterceptor;
import com.cisco.wccai.grpc.server.interceptors.ServiceExceptionHandler;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The type Grpc server.
 */
public class GrpcServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServer.class);
    private static final int PORT = 8086;
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

       Server server = ServerBuilder.forPort(PORT)
                                    .intercept(new ServiceExceptionHandler())
                                    .addService(new VoiceVAImpl())
                                    .addService(new ConversationAudioForkImpl())
                                    .addService(ProtoReflectionService.newInstance())
                                    .addService(new com.cisco.wccai.grpc.server.HealthCheckImpl())
                                    .intercept(new AuthorizationServerInterceptor())
                                    .build()
                                    .start();

        LOGGER.info("server started at port : {}", PORT );

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
