package com.cisco.wccai.grpc.server;

import com.cisco.wccai.grpc.server.interceptors.AuthorizationServerInterceptor;
import com.cisco.wccai.grpc.server.interceptors.ServiceExceptionHandler;
import com.cisco.wccai.grpc.utils.LoadProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

        // TLS Configuration
        String tlsCertPath = System.getenv("TLS_CERT_PATH");
        String tlsKeyPath = System.getenv("TLS_KEY_PATH");
        
        // Fallback to config.properties if environment variables not set
        if (tlsCertPath == null || tlsCertPath.isEmpty()) {
            tlsCertPath = properties.getProperty("TLS_CERT_PATH");
        }
        if (tlsKeyPath == null || tlsKeyPath.isEmpty()) {
            tlsKeyPath = properties.getProperty("TLS_KEY_PATH");
        }

        Server server;
        
        // Check if TLS is configured
        if (tlsCertPath != null && !tlsCertPath.isEmpty() && 
            tlsKeyPath != null && !tlsKeyPath.isEmpty()) {
            
            LOGGER.info("TLS enabled - Certificate: {}, Key: {}", tlsCertPath, tlsKeyPath);
            
            File certChainFile = new File(tlsCertPath);
            File privateKeyFile = new File(tlsKeyPath);
            
            // Validate certificate files exist
            if (!certChainFile.exists()) {
                throw new IOException("TLS certificate file not found: " + tlsCertPath);
            }
            if (!privateKeyFile.exists()) {
                throw new IOException("TLS private key file not found: " + tlsKeyPath);
            }
            
            // Build SSL context
            SslContext sslContext = GrpcSslContexts.forServer(certChainFile, privateKeyFile)
                    .build();
            
            // Create secure server with TLS
            server = NettyServerBuilder.forPort(listeningPort)
                    .sslContext(sslContext)
                    .intercept(new ServiceExceptionHandler())
                    .addService(new VoiceVAImpl())
                    .addService(new ConversationAudioForkServiceImpl())
                    .addService(ProtoReflectionService.newInstance())
                    .addService(new com.cisco.wccai.grpc.server.HealthCheckImpl())
                    .intercept(new AuthorizationServerInterceptor())
                    .build()
                    .start();
            
            LOGGER.info("✓ Secure gRPC server started at port : {} with TLS/SSL encryption", listeningPort);
            
        } else {
            // TLS not configured - start without encryption (NOT RECOMMENDED FOR PRODUCTION)
            LOGGER.warn("⚠️  WARNING: TLS is NOT configured! Server will run WITHOUT encryption.");
            LOGGER.warn("⚠️  This is a SECURITY RISK and should ONLY be used for local development.");
            LOGGER.warn("⚠️  Set TLS_CERT_PATH and TLS_KEY_PATH environment variables or config.properties to enable TLS.");
            
            server = ServerBuilder.forPort(listeningPort)
                    .intercept(new ServiceExceptionHandler())
                    .addService(new VoiceVAImpl())
                    .addService(new ConversationAudioForkServiceImpl())
                    .addService(ProtoReflectionService.newInstance())
                    .addService(new com.cisco.wccai.grpc.server.HealthCheckImpl())
                    .intercept(new AuthorizationServerInterceptor())
                    .build()
                    .start();
            
            LOGGER.info("server started at port : {} (UNENCRYPTED)", listeningPort);
        }

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
