package com.cisco.wccai.grpc.server;

import com.cisco.wcc.ccai.v1.HealthOuterClass;
import com.cisco.wcc.ccai.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckImpl extends HealthGrpc.HealthImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckImpl.class);

    @Override
    public void check(HealthOuterClass.HealthCheckRequest request, StreamObserver<HealthOuterClass.HealthCheckResponse> responseObserver) {
        LOGGER.info("Health check request received for service: {}", request.getService());
        if ( request.getService().equals("ping") || request.getService().equals("status") ) {

            HealthOuterClass.HealthCheckResponse hcresp_serving = HealthOuterClass.HealthCheckResponse.newBuilder().setStatus(HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING).build();
            HealthOuterClass.HealthCheckResponse hcresp_not_serving = HealthOuterClass.HealthCheckResponse.newBuilder().setStatus(HealthOuterClass.HealthCheckResponse.ServingStatus.NOT_SERVING).build();

            if ( GrpcServer.isServerIsRunning() ) {
                responseObserver.onNext(hcresp_serving);
                LOGGER.info("Health check response sent: SERVING");
            } else {
                responseObserver.onNext(hcresp_not_serving);
                LOGGER.info("Health check response sent: NOT_SERVING");
            }

            responseObserver.onCompleted();

        }
    }


    @Override
    public void watch(HealthOuterClass.HealthCheckRequest request, StreamObserver<HealthOuterClass.HealthCheckResponse> responseObserver) {
        super.watch(request, responseObserver);
    }
}
