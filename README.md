# VoxFlow - Audio Orchestration Engine

VoxFlow is a powerful, distributed audio processing pipeline built with Spring Boot 3. It orchestrates the validation, transcription, summarization, and keyword extraction of audio files using RabbitMQ and Groq's LLMs. 

It features a stunning, real-time Analytics Dashboard and Web UI.

## Features
- **Modern Web UI**: Drag-and-drop file uploads, real-time pipeline visualization, and Glassmorphism design.
- **Distributed Processing**: Uses RabbitMQ to dispatch micro-tasks across independent worker threads.
- **AI Integration**: Powered by Groq's insanely fast `whisper-large-v3-turbo` for transcription and `llama-3.1-8b-instant` for summarization.
- **Real-Time Analytics**: Built-in Micrometer metrics exposed via a custom dashboard for deep system observability.
- **Object Storage**: Audio files are safely stored in MinIO (S3-compatible).

## Prerequisites
All you need to run this project is **Docker** and **Docker Compose**. You do NOT need Java or Maven installed on your machine.

## How to Run
1. Clone this repository:
   ```bash
   git clone https://github.com/devarshganatra/audio-workflow-orchestration.git
   cd audio-workflow-orchestration
   ```

2. Configure your API Keys:
   - Create a `.env` file in the root directory.
   - Add your Groq API key:
     ```
     GROQ_API_KEY=your_groq_api_key_here
     ```

3. Start the entire infrastructure (Spring Boot, Postgres, RabbitMQ, Redis, MinIO):
   ```bash
   docker compose up --build -d
   ```

4. Access the UI:
   - Open your browser and navigate to **http://localhost:8080**
   - Upload an audio file and watch the orchestration happen in real-time!

## Architecture Stack
- **Backend**: Java 21, Spring Boot 3, Spring Data JPA
- **Database**: PostgreSQL 16
- **Message Broker**: RabbitMQ
- **Caching**: Redis
- **Storage**: MinIO
- **Frontend**: Vanilla HTML/JS/CSS
