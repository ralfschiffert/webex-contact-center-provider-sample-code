# Getting Started with Media Forking for Webex Contact Center

**Feature:** Real-Time Media Streaming for Customer-Agent Conversations  
**Last Updated:** January 7, 2026  
**Audience:** Developers and Partners

---

## Table of Contents

1. [Overview](#overview)
2. [What is Media Forking?](#what-is-media-forking)
3. [Use Cases](#use-cases)
4. [Architecture](#architecture)
5. [Prerequisites](#prerequisites)
6. [Quick Start: Running the Simulator](#quick-start-running-the-simulator)
7. [Step-by-Step Setup Guide](#step-by-step-setup-guide)
8. [Testing Your Integration](#testing-your-integration)
9. [Troubleshooting](#troubleshooting)
10. [Next Steps](#next-steps)
11. [Support & Resources](#support--resources)

---

## Overview

Media Forking is a premium feature of Webex Contact Center (WXCC) that enables third-party partners and developers to receive real-time audio streams from customer-agent conversations. This powerful capability allows you to build AI-driven solutions for sentiment analysis, real-time transcription, agent assistance, quality monitoring, and more.

### Key Benefits

- **Real-Time Access:** Receive audio streams as the conversation happens
- **Dual-Channel Audio:** Separate streams for customer and agent audio
- **Flexible Use Cases:** Recording, transcription, sentiment analysis, agent coaching, compliance monitoring
- **Secure Integration:** gRPC-based protocol with authentication and encryption
- **Scalable:** Designed to handle high-volume contact center operations

---

## What is Media Forking?

Media Forking allows you to "tap into" live customer-agent voice conversations in a Webex Contact Center deployment. Here's how it works:

### The Customer Journey

1. **Inbound Call:** A customer calls your contact center
2. **IVR Interaction:** The call is routed to an IVR (Interactive Voice Response) system for self-service
3. **Agent Escalation:** If the IVR cannot resolve the issue, the customer requests to speak with an agent
4. **Media Forking Trigger:** When the customer connects to an agent, the media forking activity in the flow is triggered
5. **Real-Time Streaming:** Audio from both the customer and agent is streamed to your registered data source endpoint

### Important Notes

- Media forking is **only triggered when a customer connects to a live agent**
- It does **not** capture IVR interactions or pre-agent audio
- This is a **paid feature** that must be enabled through Cisco's CCW (Commerce Workspace) ordering tool
- Audio is streamed in real-time with minimal latency

### What You Receive

Media forking provides two separate audio channels:

- **Channel 1:** Customer audio (what the customer is saying)
- **Channel 2:** Agent audio (what the agent is saying, or what the customer hears)

This dual-channel approach enables sophisticated analysis and processing of the conversation.

---

## Use Cases

Media Forking enables a wide range of AI-powered contact center solutions:

### 1. Real-Time Transcription
Convert speech to text in real-time, providing agents with a written record of the conversation as it happens.

### 2. Sentiment Analysis
Analyze customer emotions and sentiment during the call to help agents adjust their approach or escalate issues.

### 3. Agent Assistance & Coaching
- Provide real-time suggestions to agents based on conversation context
- Identify knowledge base articles or scripts that can help resolve the issue
- Detect compliance violations or script deviations

### 4. Quality Monitoring
- Automatically flag calls for quality review based on keywords, sentiment, or compliance criteria
- Generate quality scores in real-time

### 5. Conversation Intelligence
- Extract key insights from conversations (customer intent, pain points, product mentions)
- Build analytics dashboards with conversation data
- Identify training opportunities for agents

### 6. Compliance & Recording
- Record conversations for regulatory compliance
- Detect sensitive information (PCI, PII) and trigger security protocols
- Maintain audit trails

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    CUSTOMER CALLS IN                             │
│                           ↓                                      │
│                    IVR / Self-Service                            │
│                           ↓                                      │
│              Customer Requests Agent                             │
│                           ↓                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              WEBEX CONTACT CENTER (WXCC)                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Flow Designer: Media Forking Activity Triggered       │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓                                      │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  CCAI Orchestrator: Fetches Configuration             │    │
│  │  - Media Sink Endpoint                                 │    │
│  │  - Authentication Credentials                          │    │
│  │  - Feature Flags                                       │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓                                      │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Establishes gRPC Connection to Partner Endpoint      │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    gRPC Bidirectional Stream
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              YOUR MEDIA SINK (gRPC Server)                       │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Receives Real-Time Audio Streams                      │    │
│  │  - Channel 1: Customer Audio                           │    │
│  │  - Channel 2: Agent Audio                              │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓                                      │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Your AI/Processing Pipeline                           │    │
│  │  - Speech-to-Text                                      │    │
│  │  - Sentiment Analysis                                  │    │
│  │  - Real-Time Insights                                  │    │
│  │  - Recording/Storage                                   │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Component Breakdown

#### 1. Service App (Partner Setup)
- Partners create a Service App in the Webex Developer Portal
- Service App includes:
  - **Data source schema:** Media forking schema (wraps the protobuf)
  - **Data exchange domain:** Your gRPC endpoint URL
  - **Required scopes:** `spark-admin:dataSource_read` and `spark-admin:dataSource_write`
- Service App is submitted for customer authorization

#### 2. Admin Authorization (Customer Organization)
- Customer admin authorizes the Service App in Control Hub
- Admin reviews:
  - Partner and developer information
  - Data destination URL (where media will be sent)
  - Schema (what data is being sent)
- Upon authorization:
  - Service App becomes selectable in Flow Designer for media forking
  - Partner can retrieve organization-specific access and refresh tokens

#### 3. Data Source Registration (Partner Action)
- Partner uses the organization-specific access token to register a **data source**
  - **Note:** For media forking, this is actually a data sink (destination for media). The "data source" API is generic and used for multiple use cases.
- Data source registration includes:
  - **URL:** Where media will be sent (must match or be subdomain/path of Service App URL)
  - **Authentication info:** How JWS token should be constructed (nonce, expiration, public key)
- Each customer organization has its own data source for independent management

#### 4. Flow Designer (WXCC Admin)
- Contact center administrators create call flows using the Flow Designer
- A **Media Forking Activity** is added to the flow at the point where agent connection occurs
- Admin selects the **authorized Service App** (not a CCAI Configuration)

#### 5. CCAI Orchestrator (Runtime - WXCC Platform)
- When media forking is triggered during a call:
  1. Orchestrator receives the media stream
  2. Uses the organization ID to lookup the registered data source URL
  3. Retrieves authentication details for JWS token construction
  4. Establishes gRPC connection to the partner's endpoint
  5. Authenticates using the JWS token
  6. Streams audio in real-time to the partner

#### 6. Your Media Sink (Partner gRPC Server)
- Validates the JWS token
- Receives bidirectional streaming audio
- Processes audio according to your use case
- **Token Management:** 
  - **OAuth Tokens (Access/Refresh):** Standard Webex expiration (14 days for access, 90 days for refresh). Refresh as needed for data source management.
  - **JWS Token (Runtime):** Update data source with new nonce and expiration between every 1 hour (minimum) and 24 hours (maximum) so WXCC can generate fresh JWS tokens

---

## Prerequisites

Before you begin, ensure you have:

### 1. Licensing & Provisioning

**For Customer Organizations:**
- [ ] **Media Forking Subscription:** Purchased through a Cisco partner
  - **How it works:** Customers work with their Cisco partner to order media forking subscriptions
  - **Subscription Model:** Licensed in bundles of 2,000 minutes per month
  - **Usage:** Minutes are counted from when media forking starts until the end of the call
  - **Note:** The partner providing the media forking service can assist with ordering, or customers can work with their existing Cisco partner
- [ ] **License Applied:** Feature enabled for your WXCC organization by Cisco
- [ ] **Admin Access:** Full administrator access to Webex Control Hub

**For Partners/Developers:**
- [ ] **Development/Testing:** Use a Webex sandbox or your own licensed WXCC organization for development and testing
- [ ] **Customer Licensing:** Your customers will need their own media forking subscriptions ordered through a Cisco partner

### 2. Technical Requirements
- [ ] **gRPC Server:** Ability to host a gRPC server accessible from the internet
- [ ] **TLS/SSL Certificate:** Valid certificate for secure gRPC connections
- [ ] **Public Endpoint:** Your gRPC server must be reachable from WXCC (public IP or domain)
- [ ] **Firewall Rules:** Appropriate firewall rules to allow incoming gRPC connections

### 3. Development Environment
- [ ] **Java 17 or higher** (for running the simulator)
  - **Recommended:** Java 17 for best compatibility
  - The project is compiled to Java 17 bytecode (maven.compiler.source/target=17)
  - Lombok 1.18.32 and other dependencies support Java 17-22
- [ ] **Maven** (for building the simulator)
- [ ] **Git** (for cloning the sample code repository)
- [ ] **Docker** (optional, for containerized deployment)

### 4. Audio Processing Capabilities
- **Supported Audio Format:** WAV
- **Sampling Rate:** 8kHz or 16kHz
- **Encoding:** Linear16 or μ-law (ulaw)
- **Channels:** Single channel per stream (customer and agent are separate streams)

### 5. Knowledge Prerequisites
- Basic understanding of gRPC and Protocol Buffers
- Familiarity with Webex Contact Center concepts (flows, entry points, routing)
- Experience with audio processing (if building custom solutions)

---

## Quick Start: Running the Simulator

The fastest way to validate your setup and understand media forking is to run the provided simulator. This section gets you up and running in under 30 minutes.

### What is the Simulator?

The **Dialog Connector Simulator** is a sample gRPC server that:
- Implements the media forking protocol
- Receives audio streams from WXCC
- Logs received audio for inspection
- Responds with simple acknowledgments
- Helps you validate your configuration before building your production solution

### Step 1: Clone the Sample Code

```bash
# Clone the repository
git clone https://github.com/CiscoDevNet/webex-contact-center-provider-sample-code.git

# Navigate to the simulator directory
cd webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator
```

### Step 2: Understand the Project Structure

Before building, familiarize yourself with the project layout:

```
dialog-connector-simulator/
├── pom.xml                          # Maven build configuration
├── Dockerfile                       # Container image definition
├── src/
│   └── main/
│       ├── java/com/cisco/wccai/
│       │   └── grpc/
│       │       ├── server/          # gRPC server implementation
│       │       │   ├── GrpcServer.java                    # Main server entry point
│       │       │   ├── ConversationAudioForkServiceImpl.java  # Media forking service
│       │       │   ├── HealthServiceImpl.java             # Health check service
│       │       │   └── interceptors/                      # Authentication interceptors
│       │       ├── client/          # gRPC client (for testing)
│       │       ├── config/          # Configuration management
│       │       └── model/           # Data models
│       ├── proto/com/cisco/wcc/ccai/v1/
│       │   ├── ccai-api.proto                   # Main API definitions
│       │   ├── conversationaudioforking.proto   # Media forking protocol
│       │   └── common/                          # Shared protocol definitions
│       │       ├── health.proto
│       │       ├── media_service_common.proto
│       │       └── virtualagent.proto
│       └── resources/               # Configuration files
├── test-health.sh                   # Health check test script
└── .idea/                           # IntelliJ IDEA project files
```

**Key Components:**
- **GrpcServer.java:** Main entry point that starts the gRPC server on port 8086
- **ConversationAudioForkServiceImpl.java:** Implements media forking protocol, receives audio streams
- **conversationaudioforking.proto:** Protocol buffer definition for media forking API
- **Dockerfile:** Builds container image using Google Cloud SDK base image

### Step 3: Build the Simulator

```bash
# Compile Protocol Buffer definitions
mvn clean compile

# Build the application
mvn clean install
```

**Expected Output:**
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.471 s
[INFO] Finished at: 2026-01-29T19:55:14-08:00
[INFO] ------------------------------------------------------------------------
```

**What happens during build:**
1. Protocol Buffer files (`.proto`) are compiled to Java classes
2. Generated classes are placed in `target/generated-sources/protobuf/`
3. Maven Shade plugin creates an "all-in-one" JAR: `dialog-connector-simulator-1.0.0-SNAPSHOT-allinone.jar`
4. This JAR contains all dependencies and can be run standalone

### Step 4: Configure the Simulator

The simulator uses `config.properties` located at `src/main/resources/config.properties`. Review and modify as needed:

```properties
# Endpoint to connect
API_URL=localhost

# Port (TLS - 443, NonTLS - 31400, Google Cloud - 8086)
PORT=8086

# Language code
LANGUAGE_CODE=en-US

# Flag to set for TLS
USE_TLS=false

# Audio encoding supported types - LINEAR16, MULAW
AUDIO_ENCODING_TYPE=MULAW

# Sample rate
SAMPLE_RATE_HERTZ=8000

# Buffer Size
BUFFER_SIZE=8192

# Org Id
ORG_ID=org_01

# Prompt duration
PROMPT_DURATION_MS=10000

# Audio duration
AUDIO_DURATION_MS=60000
```

**Key Configuration Options:**
- **PORT:** 8086 (default for both local and Google Cloud)
  - Can be configured via `config.properties` file
  - Can be overridden with PORT environment variable
  - **Configuration Priority:** Environment variable > config.properties > hardcoded default (8086)
  - **Important:** After changing PORT in config.properties, you must rebuild: `mvn clean install`
- **USE_TLS:** Set to `true` for production, `false` for local testing
- **AUDIO_ENCODING_TYPE:** LINEAR16 or MULAW (must match incoming stream)
- **SAMPLE_RATE_HERTZ:** 8000 or 16000 Hz

### Step 5: Run the Simulator Locally

```bash
# Run the gRPC server
mvn exec:java -Dexec.mainClass="com.cisco.wccai.grpc.server.GrpcServer"
```

**Expected Output:**
```
INFO: server started at port : 8086
INFO: Initializing the context
```

**Test the Server Locally:**

Once the server is running, open a new terminal and verify it's responding:

**Note:** If you don't have `grpcurl` installed, download it from [https://github.com/fullstorydev/grpcurl/releases](https://github.com/fullstorydev/grpcurl/releases) or install via package manager:
```bash
# macOS
brew install grpcurl

# Linux
apt-get install grpcurl  # or yum install grpcurl
```

```bash
# List available services
grpcurl -plaintext :8086 list

# Test the health endpoint
grpcurl -plaintext -d '{}' :8086 com.cisco.wcc.ccai.v1.Health/Check
```

**Expected Response:**
```json
{
  "status": "SERVING"
}
```

**Note:** The health check endpoint does not require authentication for local testing. It's designed to be accessible for monitoring and verification purposes.

**Stopping the Server:**

When you're done testing, stop the server by pressing `Ctrl+C` in the terminal where it's running.

If you need to kill a server that's running in the background:

```bash
# Find the process using port 8086
lsof -i :8086

# Kill the process (replace PID with the actual process ID from the output)
kill <PID>
```

### Step 6: Deploy to a Public Endpoint

For WXCC to connect to your simulator, it must be publicly accessible. Before deploying to the cloud, it's recommended to test your Docker container locally.

#### Step 6a: Test Docker Container Locally (Recommended)

Before deploying to Google Cloud or other platforms, verify your Docker image works correctly on your local machine.

**1. Build the Docker Image Locally:**

The project includes a **multi-stage Dockerfile** that compiles inside the container. However, for faster local development, you can also build the JAR first in IntelliJ.

**Option A: Quick Development Build (Recommended for Local Testing)**

If you're actively developing in IntelliJ, build the JAR locally first for faster Docker builds:

```bash
# Navigate to the project directory
cd /Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator

# Build the JAR in IntelliJ or via Maven
mvn clean install

# Create a simple Dockerfile.dev (if not exists)
cat > Dockerfile.dev << 'EOF'
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*-allinone.jar /app/app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
EOF

# Build Docker image using the dev Dockerfile
docker build -f Dockerfile.dev -t media-forking-simulator:local .
```

**Pros:** Fast rebuilds (~5 seconds), easier debugging, leverages your IntelliJ build

**Option B: Multi-Stage Build (Production-Ready)**

Use the standard Dockerfile that compiles everything inside the container:

```bash
# Navigate to the project directory
cd /Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator

# Build Docker image (compiles inside container)
docker build -t media-forking-simulator:local .
```

**What happens during multi-stage build:**
1. **Build Stage:** Uses `eclipse-temurin:17-jdk` image
   - Installs Maven
   - Downloads dependencies
   - Compiles the project (`mvn clean package`)
   - Creates the all-in-one JAR
2. **Runtime Stage:** Uses smaller `eclipse-temurin:17-jre` image
   - Copies only the compiled JAR from build stage
   - Sets up non-root user for security
   - Configures Java optimizations

**Pros:** Reproducible builds, no local dependencies, CI/CD ready

**Which to use?**
- **Local development/testing:** Use Option A (faster iteration)
- **Production/CI/CD:** Use Option B (reproducible builds)

**2. Run the Container Locally:**

```bash
# Run the container
docker run -p 8086:8086 media-forking-simulator:local

# Or run in detached mode
docker run -d -p 8086:8086 --name media-forking-test media-forking-simulator:local
```

**3. Test the Local Container:**

```bash
# Test health endpoint
grpcurl -plaintext localhost:8086 com.cisco.wcc.ccai.v1.Health/Check

# List available services
grpcurl -plaintext localhost:8086 list
```

**Expected Response:**
```json
{
  "status": "SERVING"
}
```

**4. View Container Logs:**

```bash
# View logs (if running in detached mode)
docker logs media-forking-test

# Follow logs in real-time
docker logs -f media-forking-test
```

**5. Stop and Remove Container:**

```bash
# Stop the container
docker stop media-forking-test

# Remove the container
docker rm media-forking-test
```

**IntelliJ Docker Configuration:**

If you have a Docker run configuration in IntelliJ:

1. **Open Run/Debug Configurations** (Run → Edit Configurations)
2. **Select your Docker configuration**
3. **Run** the configuration (▶️ button)
4. **View logs** in the IntelliJ Run tool window
5. **Stop** the container using the stop button (⏹️)

**Troubleshooting Local Docker:**

- **Port already in use:** Make sure no other process is using port 8086
  ```bash
  lsof -i :8086
  kill <PID>
  ```
- **Image build fails:** Ensure `mvn clean install` completed successfully
- **Container exits immediately:** Check logs with `docker logs <container-id>`

---

#### Step 6b: Deploy to Cloud Platforms

Once you've verified the Docker container works locally, deploy it to a public endpoint.

**Option A: Deploy to Google Cloud Run (Recommended for Testing)**

Google Cloud Run is ideal for testing because it's serverless, scales automatically, and has a generous free tier.

**Prerequisites:**
- Google Cloud account with billing enabled
- `gcloud` CLI installed and authenticated
- Project ID ready (replace `YOUR_PROJECT_ID` below)
  - **Important:** Project ID must be lowercase (e.g., `cloudrungrpc` not `CloudRunGRPC`)

**For Apple Silicon Macs (M1/M2/M3):**

The base image needs to support multiple platforms. Use Docker buildx for multi-platform builds:

```bash
# Navigate to project directory
cd /Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator

# Enable buildx (if not already enabled)
docker buildx create --use

# Build for AMD64 (Cloud Run) and push directly
# Note: This will compile the project inside Docker (takes 2-3 minutes)
docker buildx build --platform linux/amd64 \
  -t gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v1 \
  --push .
```

**For Intel Macs or Linux:**

```bash
# Navigate to project directory
cd /Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator

# Build Docker image (compiles inside container)
docker build -t gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v1 .

# Push to Google Container Registry
docker push gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v1
```

**Deploy to Cloud Run:**

```bash
gcloud run deploy media-forking-simulator \
  --image gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8086 \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10
```

**Expected Output:**
```
Deploying container to Cloud Run service [media-forking-simulator] in project [YOUR_PROJECT_ID] region [us-central1]
✓ Deploying... Done.
  ✓ Creating Revision...
  ✓ Routing traffic...
Done.
Service [media-forking-simulator] revision [media-forking-simulator-00001-abc] has been deployed and is serving 100 percent of traffic.
Service URL: https://media-forking-simulator-abc123-uc.a.run.app
```

**Save the Service URL** - you'll need it for WXCC configuration.

**View Logs:**
```bash
# View recent logs
gcloud run services logs read media-forking-simulator --region us-central1 --limit 50

# Stream logs in real-time
gcloud run services logs tail media-forking-simulator --region us-central1
```

**Update Deployment:**
```bash
# After making code changes, rebuild and redeploy
docker build -t gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v2 .
docker push gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v2

gcloud run deploy media-forking-simulator \
  --image gcr.io/YOUR_PROJECT_ID/media-forking-simulator:v2 \
  --region us-central1
```

**Cost Estimate:**
- Free tier: 2 million requests/month, 360,000 GB-seconds/month
- Typical usage: ~$5-20/month for development/testing
- Production: Scales with usage

#### Option B: Use ngrok for Local Testing

```bash
# Install ngrok (if not already installed)
# Download from https://ngrok.com/download

# Start ngrok tunnel
ngrok tcp 8086
```

**Note:** ngrok will provide a public endpoint like `tcp://0.tcp.ngrok.io:12345`

#### Option C: Deploy to Your Own Infrastructure

- Deploy to AWS ECS, Azure Container Instances, or your own Kubernetes cluster
- Ensure the endpoint is publicly accessible
- Configure appropriate security groups and firewall rules

### Step 7: Verify the Simulator is Running

Test the health endpoint to confirm your deployment is working.

**For Cloud Run Deployments:**

Cloud Run automatically provides TLS, so you don't use `-plaintext` and must specify port 443:

```bash
# Test Cloud Run endpoint (must specify port 443)
grpcurl YOUR_SERVICE_NAME.run.app:443 com.cisco.wcc.ccai.v1.Health/Check

# Example:
grpcurl media-forking-simulator-908846715353.us-central1.run.app:443 com.cisco.wcc.ccai.v1.Health/Check
```

**For Local Testing:**

Local containers use plaintext (no TLS) on port 8086:

```bash
# Test local endpoint (plaintext on port 8086)
grpcurl -plaintext localhost:8086 com.cisco.wcc.ccai.v1.Health/Check
```

**Common Mistakes:**
- ❌ `grpcurl -plaintext https://your-service.run.app:8086` - Don't use `https://`, `-plaintext`, or `:8086` with Cloud Run
- ❌ `grpcurl your-service.run.app:8086` - Cloud Run uses port 443, not 8086
- ✅ `grpcurl your-service.run.app:443` - Correct for Cloud Run
- ✅ `grpcurl -plaintext localhost:8086` - Correct for local testing

**Expected Response:**
```json
{
  "status": "SERVING"
}
```

**Note:** The Health service returns a simple status enum (UNKNOWN, SERVING, or NOT_SERVING) as defined in the gRPC health check protocol.

---

## Audio Storage Feature

The simulator automatically saves received audio streams as WAV files. This feature helps you verify that audio is being received correctly and provides recordings for analysis.

### How It Works

**Automatic Environment Detection:**
- **Cloud Run:** Saves WAV files to Google Cloud Storage bucket `ccaiaudiofiles`
- **Local:** Saves WAV files to `target/audio/` directory

**File Naming:**
- Format: `audio/{conversationId}-{roleId}.wav`
- Example: `audio/050bdbad-dfcc-4049-b39d-81e2643b00f7-7bbcc1ed-170f-4dfa-a37c-d51179cfd50f.wav`
- Separate files for each role (customer and agent)
- Files stored in `audio/` subdirectory within the bucket

**Audio Format:**
- Format: WAV with proper headers
- Encoding: Automatically detected from stream
  - **LINEAR16** → PCM format (format code 1, 16-bit)
  - **MULAW** → G.711 μ-law format (format code 7, 8-bit)
- Sample Rate: As received from WXCC (typically 8kHz)
- Channels: Mono (1 channel per role)

### Configuration

Audio storage is controlled by the `GCS_BUCKET_NAME` environment variable:

**For Cloud Run:**
```bash
# Set during deployment
gcloud run deploy media-forking-simulator \
  --image gcr.io/cloudrungrpc/media-forking-simulator:v2 \
  --region us-central1 \
  --set-env-vars GCS_BUCKET_NAME=ccaiaudiofiles
```

**For local testing:**
```bash
# Set environment variable before running
export GCS_BUCKET_NAME=ccaiaudiofiles
java -jar target/dialog-connector-simulator-1.0.0-SNAPSHOT-allinone.jar
```

**To disable audio storage:** Don't set the `GCS_BUCKET_NAME` environment variable. The service will return an error if audio forking is attempted without GCS configured.

### Viewing Saved Audio Files

**On Cloud Run:**

1. **Navigate to Google Cloud Storage:**
   ```bash
   # List files in the bucket
   gsutil ls gs://ccaiaudiofiles/audio/
   
   # Download a specific file
   gsutil cp gs://ccaiaudiofiles/audio/CONVERSATION_ID-ROLE_ID.wav ./
   ```

2. **Or use the Cloud Console:**
   - Go to: https://console.cloud.google.com/storage/browser/ccaiaudiofiles/audio
   - Browse and download files

**Locally:**

```bash
# Audio files are saved to target/audio/ (if GCS_BUCKET_NAME is set and GCS is accessible)
ls -la target/audio/

# Play audio (macOS)
afplay CONVERSATION_ID-ROLE_ID.wav

# Play audio (Linux)
aplay CONVERSATION_ID-ROLE_ID.wav
```

### Audio Processing Workflow

1. **Audio chunks received** from WXCC via gRPC stream
2. **Buffered by participant** (customer and agent tracked separately)
3. **When stream completes** (isFinal flag received):
   - Complete audio assembled from chunks
   - WAV header added
   - File saved to GCS (Cloud Run) or local directory

### Troubleshooting Audio Storage

**Issue: No audio files appearing**

**Check logs:**
```bash
# Cloud Run
gcloud run services logs read media-forking-simulator --region us-central1 | grep -i "audio"

# Local
# Look for "Saved audio file" or "Audio storage is ENABLED" in console output
```

**Verify configuration:**
- Ensure `GCS_BUCKET_NAME` environment variable is set
- Cloud Run: Check deployment env vars: `gcloud run services describe media-forking-simulator --region us-central1 --format="value(spec.template.spec.containers[0].env)"`
- Local: Check env var: `echo $GCS_BUCKET_NAME`

**Cloud Run specific:**
- Verify the service account has write permissions to the GCS bucket
- Check bucket exists: `gsutil ls gs://ccaiaudiofiles/`
- Create bucket if needed: `gsutil mb gs://ccaiaudiofiles/`
- Verify bucket has `audio/` directory (created automatically on first upload)

**Local specific:**
- Requires Google Cloud credentials configured locally
- Run `gcloud auth application-default login` to set up credentials
- Service will attempt to use default GCS credentials

**Issue: Audio files are empty or corrupted**

**Possible causes:**
- Audio stream ended prematurely
- Incorrect audio encoding format
- Sample rate mismatch

**Check logs for:**
- Audio chunk sizes
- Sample rate values
- Encoding type (should be LINEAR16 or MULAW)

### Security Considerations

**Google Cloud Storage:**
- Audio files may contain sensitive customer conversations
- Ensure bucket has appropriate access controls
- Consider enabling encryption at rest
- Set lifecycle policies to auto-delete old files

```bash
# Set bucket to private (recommended)
gsutil iam ch allUsers:objectViewer gs://ccaiaudiofiles/
gsutil iam ch -d allUsers gs://ccaiaudiofiles/

# Add lifecycle rule to delete files after 30 days
cat > lifecycle.json << EOF
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {"age": 30}
      }
    ]
  }
}
EOF
gsutil lifecycle set lifecycle.json gs://ccaiaudiofiles/
```

**Compliance:**
- Check your organization's data retention policies
- Ensure compliance with regulations (GDPR, CCPA, etc.)
- Consider PCI-DSS requirements if handling payment card data
- Implement appropriate access logging and monitoring

---

## JWS Token Authentication

The simulator implements robust security by validating JWS (JSON Web Signature) tokens sent by WXCC. This ensures that only authenticated requests from Cisco's Webex Contact Center can access your media forking endpoint.

### How It Works

**Authentication Flow:**

1. **WXCC sends gRPC request** with JWS token in `authorization` header
2. **AuthorizationServerInterceptor** intercepts the request
3. **Token extracted** and routed to appropriate handler
4. **JWTAuthorizationHandler validates:**
   - Token signature using Cisco's public key
   - Token expiration
   - Issuer (must be Cisco Identity Broker)
   - Claims (audience, subject, JWT ID)
   - **Datasource URL** (must match your configured endpoint)
   - **Schema UUID** (must be media forking schema)
5. **Request allowed** if validation succeeds, rejected otherwise

### Configuration

Set your datasource URL in `config.properties`:

```properties
# Datasource URL for JWT validation - must match the URL in JWT claims
# This URL represents where your dialog connector simulator is accessible
# For local development with ngrok, use your ngrok URL
# For production, use your actual service URL
DATASOURCE_URL = https://media-forking-simulator-908846715353.us-central1.run.app:443
```

**Important:** This URL must **exactly match** the datasource URL you register with WXCC.

### Security Features

**1. Signature Verification**
- Fetches Cisco's public keys from Identity Broker
- Validates token signature using RSA public key cryptography
- Ensures token hasn't been tampered with

**2. Token Expiration**
- Checks token expiration timestamp
- Rejects expired tokens automatically

**3. Claims Validation**
- **Issuer:** Must be one of Cisco's valid Identity Broker URLs
  - `https://idbroker.webex.com/idb`
  - `https://idbroker-eu.webex.com/idb`
  - `https://idbroker-b-us.webex.com/idb`
  - `https://idbroker-ca.webex.com/idb`
  - `https://idbrokerbts.webex.com/idb` (test)
  - `https://idbrokerbts-eu.webex.com/idb` (test)
- **Audience:** Must be present
- **Subject:** Must be present
- **JWT ID:** Must be present

**4. Datasource URL Validation**
- Token must contain `com.cisco.datasource.url` claim
- Value must match your configured `DATASOURCE_URL`
- Prevents tokens intended for other endpoints from being accepted

**5. Schema UUID Validation**
- Token must contain `com.cisco.datasource.schema.uuid` claim
- Value must be `5397013b-7920-4ffc-807c-e8a3e0a18f43` (media forking schema)
- Ensures token is specifically for media forking, not other services

**6. Public Key Caching**
- Public keys cached for 60 minutes
- Reduces latency and API calls
- Handles rate limiting gracefully

### Authentication Bypass

The following services **do not require authentication:**
- **Health Check:** `com.cisco.wcc.ccai.v1.Health/Check`
- **gRPC Reflection:** For service discovery with `grpcurl`

This allows you to test the health endpoint without tokens.

### Testing Authentication

**Test without token (should fail):**
```bash
# This will fail with UNAUTHENTICATED error
grpcurl media-forking-simulator-908846715353.us-central1.run.app:443 \
  com.cisco.wcc.ccai.media.v1.ConversationAudio/streamConversationAudio
```

**Expected error:**
```
ERROR:
  Code: Unauthenticated
  Message: Authorization failed: Invalid authorization token
```

**Test with valid token (from WXCC):**
```bash
# WXCC automatically includes the JWS token when connecting
# You don't need to manually provide it
```

### Troubleshooting Authentication

**Issue: All requests failing with "Authorization failed"**

**Check logs:**
```bash
# Cloud Run
gcloud run services logs read media-forking-simulator --region us-central1 | grep -i "authorization\|token"

# Look for:
# - "Token validation successful" (good)
# - "Token validation failed" (bad)
# - "JWT token is expired" (token expired)
# - "Claims validation failed" (URL or schema mismatch)
```

**Common causes:**

1. **Datasource URL mismatch**
   - Token contains different URL than your `DATASOURCE_URL`
   - **Solution:** Update `DATASOURCE_URL` in `config.properties` to match your registered datasource
   - Rebuild and redeploy: `mvn clean install`

2. **Token expired**
   - JWS tokens have expiration timestamps
   - **Solution:** WXCC should automatically refresh tokens; check your data source registration

3. **Invalid issuer**
   - Token from unexpected source
   - **Solution:** Verify token is from Cisco Identity Broker

4. **Public key fetch failed**
   - Can't reach Identity Broker
   - **Solution:** Check network connectivity, firewall rules

**Issue: Health check failing**

Health checks should **not** require authentication. If failing:

```bash
# Verify health check bypass is working
grpcurl media-forking-simulator-908846715353.us-central1.run.app:443 \
  com.cisco.wcc.ccai.v1.Health/Check
```

Should return `{"status": "SERVING"}` without any token.

### Security Best Practices

**1. Keep DATASOURCE_URL Secure**
- Don't commit sensitive URLs to public repositories
- Use environment variables for production deployments
- Rotate URLs if compromised

**2. Monitor Authentication Failures**
- Set up alerts for repeated authentication failures
- May indicate attack attempts or misconfiguration

**3. Log Security Events**
- Authentication successes and failures are logged
- Review logs regularly for suspicious activity

**4. Update Dependencies**
- Keep `nimbus-jose-jwt` library updated
- Security patches for cryptographic libraries are critical

**5. Network Security**
- Use TLS for all connections (automatic on Cloud Run)
- Don't expose gRPC endpoint without authentication
- Consider additional network-level security (VPC, firewall rules)

### Implementation Details

**Key Classes:**

- **`AuthorizationServerInterceptor`** - Intercepts all gRPC calls
- **`JWTAuthorizationHandler`** - Validates JWS tokens
- **`AuthorizationHandlerFactory`** - Routes to appropriate handler
- **`PublicKeyResponse`** - Caches Cisco's public keys

**Dependencies:**
- `nimbus-jose-jwt` - JWT parsing and validation
- `jackson-databind` - JSON parsing

---

## Setting Up IntelliJ IDEA for Development

If you're using IntelliJ IDEA (recommended for Java development), follow these steps to set up your development environment.

### Prerequisites

- **IntelliJ IDEA:** Download from [JetBrains](https://www.jetbrains.com/idea/download/) (Community or Ultimate edition)
- **Java 17 JDK:** Java 17 is recommended (Java 18-22 also supported but Java 17 provides best compatibility)
- **Maven:** IntelliJ has built-in Maven support
- **Google Cloud Code Plugin:** For Google Cloud integration

### Step 1: Open the Project in IntelliJ

1. **Launch IntelliJ IDEA**
2. **Open Project:**
   - Click **File → Open**
   - Navigate to: `/Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator`
   - Click **OK**

3. **Import as Maven Project:**
   - IntelliJ will detect the `pom.xml` file
   - Click **"Import Maven Projects"** in the notification that appears
   - Wait for Maven to download dependencies (this may take a few minutes)

### Step 2: Configure Java SDK

1. **Open Project Structure:**
   - Click **File → Project Structure** (or press `Cmd + ;` on Mac)
2. **Set Project SDK:**
   - Under **Project Settings → Project**
   - Set **SDK:** to Java 17 (recommended) or Java 18-22
   - Set **Language Level:** to 17 (project compiles to Java 17 bytecode)
3. **Click Apply and OK**

**Note:** While Java 18-22 are supported, Java 17 is recommended for best compatibility with all dependencies.

### Step 3: Generate Protocol Buffer Classes

Before running the application, generate Java classes from `.proto` files:

1. **Open Maven Tool Window:**
   - Click **View → Tool Windows → Maven** (or press `Cmd + 1` then select Maven)
2. **Run Maven Goals:**
   - Expand **dialog-connector-simulator → Lifecycle**
   - Double-click **clean**
   - Double-click **compile**
3. **Verify Generated Classes:**
   - Check `target/generated-sources/protobuf/java/` for generated files
   - IntelliJ should automatically mark this as a source folder (blue folder icon)

### Step 4: Configure Run Configuration

1. **Create Run Configuration:**
   - Click **Run → Edit Configurations**
   - Click **+** (Add New Configuration) → **Application**
2. **Configure:**
   - **Name:** `Media Forking Simulator`
   - **Main class:** `com.cisco.wccai.grpc.server.GrpcServer`
   - **VM options:** `-Xmx512m` (optional, for memory allocation)
   - **Working directory:** `$MODULE_WORKING_DIR$`
   - **Use classpath of module:** `dialog-connector-simulator`
3. **Click Apply and OK**

### Step 5: Run the Simulator from IntelliJ

1. **Start the Server:**
   - Click the green **Run** button (▶️) in the toolbar
   - Or press `Ctrl + R` (Mac) / `Shift + F10` (Windows/Linux)

2. **Check Console Output:**
   ```
   INFO: Starting gRPC Server...
   INFO: Server started, listening on port 8086
   INFO: Health service registered
   INFO: ConversationAudioFork service registered
   ```

3. **Test Locally:**
   ```bash
   # In a terminal, test the health endpoint
   grpcurl -plaintext localhost:8086 com.cisco.wcc.ccai.v1.Health/Check
   ```

### Step 6: Debug Mode

To debug the simulator:

1. **Set Breakpoints:**
   - Open `ConversationAudioForkServiceImpl.java`
   - Click in the left gutter next to line numbers to set breakpoints
   - Recommended breakpoints:
     - `onNext()` method (when audio chunks arrive)
     - `onCompleted()` method (when stream ends)

2. **Start Debug Session:**
   - Click the **Debug** button (🐛) instead of Run
   - Or press `Ctrl + D` (Mac) / `Shift + F9` (Windows/Linux)

3. **Inspect Variables:**
   - When breakpoint hits, inspect audio data, conversation IDs, etc.
   - Use **Evaluate Expression** (`Alt + F8`) to test code snippets

---

## Deploying to Google Cloud from IntelliJ

Deploy your simulator to Google Cloud Run directly from IntelliJ for easy testing and production deployment.

### Prerequisites

- **Google Cloud Account:** Sign up at [cloud.google.com](https://cloud.google.com)
- **Google Cloud Project:** Create a project in Google Cloud Console
- **Billing Enabled:** Cloud Run requires billing to be enabled
- **Cloud Code Plugin:** Install in IntelliJ

### Step 1: Install Google Cloud Code Plugin

1. **Open Plugin Settings:**
   - Click **IntelliJ IDEA → Preferences** (Mac) or **File → Settings** (Windows/Linux)
   - Navigate to **Plugins**
2. **Search and Install:**
   - Search for **"Cloud Code"**
   - Click **Install**
   - Restart IntelliJ when prompted

### Step 2: Authenticate with Google Cloud

1. **Open Cloud Code:**
   - Click **Tools → Cloud Code → Sign In to Google Cloud**
2. **Login:**
   - Browser will open for authentication
   - Sign in with your Google account
   - Grant permissions
3. **Select Project:**
   - In IntelliJ, click **Cloud Code** in the toolbar
   - Select your Google Cloud project from the dropdown

### Step 3: Configure Dockerfile

The project already includes a Dockerfile optimized for Google Cloud:

```dockerfile
FROM google/cloud-sdk

RUN mkdir /app
COPY target/dialog-connector-simulator-1.0.0-SNAPSHOT-allinone.jar /app
WORKDIR /app

EXPOSE 8086

CMD ["java", "-jar", "/app/dialog-connector-simulator-1.0.0-SNAPSHOT-allinone.jar"]
```

**Key Points:**
- Uses `google/cloud-sdk` base image for Google Cloud integration
- Exposes port 8086 (gRPC server port)
- Runs the all-in-one JAR file

### Step 4: Build and Deploy to Cloud Run

#### Option A: Using Cloud Code Plugin (Recommended)

1. **Open Run/Debug Configurations:**
   - Click **Run → Edit Configurations**
   - Click **+** → **Cloud Run: Deploy**

2. **Configure Deployment:**
   - **Name:** `Deploy to Cloud Run`
   - **Project:** Select your Google Cloud project
   - **Region:** Choose region (e.g., `us-central1`)
   - **Service name:** `media-forking-simulator`
   - **Dockerfile:** Select `Dockerfile` in project root
   - **Build settings:**
     - **Builder:** Cloud Build
     - **Image:** `gcr.io/YOUR_PROJECT_ID/media-forking-simulator`

3. **Advanced Settings:**
   - **Port:** 8086
   - **Memory:** 512 MiB (adjust based on needs)
   - **CPU:** 1
   - **Max instances:** 10 (adjust for scale)
   - **Allow unauthenticated:** ✅ (for WXCC to connect)

4. **Deploy:**
   - Click **Run** (▶️) button
   - IntelliJ will:
     1. Build the JAR file
     2. Build Docker image
     3. Push to Google Container Registry
     4. Deploy to Cloud Run
   - Watch progress in the **Run** tool window

5. **Get Service URL:**
   - After deployment completes, copy the service URL
   - Format: `https://media-forking-simulator-abc123-uc.a.run.app`
   - **Important:** This is your public endpoint for WXCC configuration

#### Option B: Using gcloud CLI

Alternatively, deploy using command line:

```bash
# Navigate to project directory
cd /Users/raschiff/coding/forking/webex-contact-center-provider-sample-code/media-service-api/dialog-connector-simulator

# Build the JAR
mvn clean install

# Build and push Docker image
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/media-forking-simulator

# Deploy to Cloud Run
gcloud run deploy media-forking-simulator \
  --image gcr.io/YOUR_PROJECT_ID/media-forking-simulator \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8086 \
  --memory 512Mi \
  --max-instances 10
```

### Step 5: Configure Environment Variables (Optional)

If your simulator needs configuration:

1. **In Cloud Code Deployment:**
   - In deployment configuration, add **Environment Variables**
   - Example:
     ```
     LOG_LEVEL=DEBUG
     AUDIO_STORAGE_BUCKET=gs://your-bucket-name
     ```

2. **Via gcloud CLI:**
   ```bash
   gcloud run services update media-forking-simulator \
     --set-env-vars LOG_LEVEL=DEBUG,AUDIO_STORAGE_BUCKET=gs://your-bucket-name
   ```

### Step 6: Monitor and View Logs

#### From IntelliJ:

1. **Open Cloud Code:**
   - Click **Tools → Cloud Code → Cloud Run → View Logs**
2. **Select Service:**
   - Choose `media-forking-simulator`
3. **View Real-Time Logs:**
   - Logs appear in the tool window
   - Filter by severity (INFO, WARNING, ERROR)

#### From Google Cloud Console:

1. **Navigate to:** [Cloud Run Console](https://console.cloud.google.com/run)
2. **Click your service:** `media-forking-simulator`
3. **Click "LOGS" tab**
4. **View logs in real-time**

### Step 7: Test the Deployed Service

```bash
# Test health endpoint
grpcurl YOUR_SERVICE_URL:443 com.cisco.wcc.ccai.v1.Health/Check

# Example:
grpcurl media-forking-simulator-abc123-uc.a.run.app:443 com.cisco.wcc.ccai.v1.Health/Check
```

**Expected Response:**
```json
{
  "status": "SERVING"
}
```

### Step 8: Update and Redeploy

When you make code changes:

1. **Make Changes** in IntelliJ
2. **Test Locally** using the Run configuration
3. **Redeploy:**
   - Click **Run → Run 'Deploy to Cloud Run'**
   - Or use the green Run button with the Cloud Run configuration selected
4. **Cloud Code automatically:**
   - Rebuilds the JAR
   - Rebuilds the Docker image
   - Deploys the new version
   - Routes traffic to the new version

### Troubleshooting Google Cloud Deployment

#### Issue: Build Fails

```bash
# Check Maven build locally first
mvn clean install

# If successful locally, check Cloud Build logs
gcloud builds list --limit 5
gcloud builds log BUILD_ID
```

#### Issue: Service Won't Start

```bash
# Check service status
gcloud run services describe media-forking-simulator --region us-central1

# View recent logs
gcloud run services logs read media-forking-simulator --region us-central1 --limit 50
```

#### Issue: Port Mismatch

- Ensure Dockerfile `EXPOSE` matches the port in your Java code (8086)
- Ensure Cloud Run deployment uses `--port 8086`
- Check that `GrpcServer.java` binds to the correct port

#### Issue: Authentication Errors

```bash
# Re-authenticate
gcloud auth login
gcloud auth application-default login

# Set project
gcloud config set project YOUR_PROJECT_ID
```

---

## Google Cloud Integration Best Practices

### 1. Use Google Cloud Storage for Audio

Store received audio in Google Cloud Storage:

```java
// Add to pom.xml (already included)
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
    <version>2.38.0</version>
</dependency>

// In your audio handler
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.BlobInfo;

Storage storage = StorageOptions.getDefaultInstance().getService();
BlobInfo blobInfo = BlobInfo.newBuilder("your-bucket", "audio-file.wav").build();
storage.create(blobInfo, audioBytes);
```

### 2. Use Cloud Logging

The project uses Logback with Logstash encoder for structured logging:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(YourClass.class);

// Logs automatically appear in Cloud Logging
logger.info("Received audio chunk: {} bytes", audioData.length);
logger.error("Error processing audio", exception);
```

### 3. Use Secret Manager for Credentials

Store sensitive data in Google Secret Manager:

```bash
# Create secret
echo -n "your-jwt-secret" | gcloud secrets create jwt-secret --data-file=-

# Grant Cloud Run access
gcloud secrets add-iam-policy-binding jwt-secret \
  --member="serviceAccount:YOUR_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"

# Reference in Cloud Run
gcloud run services update media-forking-simulator \
  --set-secrets="JWT_SECRET=jwt-secret:latest"
```

### 4. Enable Cloud Monitoring

Monitor your service performance:

1. **Navigate to:** [Cloud Monitoring](https://console.cloud.google.com/monitoring)
2. **Create Dashboard** for your service
3. **Add Metrics:**
   - Request count
   - Request latency
   - Error rate
   - Memory usage
   - CPU utilization

### 5. Set Up Alerts

```bash
# Create alert for high error rate
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-threshold-value=0.05 \
  --condition-threshold-duration=300s
```

---

## Step-by-Step Setup Guide

Now that you have the simulator running, let's configure WXCC to send media streams to your endpoint.

### Step 1: Create a Service App

**Tool:** [Webex Developer Portal](https://developer.webex.com)

**What is a Service App?**

A Service App is a special type of Webex application that operates independently of user authentication. Unlike integrations that act on behalf of users, Service Apps use machine accounts and are authorized at the organization level. Learn more: [Service Apps Documentation](https://developer.webex.com/messaging/docs/service-apps)

**Steps:**

1. **Login** to the Webex Developer Portal
2. Navigate to **My Webex Apps**
3. Click **Create a New App** → **Create a Service App**

4. **Fill in the registration form:**
   - **Name:** Your media forking service name (e.g., "Acme Real-Time Analytics")
   - **Description:** Clear explanation of your media forking service and value proposition
   - **Logo:** Professional logo (displayed to customer admins)
   - **Icon:** App icon
   - **Support URL:** Your support contact page
   - **Privacy URL:** Your privacy policy

5. **Select Data Source Schema:**
   - ✅ **Media Forking Schema** (this wraps the protobuf protocol)
   - Specify your **Data Exchange Domain** (your public gRPC endpoint)
   - Example: `https://media-forking-simulator-abc123.run.app`
   - **Important:** This is the top-level domain that will be validated. Your actual data source URLs can be subdomains or paths under this domain.

6. **Select Required Scopes:**
   - ✅ `spark-admin:dataSource_read` (Required - read data source configurations)
   - ✅ `spark-admin:dataSource_write` (Required - register data sources)

7. **Save and Copy Credentials:**
   - **Client ID:** Copy and save securely
   - **Client Secret:** ⚠️ Shown only once! Copy and save securely
   - You'll need these to retrieve organization-specific tokens

8. **Submit for Authorization:**
   - Click **"Request Admin Authorization"** (for testing in your own org)
   - Or **"Submit to App Hub"** (for production deployment to customer orgs)
   - This makes your Service App visible to customer admins for authorization

---

### Step 2: Admin Authorization (Customer Org Admin)

**Tool:** Webex Control Hub

⚠️ **This step is performed by the customer's Full Admin.**

**Provide these instructions to your customers:**

1. **Login to Control Hub:** https://admin.webex.com
2. **Navigate to:** Apps → Service Apps
3. **Find your Service App** in the list (or via App Hub if published)
4. **Review the app details carefully:**
   - **Partner/Developer information:** Who created this app
   - **Description:** What the app does
   - **Data destination URL:** Where media will be sent
   - **Schema:** What type of data is being sent (Media Forking)
   - **Requested scopes:** What permissions the app needs
5. **Click "Authorize"**
6. **Confirm authorization**

**What happens behind the scenes:**
- Your Service App is authorized for the customer's organization
- A machine account is created for your Service App
- The Service App becomes **selectable in Flow Designer** for media forking activities
- You (the partner) can now retrieve organization-specific access and refresh tokens
- You can register data sources for this organization

---

### Step 3: Retrieve Organization-Specific Tokens

**Tool:** Webex Developer Portal or API

After a customer admin authorizes your Service App, you need to retrieve access and refresh tokens specific to that organization.

#### Option A: Via Developer Portal (Manual)

1. **Go to your Service App details page** in the Developer Portal
2. **Scroll to "Org Authorizations" section**
3. **Select the organization** from the dropdown
   - Organizations are identified by their base64-encoded Org ID
   - Format: `Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi8...`
4. **Enter your Client Secret**
5. **Click "Get Tokens"**
6. **Copy and securely store:**
   - **Access Token:** Used for API calls (expires in ~14 days)
   - **Refresh Token:** Used to get new access tokens (expires in ~90 days)

#### Option B: Via API (Automated - Recommended)

```bash
POST https://webexapis.com/v1/applications/{appId}/token
Authorization: Bearer YOUR_PERSONAL_ACCESS_TOKEN
Content-Type: application/json

{
  "clientId": "YOUR_CLIENT_ID",
  "clientSecret": "YOUR_CLIENT_SECRET",
  "targetOrgId": "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi8..."
}
```

**Response:**
```json
{
  "access_token": "ZmI0ZjYwNzktYzBjMi00NGE1LWEwMjYtZGU3MDUzZjk5YzRm...",
  "refresh_token": "MDM4NzYwNmItZjBhYi00YTQ5LWE1ZjItZGU3MDUzZjk5YzRm...",
  "token_type": "Bearer",
  "expires_in": 1209600
}
```

**Important:** Store these tokens securely! You'll use them to register the data source.

---

### Step 4: Register Your Data Source

**Tool:** [Webex Data Sources API](https://developer.webex.com/webex-contact-center/docs/api/v1/data-sources)

Now register your gRPC endpoint as a **data source** (more accurately, a data sink) for this specific customer organization.

**Why "Data Source"?**

The API is called "data sources" because it's a generic API used for multiple use cases. For media forking specifically, this is actually a **data sink** - the destination where media is sent. Each customer organization gets its own data source registration, allowing independent management per customer.

#### Register the Data Source

```bash
POST https://webexapis.com/v1/dataSources
Authorization: Bearer SERVICE_APP_ACCESS_TOKEN_FOR_THIS_ORG
Content-Type: application/json

{
  "name": "Acme Media Forking Endpoint - Customer XYZ",
  "description": "Real-time media streaming for sentiment analysis",
  "schema": "mediaForking",
  "endpoint": "https://media-forking-customer-xyz.acme.com:8086",
  "authentication": {
    "type": "JWS",
    "publicKey": "YOUR_PUBLIC_KEY_FOR_JWS_VALIDATION"
  }
}
```

**Important Notes:**
- **Endpoint URL:** Must match or be a subdomain/path of the Data Exchange Domain specified in your Service App
  - ✅ Service App domain: `acme.com` → Data source: `https://media-forking-customer-xyz.acme.com`
  - ✅ Service App domain: `acme.com` → Data source: `https://acme.com/customer-xyz`
  - ❌ Service App domain: `acme.com` → Data source: `https://different.com` (will be rejected)
- **Authentication:** Provide your public key so WXCC can construct JWS tokens that you can validate
- **Per-Organization:** Register a separate data source for each customer organization

**Response:**
```json
{
  "id": "dataSource-abc123",
  "name": "Acme Media Forking Endpoint - Customer XYZ",
  "schema": "mediaForking",
  "endpoint": "https://media-forking-customer-xyz.acme.com:8086",
  "status": "active",
  "created": "2026-01-07T16:00:00.000Z",
  "orgId": "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi8..."
}
```

**Save the `dataSource-id` for your records.**

---

### Step 5: Token Management Strategy

**Important:** Understand the different tokens involved and their management requirements.

#### OAuth Tokens (Access & Refresh)

These tokens are used for **managing your data source registrations** via the Webex APIs.

**Token Lifecycle:**
- **Access Token:** Expires in 14 days
- **Refresh Token:** Expires in 90 days, but expiration extends each time it's used
- **Standard OAuth behavior:** As long as you use the refresh token, it remains valid

**When to Refresh:**
- Refresh the access token before it expires (recommend: when < 1 day remaining)
- Use the refresh token to get a new access/refresh token pair
- Both tokens are replaced when you refresh

**Refresh OAuth Access Token:**

```bash
POST https://webexapis.com/v1/access_token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id=YOUR_CLIENT_ID
&client_secret=YOUR_CLIENT_SECRET
&refresh_token=YOUR_REFRESH_TOKEN
```

**Response:**
```json
{
  "access_token": "NEW_ACCESS_TOKEN",
  "refresh_token": "NEW_REFRESH_TOKEN",
  "token_type": "Bearer",
  "expires_in": 1209600
}
```

**Important:** Both tokens are refreshed - store the new tokens and discard the old ones.

---

#### JWS Token (Runtime Authentication)

The JWS token is used by WXCC to **authenticate with your gRPC server at runtime** when streaming media.

**How It Works:**
1. You register a data source with authentication details (public key, nonce, expiration)
2. When a call triggers media forking, WXCC constructs a JWS token using your authentication details
3. WXCC sends the JWS token to your gRPC server
4. Your server validates the token using your private key

**JWS Token Requirements:**
- **Maximum lifetime:** 24 hours
- **Update frequency:** Between 1 hour (minimum) and 24 hours (maximum)
- **Best practice:** Update every 12 hours

**How to Update JWS Token Info:**

Update your data source registration with new nonce and expiration:

```bash
PATCH https://webexapis.com/v1/dataSources/{dataSourceId}
Authorization: Bearer SERVICE_APP_ACCESS_TOKEN
Content-Type: application/json

{
  "authentication": {
    "type": "JWS",
    "publicKey": "YOUR_PUBLIC_KEY",
    "nonce": "NEW_RANDOM_NONCE",
    "expiration": "2026-01-08T12:00:00Z"  // 24 hours from now
  }
}
```

**Implementation Example:**

```python
import time
import secrets
from datetime import datetime, timedelta

class MediaForkingTokenManager:
    def __init__(self, client_id, client_secret):
        self.client_id = client_id
        self.client_secret = client_secret
        self.oauth_tokens = {}  # org_id -> {access_token, refresh_token, expiry}
        self.data_sources = {}  # org_id -> {data_source_id, last_jws_update}
    
    def refresh_oauth_if_needed(self, org_id):
        """Refresh OAuth access token if expiring soon"""
        token_info = self.oauth_tokens.get(org_id)
        if not token_info:
            return
        
        # Refresh if token expires in less than 1 day
        if datetime.now() >= token_info['expiry'] - timedelta(days=1):
            self.refresh_oauth_token(org_id)
    
    def update_jws_token_info(self, org_id):
        """Update data source with new JWS nonce and expiration"""
        ds_info = self.data_sources.get(org_id)
        if not ds_info:
            return
        
        # Check if we need to update (every 12 hours)
        if datetime.now() >= ds_info['last_jws_update'] + timedelta(hours=12):
            # Generate new nonce
            new_nonce = secrets.token_urlsafe(32)
            new_expiration = datetime.now() + timedelta(hours=24)
            
            # Update data source via API
            self.update_data_source_auth(org_id, new_nonce, new_expiration)
            
            # Update tracking
            ds_info['last_jws_update'] = datetime.now()
    
    def refresh_oauth_token(self, org_id):
        # Call OAuth refresh endpoint
        # Update self.oauth_tokens[org_id] with new tokens
        pass
    
    def update_data_source_auth(self, org_id, nonce, expiration):
        # Call PATCH /v1/dataSources/{id} endpoint
        # Update authentication.nonce and authentication.expiration
        pass

# Background task: Check and update tokens
while True:
    for org_id in token_manager.oauth_tokens.keys():
        # Refresh OAuth tokens if needed
        token_manager.refresh_oauth_if_needed(org_id)
        
        # Update JWS token info if needed
        token_manager.update_jws_token_info(org_id)
    
    time.sleep(3600)  # Check every hour
```

---

### Step 6: Create a Flow with Media Forking

**Tool:** Flow Designer (Control Hub)

⚠️ **This step is performed by the customer's contact center administrator.**

1. **Navigate to:** Contact Center → Flows
2. **Create a new flow** or **edit an existing flow**

3. **Add the Media Forking Activity:**
   - Drag the **"Media Forking"** activity into your flow
   - Place it **after** the customer connects to an agent
   - **Important:** Media forking only works during agent conversations, not during IVR

4. **Configure the Activity:**
   - **Service App:** Select your authorized Service App from the dropdown
     - Only Service Apps that have been authorized will appear here
     - This is NOT a CCAI Configuration - you're selecting the Service App directly
   - **Conversation ID:** Use flow variable `{{conversationId}}`

5. **Connect the Flow:**
   ```
   [Entry Point] → [IVR/Self-Service] → [Queue to Agent] → [Agent Answers] → [Media Forking Activity] → [Continue Flow]
   ```

6. **Validate and Publish** the flow

**What Happens at Runtime:**

When the media forking activity is triggered:
1. WXCC Orchestrator receives the media stream from the agent-customer conversation
2. Orchestrator uses the organization ID to lookup the registered data source URL
3. Orchestrator retrieves authentication details and constructs a JWS token
4. Orchestrator establishes a gRPC connection to your endpoint
5. Orchestrator authenticates with the JWS token
6. Audio streams (customer and agent channels) are sent to your gRPC server in real-time

---

### Step 7: Map Entry Point to Flow

**Tool:** Flow Designer (Control Hub)

1. **Navigate to:** Contact Center → Channels → Entry Points
2. **Select your Entry Point** (phone number, chat channel, etc.)
3. **Routing Strategy:** Select your routing strategy
4. **Flow:** Select the flow you created in Step 5
5. **Save the mapping**

---

## Testing Your Integration

### Test Checklist

- [ ] **Simulator Running:** Verify your gRPC server is running and accessible
- [ ] **Health Check:** Confirm health endpoint responds correctly
- [ ] **Service App Authorized:** Verify authorization in Control Hub
- [ ] **Data Source Registered:** Confirm data source is active
- [ ] **CCAI Configuration Created:** Verify configuration exists
- [ ] **Flow Published:** Confirm flow is published and active
- [ ] **Entry Point Mapped:** Verify entry point routes to your flow

### Making a Test Call

1. **Call your Entry Point** (contact center phone number)
2. **Navigate through IVR** (if applicable)
3. **Request to speak with an agent**
4. **Wait for agent connection**
5. **Media forking should trigger** when agent answers

### Monitoring the Simulator

Watch the simulator logs for incoming connections:

```bash
# Simulator logs should show:
INFO: Received gRPC connection from WXCC
INFO: Session started - conversationId: conv-12345
INFO: Receiving audio stream - Channel 1 (Customer)
INFO: Receiving audio stream - Channel 2 (Agent)
INFO: Audio chunk received - 320 bytes
INFO: Audio chunk received - 320 bytes
...
```

### Verifying Audio Streams

The simulator saves received audio to files:

```bash
# Check the output directory
ls -la /tmp/media-forking/

# You should see files like:
# conv-12345-customer.wav
# conv-12345-agent.wav
```

Play these files to verify audio quality:

```bash
# Play customer audio
aplay /tmp/media-forking/conv-12345-customer.wav

# Play agent audio
aplay /tmp/media-forking/conv-12345-agent.wav
```

---

## Troubleshooting

### Common Issues

#### Issue 1: Simulator Not Receiving Connections

**Symptoms:**
- No logs showing incoming connections
- WXCC shows "connection failed" errors

**Possible Causes & Solutions:**

1. **Endpoint Not Publicly Accessible**
   ```bash
   # Test connectivity from external network
   telnet YOUR_PUBLIC_ENDPOINT 8086
   ```
   - **Solution:** Ensure firewall rules allow incoming connections on port 8086
   - **Solution:** Verify your endpoint is publicly routable (not localhost)

2. **TLS/SSL Certificate Issues**
   ```bash
   # Test TLS connection
   openssl s_client -connect YOUR_PUBLIC_ENDPOINT:8086
   ```
   - **Solution:** Ensure certificate is valid and not self-signed
   - **Solution:** Verify certificate chain is complete

3. **Wrong Endpoint in Configuration**
   - **Solution:** Double-check the endpoint URL in your data source registration
   - **Solution:** Ensure protocol is `grpc://` not `https://`

#### Issue 2: Audio Quality Issues

**Symptoms:**
- Audio is choppy or distorted
- Missing audio chunks

**Possible Causes & Solutions:**

1. **Network Latency**
   ```bash
   # Test latency to your endpoint
   ping YOUR_PUBLIC_ENDPOINT
   ```
   - **Solution:** Deploy closer to WXCC data centers (US-based recommended)
   - **Solution:** Use a CDN or edge deployment

2. **Incorrect Audio Format**
   - **Solution:** Verify you're handling LINEAR16 or μ-law encoding
   - **Solution:** Confirm sample rate is 8kHz or 16kHz
   - **Solution:** Ensure single-channel audio processing

3. **Buffer Overflow**
   - **Solution:** Increase buffer size in your gRPC server
   - **Solution:** Process audio asynchronously to avoid blocking

#### Issue 3: Authentication Failures

**Symptoms:**
- Connection rejected with "authentication failed"
- 401 or 403 errors in logs

**Possible Causes & Solutions:**

1. **Invalid JWT Token**
   - **Solution:** Regenerate JWT token
   - **Solution:** Verify token hasn't expired
   - **Solution:** Check token signing algorithm matches expected

2. **Service App Not Authorized**
   - **Solution:** Verify admin has authorized your Service App
   - **Solution:** Check authorization status in Control Hub

#### Issue 4: Media Forking Not Triggering

**Symptoms:**
- Call completes but no media streams received
- Flow executes but skips media forking activity

**Possible Causes & Solutions:**

1. **Activity Placed Before Agent Connection**
   - **Solution:** Move media forking activity to **after** agent answers
   - **Solution:** Verify flow logic ensures agent connection before forking

2. **License Not Enabled**
   - **Solution:** Verify media forking license is active for the organization
   - **Solution:** Contact Cisco support to confirm license provisioning

3. **Configuration Not Selected**
   - **Solution:** Verify CCAI configuration is selected in the flow activity
   - **Solution:** Confirm configuration status is "active"

#### Issue 5: High Latency

**Symptoms:**
- Significant delay between conversation and audio receipt
- Real-time features not working effectively

**Possible Causes & Solutions:**

1. **Geographic Distance**
   - **Solution:** Deploy your endpoint in the same region as WXCC
   - **Solution:** Use edge computing or regional deployments

2. **Processing Bottleneck**
   - **Solution:** Profile your audio processing pipeline
   - **Solution:** Optimize or parallelize heavy operations
   - **Solution:** Use asynchronous processing

3. **Network Congestion**
   - **Solution:** Monitor network metrics
   - **Solution:** Implement QoS (Quality of Service) policies
   - **Solution:** Use dedicated network connections

### Debug Mode

Enable detailed logging in the simulator:

```properties
# application.properties
logging.level.root=DEBUG
logging.level.com.cisco.wccai=TRACE
logging.level.io.grpc=DEBUG

# Log all gRPC messages
grpc.server.enableReflection=true
grpc.server.logRequests=true
grpc.server.logResponses=true
```

### Testing Tools

#### grpcurl - Test gRPC Endpoints

```bash
# List available services
grpcurl -plaintext YOUR_ENDPOINT:8086 list

# Call health check
grpcurl -plaintext YOUR_ENDPOINT:8086 com.cisco.wcc.ccai.v1.Health/Check

# Test with reflection
grpcurl -plaintext YOUR_ENDPOINT:8086 describe
```

#### Wireshark - Capture gRPC Traffic

```bash
# Capture on gRPC port
sudo tcpdump -i any -w grpc-capture.pcap port 8086

# Open in Wireshark and filter:
# tcp.port == 8086
```

### Getting Help

If you're still experiencing issues:

1. **Check Simulator Logs:** Review detailed logs for error messages
2. **Verify Configuration:** Double-check all configuration steps
3. **Test Connectivity:** Use tools like `telnet`, `grpcurl`, `openssl`
4. **Contact Support:** Reach out to Cisco support with:
   - Organization ID
   - Configuration ID
   - Conversation ID (from failed call)
   - Simulator logs
   - Network diagnostics

---

## Next Steps

Congratulations! You now have a working media forking setup with the simulator. Here's what to do next:

### 1. Understand the gRPC Protocol

**Next Document:** [Media Forking gRPC Protocol Reference](./Media_Forking_gRPC_Protocol.md)

This document will cover:
- Detailed protocol buffer definitions
- Message types and sequences
- Bidirectional streaming patterns
- Error handling and retries
- Best practices for production implementations

### 2. Build Your Production Solution

Replace the simulator with your production implementation:

- **Speech-to-Text:** Integrate with Google Speech-to-Text, AWS Transcribe, or Azure Speech Services
- **Sentiment Analysis:** Add real-time sentiment detection
- **Agent Assistance:** Build AI-powered agent coaching
- **Recording:** Implement secure storage and retrieval
- **Analytics:** Create dashboards and insights

### 3. Optimize for Production

- **Scalability:** Design for high-volume concurrent calls
- **Reliability:** Implement retry logic and failover
- **Monitoring:** Add metrics, alerts, and observability
- **Security:** Implement proper authentication and encryption
- **Compliance:** Ensure PCI-DSS, GDPR, HIPAA compliance as needed

### 4. Performance Tuning

- **Latency Optimization:** Minimize end-to-end latency
- **Resource Management:** Optimize CPU, memory, and network usage
- **Load Testing:** Test with realistic call volumes
- **Auto-Scaling:** Implement dynamic scaling based on demand

### 5. Advanced Features

Explore advanced capabilities:
- **Multi-Language Support:** Handle conversations in multiple languages
- **Speaker Diarization:** Identify and separate multiple speakers
- **Emotion Detection:** Detect emotional states beyond sentiment
- **Intent Recognition:** Identify customer intent in real-time
- **Knowledge Base Integration:** Suggest relevant articles to agents

---

## Support & Resources

### Documentation

- **Webex Contact Center Developer Portal:** https://developer.webex.com/webex-contact-center
- **Service Apps Guide:** https://developer.webex.com/docs/service-apps
- **gRPC Documentation:** https://grpc.io/docs/
- **Protocol Buffers:** https://developers.google.com/protocol-buffers

### Sample Code

- **GitHub Repository:** https://github.com/CiscoDevNet/webex-contact-center-provider-sample-code
- **Dialog Connector Simulator:** `/media-service-api/dialog-connector-simulator`
- **Protocol Definitions:** `/media-service-api/dialog-connector-simulator/src/main/proto`

### Getting Help

- **Developer Support:** ccai-connectors@cisco.com
- **Developer Community:** https://developer.webex.com/support
- **Cisco TAC:** For licensed customers with support contracts

### Related Guides

- [Bring Your Own Virtual Agent](https://developer.webex.com/webex-contact-center/docs/bring-your-own-virtual-agent)
- [Virtual Agent Transcripts and Call Summary](https://developer.webex.com/webex-contact-center/docs/virtual-agent-transcripts-and-call-summary)
- [Contact Control APIs](https://developer.webex.com/webex-contact-center/docs/contact-control-apis)

---

## Glossary

- **Media Forking:** Real-time streaming of audio from customer-agent conversations to external systems
- **CCAI:** Contact Center AI - Cisco's AI platform for contact centers
- **gRPC:** High-performance RPC framework used for media streaming
- **Media Sink:** Your gRPC server endpoint that receives audio streams
- **Data Source:** Registered endpoint configuration in Webex
- **CCAI Configuration:** Settings that define which features and endpoints to use
- **Flow Designer:** Visual tool for creating contact center call flows
- **Entry Point:** The initial point of contact (phone number, chat, etc.)
- **Conversation ID:** Unique identifier for each customer-agent conversation
- **Dual-Channel Audio:** Separate audio streams for customer and agent

---

**Document Version:** 1.0  
**Last Updated:** January 7, 2026  
**Maintained By:** Webex Contact Center AI Team  
**Feedback:** ccai-connectors@cisco.com
