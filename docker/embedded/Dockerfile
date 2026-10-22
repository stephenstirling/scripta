# Scripta - Full version (embedded frontend)
# Uses pre-built base image for fast builds

ARG BASE_VERSION=latest
ARG BASE_IMAGE=ghcr.io/stephenstirling/scripta-base:${BASE_VERSION}

# Stage 1: Build the Java application and frontend
FROM gradle:9.7.1-jdk25@sha256:e06837018d077ee7f1218e53499425ca7615702ad58856d980c154caa17eb093 AS app-build

ARG TASK_VERSION=3.54.0
ARG NODE_MAJOR_VERSION=24
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && update-ca-certificates \
    && curl -fsSL "https://deb.nodesource.com/setup_${NODE_MAJOR_VERSION}.x" | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && ARCH=$(dpkg --print-architecture) \
    && curl -fsSL "https://github.com/go-task/task/releases/download/v${TASK_VERSION}/task_${TASK_VERSION}_linux_${ARCH}.deb" -o /tmp/task.deb \
    && dpkg -i /tmp/task.deb \
    && rm /tmp/task.deb \
    && rm -rf /var/lib/apt/lists/*

# Bake the detection model in so the tool works out of the box (~26MB pull, 35MB on disk).
# Only weights we may redistribute belong here; FFDetr is Apache-2.0 end to end.
ARG FORM_DETECTION_MODEL_URL="https://huggingface.co/Frooodle/ffdetr-int8/resolve/ad3223d7c3ae25ce1d1ec8fe972c8ff7630aff45/ffdetr-int8.onnx"
ARG FORM_DETECTION_MODEL_SHA256="de43bb39adb08459fe62fa1a7be6bfb4f2d397bb034c46055f3b8fa3d04fc8f4"
ARG FORM_DETECTION_MODEL_ID="ffdetr"
RUN mkdir -p /preinstalled-models \
    && if [ -n "${FORM_DETECTION_MODEL_URL}" ]; then \
         curl -fSL "${FORM_DETECTION_MODEL_URL}" -o "/preinstalled-models/${FORM_DETECTION_MODEL_ID}.onnx" \
         && echo "${FORM_DETECTION_MODEL_SHA256}  /preinstalled-models/${FORM_DETECTION_MODEL_ID}.onnx" | sha256sum -c -; \
       else \
         echo "No form-detection model pre-bundled (FORM_DETECTION_MODEL_URL unset)"; \
       fi

# JDK 25+: --add-exports is no longer accepted via JAVA_TOOL_OPTIONS; use JDK_JAVA_OPTIONS instead
ENV JDK_JAVA_OPTIONS="--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle/                               gradle/
COPY buildSrc/build.gradle                 buildSrc/
COPY buildSrc/src/main/                    buildSrc/src/main/
COPY app/core/build.gradle                  app/core/
COPY app/common/build.gradle                app/common/

# Use system gradle instead of gradlew to avoid SSL issues downloading gradle distribution on emulated arm64
RUN gradle dependencies --no-daemon || true

COPY . .

ARG STIRLING_FLAVOR=core
ENV STIRLING_FLAVOR=${STIRLING_FLAVOR}
# Include the processor (admin portal) in the bundled editor; false leaves it out.
ARG BUILD_PORTAL=false

# Which Stirling account the portal connects to. Build-time because Vite inlines VITE_* into the
# bundle; there is no runtime override. Empty keeps the committed frontend .env defaults, which are
# production Stirling, so a stock image links with no build args. Overriding VITE_SAAS_API_URL
# also means setting STIRLING_BILLING_ACCOUNT_LINK_SAAS_BASE_URL on the container, or the browser
# and the server link against different SaaS. The publishable key is client-side by design, not a
# secret. Pass the URL and the key from the same Supabase project or the browser accepts the pair
# and Supabase rejects it, which surfaces later as "session expired".
ARG VITE_SUPABASE_URL=""
ARG VITE_SUPABASE_PUBLISHABLE_DEFAULT_KEY=""
ARG VITE_SAAS_API_URL=""

# Bundle only the JPDFium native for this image's target arch.
ARG TARGETARCH
# Every ARG reaches RUN as an environment variable, empty ones included, and Vite lets any VITE_*
# in process.env beat the .env files. Unset the empty ones or they blank the committed defaults.
RUN JPDFIUM_PLATFORM="$([ "$TARGETARCH" = arm64 ] && echo linux-arm64 || echo linux-x64)" && \
    if [ -z "${VITE_SUPABASE_URL}" ]; then unset VITE_SUPABASE_URL; fi; \
    if [ -z "${VITE_SUPABASE_PUBLISHABLE_DEFAULT_KEY}" ]; then unset VITE_SUPABASE_PUBLISHABLE_DEFAULT_KEY; fi; \
    if [ -z "${VITE_SAAS_API_URL}" ]; then unset VITE_SAAS_API_URL; fi; \
    STIRLING_FLAVOR=${STIRLING_FLAVOR} \
    gradle clean build \
      -PbuildWithFrontend=true \
      -PbuildWithPortal=${BUILD_PORTAL} \
      -PjpdfiumPlatforms="$JPDFIUM_PLATFORM" \
      -PbundleOnnxRuntime=true \
      -x spotlessApply -x spotlessCheck -x test -x sonarqube \
      --no-daemon

# Stage 2: Extract Spring Boot Layers
FROM eclipse-temurin:25-jre-noble@sha256:b4c93a50fc67612798db73d68ca3b0ee4ebdd51736e59cca370e689b9797037e AS jar-extract
WORKDIR /tmp
COPY --from=app-build /app/app/core/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination /layers

# Strip the bundled ONNX Runtime's natives down to the target Linux arch (~42MB jar -> ~8MB).
COPY scripts/slim-onnxruntime.sh /tmp/slim-onnxruntime.sh
RUN apt-get update \
    && apt-get install -y --no-install-recommends zip \
    && rm -rf /var/lib/apt/lists/* \
    && sh /tmp/slim-onnxruntime.sh /layers/dependencies


# Stage 3: Final runtime image on top of pre-built base
FROM ${BASE_IMAGE}

ARG VERSION_TAG

WORKDIR /app

# Application layers
COPY --link --from=jar-extract --chown=1000:1000 /layers/dependencies/           /app/
COPY --link --from=jar-extract --chown=1000:1000 /layers/spring-boot-loader/     /app/
COPY --link --from=jar-extract --chown=1000:1000 /layers/snapshot-dependencies/  /app/
COPY --link --from=jar-extract --chown=1000:1000 /layers/application/            /app/

COPY --link --from=app-build --chown=1000:1000 \
     /app/build/libs/restart-helper.jar /restart-helper.jar
COPY --link --chown=1000:1000 scripts/ /scripts/

# Baked-in detection model; FormDetectionModelManager seeds it into /configs on startup.
COPY --link --from=app-build --chown=1000:1000 /preinstalled-models/ /opt/stirling/preinstalled-models/

# Fonts go to system dir, root ownership is correct (world-readable)
COPY app/core/src/main/resources/static/fonts/*.ttf /usr/share/fonts/truetype/

# Permissions and configuration
RUN set -eux; \
    chmod +x /scripts/*; \
    ln -s /logs /app/logs; \
    ln -s /configs /app/configs; \
    ln -s /customFiles /app/customFiles; \
    ln -s /pipeline /app/pipeline; \
    ln -s /storage /app/storage; \
    chown -h stirlingpdfuser:stirlingpdfgroup /app/logs /app/configs /app/customFiles /app/pipeline /app/storage; \
    chown stirlingpdfuser:stirlingpdfgroup /app; \
    chmod 750 /tmp/stirling-pdf; \
    chmod 750 /tmp/stirling-pdf/heap_dumps; \
    fc-cache -f

# Tesseract "best" models (Apache-2.0), selectable per OCR run as the Accuracy setting.
# They live beside the bundled fast models, in tessdata_best, which RuntimePathConfig derives.
# osd is needed for page rotation detection when OCR runs against this directory.
ARG TESSDATA_BEST_COMMIT=e12c65a915945e4c28e237a9b52bc4a8f39a0cec
RUN set -eux; \
    dest=/usr/share/tesseract-ocr/5/tessdata_best; \
    mkdir -p "$dest"; \
    for model in \
      8280aed0782fe27257a68ea10fe7ef324ca0f8d85bd2fd145d1c2b560bcb66ba:eng \
      8407331d6aa0229dc927685c01a7938fc5a641d1a9524f74838cdac599f0d06e:deu \
      907743d98915c91a3906dfbf6e48b97598346698fe53aaa797e1a064ffcac913:fra \
      711de9dbb8052067bd42f16b9119967f30bada80d57e2ef24f65d09f531adb04:por \
      4fef2d1306c8e87616d4d3e4c6c67faf5d44be3342290cf8f2f0f6e3aa7e735b:chi_sim \
      9cf5d576fcc47564f11265841e5ca839001e7e6f38ff7f7aacf46d15a96b00ff:osd; do \
      sha="${model%%:*}"; lang="${model##*:}"; \
      curl -fsSL "https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/${TESSDATA_BEST_COMMIT}/${lang}.traineddata" -o "$dest/${lang}.traineddata"; \
      echo "${sha}  $dest/${lang}.traineddata" | sha256sum -c -; \
    done; \
    chmod -R a+rX "$dest"

# Write version to a file so it is readable by scripts without env-var inheritance.
# init-without-ocr.sh reads /etc/stirling_version for the AOT cache fingerprint.
RUN echo "${VERSION_TAG:-dev}" > /etc/stirling_version

# Environment variables
ENV VERSION_TAG=$VERSION_TAG \
    FORMDETECTION_PREINSTALLEDMODELDIR="/opt/stirling/preinstalled-models" \
    STIRLING_AOT_ENABLE="false" \
    _JVM_OPTS="-XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/configs/heap_dumps -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1PeriodicGCInterval=10000 -XX:G1PeriodicGCSystemLoadThreshold=0.0 -XX:+G1PeriodicGCInvokesConcurrent -XX:+UseCompactObjectHeaders -XX:+UseStringDeduplication -XX:+ExplicitGCInvokesConcurrent -XX:ReservedCodeCacheSize=96m -XX:CICompilerCount=2 -Dspring.threads.virtual.enabled=true -Djava.awt.headless=true" \
    JAVA_CUSTOM_OPTS="" \
    HOME=/home/stirlingpdfuser \
    PUID=1000 \
    PGID=1000 \
    UMASK=022 \
    STIRLING_TEMPFILES_DIRECTORY=/tmp/stirling-pdf \
    TMPDIR=/tmp/stirling-pdf \
    TEMP=/tmp/stirling-pdf \
    TMP=/tmp/stirling-pdf \
    DBUS_SESSION_BUS_ADDRESS=/dev/null \
    SAL_TMP=/tmp/stirling-pdf/libre

# Metadata labels
LABEL org.opencontainers.image.title="Scripta" \
      org.opencontainers.image.description="Full version with Calibre, LibreOffice, Tesseract, OCRmyPDF" \
      org.opencontainers.image.source="https://github.com/stephenstirling/scripta" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.vendor="Stephen Stirling" \
      org.opencontainers.image.url="https://github.com/stephenstirling/scripta" \
      org.opencontainers.image.documentation="https://github.com/stephenstirling/scripta#readme" \
      maintainer="Stephen Stirling" \
      org.opencontainers.image.authors="Stephen Stirling; based on Stirling-PDF by Stirling-Tools" \
      org.opencontainers.image.version="${VERSION_TAG}" \
      org.opencontainers.image.keywords="PDF, manipulation, API, Spring Boot, React"

EXPOSE 8080/tcp
STOPSIGNAL SIGTERM

HEALTHCHECK --interval=30s --timeout=15s --start-period=120s --retries=5 \
  CMD curl -fs --max-time 10 http://localhost:8080${SYSTEM_ROOTURIPATH:-''}/api/v1/info/status || exit 1

ENTRYPOINT ["tini", "--", "/scripts/init.sh"]
CMD []
