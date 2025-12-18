# Backend Developer Guide

## First Step After Cloning (Required)

This backend is a Maven multi-module Spring Boot project. After `git clone`, you must **initialize/import the Maven project** so your IDE can download dependencies and resolve Spring classes.

### Option A: IDE import (recommended)

- **IntelliJ IDEA**: open the repo, then open `backend-java-spring/pom.xml` and click **Load Maven Changes** (or right-click → **Add as Maven Project**).
- **Eclipse/STS**: **File → Import → Existing Maven Projects** and select `backend-java-spring/pom.xml`.

### Option B: CLI initialization (forces dependency download)

From the repo root:

- Windows: `.\backend-java-spring\mvnw.cmd -f backend-java-spring\pom.xml -DskipTests package`
- macOS/Linux: `./backend-java-spring/mvnw -f backend-java-spring/pom.xml -DskipTests package`

If your IDE still shows unresolved imports, re-import the Maven project and invalidate/restart IDE caches.

