# Shruti

Shruti is a distributed audio orchestration engine built with Spring Boot 3. It serves as an exploration into building resilient, event-driven distributed systems in the Spring ecosystem. The architecture is designed around asynchronous message passing, fault tolerance, and modular micro-tasking.

This project marks the beginning of a journey into distributed systems architecture, focusing heavily on backend orchestration patterns rather than simple CRUD operations.

## What This Project Demonstrates
- Distributed workflow orchestration
- Event-driven architecture
- RabbitMQ messaging patterns
- Retry and failure recovery
- State machine design
- Idempotent worker execution
- Database-backed workflow persistence
- Observability with Micrometer

## Architecture & Technical Depth

The system is built on an event-driven orchestrator pattern. When an audio file is ingested, it is securely stored in an S3-compatible blob store (MinIO), and a state machine is initialized in PostgreSQL. The `OrchestratorService` then dispatches a series of discrete, idempotent tasks to RabbitMQ.

### 1. High-Level Architecture
```mermaid
graph TD
    Client[Client / Web UI] -->|POST /api/v1/workflows| API[REST API]
    API -->|Store Blob| MinIO[(MinIO Storage)]
    API -->|Init State| DB[(PostgreSQL)]
    API -->|Start DAG| Orch[Orchestrator]
    
    Orch -->|Dispatch| RMQ((RabbitMQ Exchange))
    
    RMQ -->|Queue| W_Val[Validate Worker]
    RMQ -->|Queue| W_Trans[Transcribe Worker]
    RMQ -->|Queue| W_Summ[Summarize Worker]
    RMQ -->|Queue| W_Key[Keywords Worker]
    
    W_Val <-->|Idempotency Check| Redis[(Redis)]
    W_Trans <-->|Idempotency Check| Redis
    W_Summ <-->|Idempotency Check| Redis
    W_Key <-->|Idempotency Check| Redis
    
    W_Trans <-->|Streaming Audio| Groq1[Groq Whisper API]
    W_Summ <-->|Context Window| Groq2[Groq LLaMA 3.1 API]
    
    W_Val -->|Success/Fail| ResultQ((RabbitMQ Result Queue))
    W_Trans -->|Success/Fail| ResultQ
    W_Summ -->|Success/Fail| ResultQ
    W_Key -->|Success/Fail| ResultQ
    
    ResultQ -->|Consume| Listener[Result Listener]
    Listener -->|Trigger Next Node| Orch
```

### 2. State Transition Diagram (State Machine)
Workflows and Tasks operate as strict state machines. The `RetryScheduler` actively monitors for `FAILED` tasks and transitions them back to `PENDING` until the maximum retry limit is reached, at which point they are marked `DEAD`.

```mermaid
stateDiagram-v2
    direction LR
    
    state "Workflow State" as WS {
        [*] --> PENDING
        PENDING --> RUNNING : Start Tasks
        RUNNING --> COMPLETED : All Tasks Done
        RUNNING --> FAILED : Any Task DEAD
    }

    state "Task State" as TS {
        [*] --> P_TASK : Dispatch
        P_TASK: PENDING
        
        P_TASK --> IN_PROGRESS : Worker Picked Up
        IN_PROGRESS --> T_COMPLETED : Success
        T_COMPLETED: COMPLETED
        
        IN_PROGRESS --> T_FAILED : Exception
        T_FAILED: FAILED
        
        T_FAILED --> P_TASK : RetryScheduler (Attempts < Max)
        T_FAILED --> DEAD : Max Attempts Reached
    }
```

### 3. Sequence Diagram (The Asynchronous Loop)
Unlike synchronous REST calls, workers in Shruti never communicate directly with the next worker. They only report success back to the Orchestrator, which decides the next step in the Directed Acyclic Graph (DAG).

```mermaid
sequenceDiagram
    participant API as REST Controller
    participant Orch as OrchestratorService
    participant RMQ as RabbitMQ
    participant Worker as TaskWorker (e.g. Transcribe)
    participant DB as PostgreSQL
    
    API->>DB: Save Workflow & Tasks (PENDING)
    API->>Orch: Dispatch First Task
    Orch->>RMQ: Publish Message (routing_key: transcribe)
    API-->>Client: 202 Accepted (workflowId)
    
    Note over RMQ, Worker: Asynchronous Processing
    RMQ-->>Worker: Consume Message
    Worker->>DB: Update Task (IN_PROGRESS)
    Worker->>Worker: Perform Heavy Lifting (API calls, MinIO reads)
    
    alt Success
        Worker->>RMQ: Publish Success to 'workflow.results'
    else Failure
        Worker->>RMQ: Publish Failure to 'workflow.results'
    end
    
    RMQ-->>Orch: Consume Result Event
    Orch->>DB: Update Task (COMPLETED or FAILED)
    
    alt Task was successful
        Orch->>Orch: Find Next Task in DAG
        Orch->>RMQ: Publish Next Task
    else Task failed & Max Retries hit
        Orch->>DB: Mark Workflow as FAILED
    end
```

### 4. Entity-Relationship Diagram (ERD)
The persistence layer tracks the overarching workflow, granular task states, and an immutable audit log of task execution history for debugging and tracing.

```mermaid
erDiagram
    WORKFLOW ||--o{ TASK : "contains"
    TASK ||--o{ TASK_HISTORY : "generates"
    
    WORKFLOW {
        bigint id PK
        uuid external_id
        varchar status "PENDING, RUNNING, COMPLETED, FAILED"
        varchar audio_file_key
        jsonb metadata
        timestamp created_at
    }
    
    TASK {
        bigint id PK
        bigint workflow_id FK
        varchar task_type "VALIDATE, TRANSCRIBE, SUMMARIZE..."
        varchar status "PENDING, IN_PROGRESS, COMPLETED, FAILED, DEAD"
        int attempts
        jsonb input_data
        jsonb output_data
    }
    
    TASK_HISTORY {
        bigint id PK
        bigint task_id FK
        varchar status
        text error_message
        timestamp occurred_at
    }
```

### 5. Infrastructure & Deployment Model
The entire stack runs in isolated Docker containers communicating over a private Docker bridge network. Only the Spring Boot application (port 8080) and MinIO Console (port 9001) are exposed to the host machine.

```mermaid
graph TD
    subgraph Host Machine
        Browser[Web Browser]
        Browser -->|localhost:8080| AppPort
        Browser -->|localhost:9001| MinioPort
    end
    
    subgraph Docker Bridge Network
        AppPort((:8080)) --> App[Spring Boot Application]
        MinioPort((:9001)) --> MinioUI[MinIO Console]
        
        App -->|JDBC :5432| DB[(PostgreSQL)]
        App -->|AMQP :5672| RMQ((RabbitMQ))
        App -->|RESP :6379| Redis[(Redis)]
        App -->|S3 API :9000| Minio[(MinIO Storage)]
    end
```

### Core Concepts Demonstrated

1. **State Machine Orchestration**: Instead of synchronous method calls, the pipeline (Validation -> Transcription -> Summarization -> Keyword Extraction) is managed by an Orchestrator. As workers complete tasks asynchronously, they emit events back to a result queue. The Orchestrator listens to these events, mutates the workflow state in the database, and dispatches the subsequent task.
2. **Fault Tolerance and Retry Semantics**: Distributed systems fail. The project implements a custom `RetryScheduler` that periodically sweeps the database for stalled or failed tasks and automatically re-queues them, up to a defined maximum attempt threshold, using exponential backoff principles.
3. **Idempotent Workers**: Workers are designed to safely process duplicate messages from RabbitMQ without causing side effects or corrupting data in MinIO or Postgres.
4. **Observability**: Built-in integration with Micrometer exposes custom metrics (task failure rates, retry counts, transcription latency distributions) over Actuator endpoints. These metrics power a live monitoring dashboard.
5. **Database Versioning**: Flyway is utilized to ensure deterministic schema migrations across deployments.

## Infrastructure Stack

- **Application Framework**: Java 21, Spring Boot 3, Spring Data JPA
- **Message Broker**: RabbitMQ (AMQP 0-9-1)
- **Relational Database**: PostgreSQL 16
- **Blob Storage**: MinIO (S3 API compatible)
- **Caching**: Redis
- **AI Inference**: Groq (Whisper-large-v3-turbo and Llama-3.1-8b-instant)

## Running the System

The entire infrastructure, including the application layer, is containerized. There is no requirement for a local Java runtime or Maven installation to start the system.

1. Clone the repository:
   ```bash
   git clone https://github.com/devarshganatra/audio-workflow-orchestration.git
   cd audio-workflow-orchestration
   ```

2. Configure environment variables:
   Create a `.env` file in the root directory and provide your inference API key.
   ```text
   GROQ_API_KEY=your_groq_api_key_here
   ```

3. Build and initialize the distributed stack:
   ```bash
   docker compose up --build -d
   ```

4. Interact with the system:
   The API and the accompanying frontend observability dashboard are bound to port 8080.
   Navigate to `http://localhost:8080` to submit jobs and trace the workflow execution through the distributed workers.

## DeepWiki

For a more in-depth exploration of the project's architecture, workflows, implementation details, and design decisions, refer to the DeepWiki documentation:

**DeepWiki:** https://deepwiki.com/devarshganatra/audio-workflow-orchestration
