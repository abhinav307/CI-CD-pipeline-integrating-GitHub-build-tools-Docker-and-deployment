# CI-CD-pipeline-integrating-GitHub-build-tools-Docker-and-deployment

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.8%2B-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.11-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Docker](https://img.shields.io/badge/Docker-20.x%2B-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-Registry-0db7ed?logo=docker&logoColor=white)](https://hub.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Runtime](https://img.shields.io/badge/Runtime-Eclipse%20Temurin%2017--JRE-blue)](https://adoptium.net/)

> **Program 15 — Cloud Computing & DevOps Lab**  
> An enterprise-grade, fully automated Continuous Integration and Continuous Deployment (CI/CD) pipeline integrating **GitHub Actions**, **Apache Maven**, **JUnit 5**, **Docker**, and **Docker Hub**. Features multi-job workflow orchestration, inter-stage artifact caching, automated container publishing with dual-tagging (`latest` + `commit-sha`), and self-verifying containerized deployment.

---

## 📑 Table of Contents

- [Aim & Academic Objectives](#-aim--academic-objectives)
- [Pipeline Architecture & Workflow Graph](#-pipeline-architecture--workflow-graph)
- [Project Directory Structure](#-project-directory-structure)
- [Prerequisites & System Requirements](#-prerequisites--system-requirements)
- [Component Breakdown](#-component-breakdown)
  - [1. Java Application & Unit Testing](#1-java-application--unit-testing)
  - [2. Maven Project Object Model (pom.xml)](#2-maven-project-object-model-pomxml)
  - [3. Containerization Strategy (Dockerfile)](#3-containerization-strategy-dockerfile)
- [GitHub Actions Pipeline Deep Dive](#-github-actions-pipeline-deep-dive)
  - [Stage 1: Build and Test (`build-test`)](#stage-1-build-and-test-build-test)
  - [Stage 2: Package & Push Image (`docker-build-push`)](#stage-2-package--push-image-docker-build-push)
  - [Stage 3: Deployment Verification (`deploy`)](#stage-3-deployment-verification-deploy)
- [Docker Hub & GitHub Secrets Setup](#-docker-hub--github-secrets-setup)
- [Local Build & Run Instructions](#-local-build--run-instructions)
- [Verification & Observability Matrix](#-verification--observability-matrix)
- [Key DevOps & CI/CD Concepts](#-key-devops--cicd-concepts)
- [Troubleshooting & FAQs](#-troubleshooting--faqs)
- [GitHub Repository Setup](#-github-repository-setup)

---

## 🎯 Aim & Academic Objectives

The core objective of this project is to construct a production-ready, hands-on CI/CD pipeline from scratch:

1. **Continuous Integration (CI)**:
   - Automated triggers on code `push` and `pull_request` against the `main` branch.
   - Code compilation and automated testing using **JUnit 5** and **Maven Surefire**.
   - Packaging application into a standalone executable JAR (`maven-cicd-demo.jar`).
   - Persisting build outputs across runner boundaries using GitHub Actions **Artifacts**.

2. **Continuous Deployment (CD)**:
   - Secure container image assembly via Docker Buildx.
   - Automated authentication to **Docker Hub** using encrypted GitHub repository secrets (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`).
   - Multi-tag release publishing (`:latest` for current production, `:<git-sha>` for immutable audit traceability).
   - Automated post-deployment verification testing running the published container on a remote runner.

---

## 🏗 Pipeline Architecture & Workflow Graph

```
                                      [ GitHub Push to 'main' ]
                                                  │
                                                  ▼
                        ┌──────────────────────────────────────────────────┐
                        │      Job 1: Build and Test (build-test)          │
                        │                                                  │
                        │  1. actions/checkout@v4                          │
                        │  2. actions/setup-java@v4 (Temurin JDK 17)       │
                        │  3. mvn -B clean package                         │
                        │  4. JUnit 5 Test Execution                       │
                        │  5. actions/upload-artifact@v4 (app-jar)         │
                        └─────────────────────────┬────────────────────────┘
                                                  │
                                       app-jar (Artifact)
                                                  │
                                                  ▼
                        ┌──────────────────────────────────────────────────┐
                        │  Job 2: Build & Push Docker (docker-build-push)  │
                        │                                                  │
                        │  1. actions/download-artifact@v4 (app-jar)       │
                        │  2. docker/setup-buildx-action@v3                │
                        │  3. docker/login-action@v3 (Docker Hub Auth)     │
                        │  4. docker/build-push-action@v6                  │
                        │     - Tags: :latest, :${{ github.sha }}          │
                        └─────────────────────────┬────────────────────────┘
                                                  │
                                       Published Image to Docker Hub
                                                  │
                                                  ▼
                        ┌──────────────────────────────────────────────────┐
                        │          Job 3: Deploy & Verify (deploy)         │
                        │                                                  │
                        │  1. docker/login-action@v3                       │
                        │  2. docker pull <user>/maven-cicd-demo:latest    │
                        │  3. docker run --rm <image>                      │
                        │  4. Verify exact output: "Hello World!"          │
                        └──────────────────────────────────────────────────┘
```

---

## 📁 Project Directory Structure

```text
CI-CD-pipeline-integrating-GitHub-build-tools-Docker-and-deployment/
├── .dockerignore                 # Prevents target/, git, and local scripts from entering Docker context
├── .gitignore                    # Excludes build outputs (target/), IDE configs (.idea, .vscode)
├── .github/
│   └── workflows/
│       └── cicd.yml              # 3-job declarative GitHub Actions CI/CD pipeline definition
├── Dockerfile                    # Container definition based on Eclipse Temurin 17 JRE
├── pom.xml                       # Maven build descriptor with compiler, surefire & jar plugins
├── README.md                     # Comprehensive technical documentation & runbook
├── scripts/
│   └── run-local.ps1             # Local test automation script (Maven build -> Docker build -> run)
└── src/
    ├── main/java/com/abhinav/mavendemo/
    │   └── App.java              # Java application entry point (prints "Hello World!")
    └── test/java/com/abhinav/mavendemo/
        └── AppTest.java          # JUnit 5 unit test validating application logic
```

---

## ⚙️ Prerequisites & System Requirements

To build and run this project locally, ensure the following tools are installed:

- **Java Development Kit (JDK)**: Version 17 or higher (JDK 17, 21, or 25 LTS supported).
- **Apache Maven**: Version 3.8+ (`mvn`).
- **Docker Desktop**: Running with Linux container support.
- **Git**: Version 2.25+.

Verify installed tools in PowerShell:

```powershell
java -version
mvn -version
docker --version
git --version
```

---

## 🧩 Component Breakdown

### 1. Java Application & Unit Testing

The core application is located at `src/main/java/com/abhinav/mavendemo/App.java`:

```java
package com.abhinav.mavendemo;

public final class App {
    private App() { }

    public static String message() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        System.out.println(message());
    }
}
```

Unit testing is handled by JUnit 5 (`src/test/java/com/abhinav/mavendemo/AppTest.java`):

```java
package com.abhinav.mavendemo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void messageReturnsHelloWorld() {
        assertEquals("Hello World!", App.message());
    }
}
```

### 2. Maven Project Object Model (`pom.xml`)

Key Maven configuration properties:
- **`maven.compiler.release`**: Set to `17` to ensure bytecode compatibility.
- **`maven-compiler-plugin`** (`v3.13.0`): Compiles source files using the release flag.
- **`maven-surefire-plugin`** (`v3.5.2`): Executes JUnit 5 platform tests during the `test` phase.
- **`maven-jar-plugin`** (`v3.4.2`): Packages an executable manifest with `Main-Class: com.abhinav.mavendemo.App` into `target/maven-cicd-demo.jar`.

### 3. Containerization Strategy (`Dockerfile`)

The image packaging uses the minimal **Eclipse Temurin 17 JRE** base image, minimizing the attack surface and download size:

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/maven-cicd-demo.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## 🚀 GitHub Actions Pipeline Deep Dive

The pipeline is defined in [`.github/workflows/cicd.yml`](file:///.github/workflows/cicd.yml) and executes three ordered, dependent jobs:

### Stage 1: Build and Test (`build-test`)
- Runs on `ubuntu-latest`.
- Sets up Eclipse Temurin JDK 17 with Maven dependency caching (`cache: maven`).
- Runs `mvn -B clean package` in batch mode.
- Saves the built executable JAR as a GitHub Actions artifact named `app-jar`.

### Stage 2: Package & Push Image (`docker-build-push`)
- Depends on `build-test` (`needs: build-test`).
- Triggers on push events to `main` branch.
- Downloads the `app-jar` artifact from Stage 1 into the `target/` directory.
- Configures **Docker Buildx** for advanced image construction.
- Logs in to Docker Hub using GitHub encrypted repository secrets.
- Builds and pushes the image with dual tags:
  - `<DOCKERHUB_USERNAME>/maven-cicd-demo:latest`
  - `<DOCKERHUB_USERNAME>/maven-cicd-demo:<git-sha>`

### Stage 3: Deployment Verification (`deploy`)
- Depends on `docker-build-push` (`needs: docker-build-push`).
- Pulls the newly published image directly from Docker Hub.
- Executes the container and captures stdout.
- Asserts that the captured output strictly equals `"Hello World!"`.

---

## 🔐 Docker Hub & GitHub Secrets Setup

To enable automated Docker Hub publishing and deployment in GitHub Actions:

### Step 1: Generate a Docker Hub Personal Access Token (PAT)
1. Sign in to [hub.docker.com](https://hub.docker.com/).
2. Click your avatar (top right) → **Account settings** → **Security**.
3. Under **Personal access tokens**, click **New Access Token**.
4. Set description to `github-actions-cicd`, select permissions: **Read & Write**.
5. Copy the generated token string.

### Step 2: Configure Secrets in GitHub
1. Open your repository on GitHub: `https://github.com/abhinav307/CI-CD-pipeline-integrating-GitHub-build-tools-Docker-and-deployment`.
2. Go to **Settings** → **Secrets and variables** → **Actions**.
3. Under **Repository secrets**, click **New repository secret**:
   - Name: `DOCKERHUB_USERNAME`  
     Value: *Your Docker Hub username* (e.g., `abhinav307`)
   - Name: `DOCKERHUB_TOKEN`  
     Value: *Your Docker Hub Personal Access Token*

> [!IMPORTANT]  
> Secrets are encrypted at rest and never printed in build logs.

---

## 💻 Local Build & Run Instructions

You can reproduce every stage of the pipeline locally on your development workstation:

### 1. Compile, Test, and Package with Maven

```powershell
mvn -B clean package
```

Expected output:
```text
[INFO] Running com.abhinav.mavendemo.AppTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: ...\target\maven-cicd-demo.jar
[INFO] BUILD SUCCESS
```

### 2. Run the JAR Directly

```powershell
java -jar target/maven-cicd-demo.jar
```
Output:
```text
Hello World!
```

### 3. Build the Docker Image

```powershell
docker build -t maven-cicd-demo:local .
```

### 4. Run the Container

```powershell
docker run --rm maven-cicd-demo:local
```
Output:
```text
Hello World!
```

### 5. Automated Local Script

Run the automated helper script (handles build, packaging, image creation, and execution):

```powershell
pwsh -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

---

## 🔍 Verification & Observability Matrix

| Pipeline Stage | Verification Method | Expected Result |
| :--- | :--- | :--- |
| **Unit Test** | `mvn test` | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |
| **Packaging** | `Test-Path target/maven-cicd-demo.jar` | `True` |
| **Local Container** | `docker run --rm maven-cicd-demo:local` | `Hello World!` (Exit Code: 0) |
| **CI Artifact** | `actions/upload-artifact@v4` | Artifact `app-jar` visible in workflow run summary |
| **Docker Registry** | `docker pull <user>/maven-cicd-demo:latest` | Status: Downloaded newer image |
| **CD Smoke Test** | `docker run --rm <user>/maven-cicd-demo:latest` | Exact string match: `Hello World!` |

---

## 💡 Key DevOps & CI/CD Concepts

- **Continuous Integration (CI)**: Frequent merging of developer code changes accompanied by automated builds and test verification to detect regressions immediately.
- **Continuous Deployment (CD)**: Automatically releasing and deploying validated build artifacts to staging or production registries and environments without manual intervention.
- **Pipeline as Code**: Versioning CI/CD definitions directly alongside application source code in YAML (`.github/workflows/cicd.yml`).
- **Inter-Stage Artifact Passing**: Decoupling the heavyweight compilation environment (JDK) from the lightweight Docker build context using `upload-artifact` and `download-artifact`.
- **Ephemeral Runners**: GitHub-hosted runners provision clean virtual machines per job, ensuring high reproducibility and avoiding configuration drift.

---

## 🛠 Troubleshooting & FAQs

### 1. PowerShell Script Execution Policy Error
If running `.\scripts\run-local.ps1` gives an execution policy error:
```powershell
pwsh -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

### 2. Docker Hub Authentication Failure (`401 Unauthorized`)
- Ensure repository secret `DOCKERHUB_TOKEN` contains an access token with **Read & Write** scope, not an account password.
- Verify that `DOCKERHUB_USERNAME` matches your Docker Hub handle exactly.

### 3. Artifact Mismatch / File Not Found
- Ensure `pom.xml` contains `<finalName>maven-cicd-demo</finalName>`, which guarantees the artifact name is `maven-cicd-demo.jar`.

---

## 🐙 GitHub Repository Setup

To clone, modify, or initialize this repository:

```powershell
# 1. Initialize git repository
git init

# 2. Stage all files
git add .

# 3. Create initial commit
git commit -m "first commit"

# 4. Set main branch
git branch -M main

# 5. Connect remote repository
git remote add origin https://github.com/abhinav307/CI-CD-pipeline-integrating-GitHub-build-tools-Docker-and-deployment.git

# 6. Push to GitHub
git push -u origin main
```

---

## 📜 License & Acknowledgments

- Developed for **Program 15**: CI/CD pipeline integrating GitHub, Maven, Docker, and Deployment.
- Powered by [GitHub Actions](https://github.com/features/actions), [Apache Maven](https://maven.apache.org/), [Docker](https://www.docker.com/), and [Eclipse Adoptium](https://adoptium.net/).
