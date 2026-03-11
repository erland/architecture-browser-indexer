# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY scripts ./scripts
COPY docs ./docs
COPY lib ./lib
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV ARCH_BROWSER_TREE_SITTER_LIB_DIR=/app/lib/macos-aarch64
COPY --from=build /workspace/target/architecture-browser-indexer-*.jar /app/architecture-browser-indexer.jar
COPY lib /app/lib
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["--help"]
