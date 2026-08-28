#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
ENV_FILE="${SCRIPT_DIR}/.env"
ARTIFACT_DIR="${SCRIPT_DIR}/artifacts"

declare -A MAVEN_MODULE=(
    [bin-gateway]="bin-gateway"
    [admin-api]="admin-api"
    [open-api]="open-api"
    [platform-service]="platform-service"
)

declare -A TARGET_DIR=(
    [bin-gateway]="bin-gateway"
    [admin-api]="bin-api/admin-api"
    [open-api]="bin-api/open-api"
    [platform-service]="bin-platform/platform-service"
)

declare -A JAR_PREFIX=(
    [bin-gateway]="bin-gateway"
    [admin-api]="admin-api"
    [open-api]="open-api"
    [platform-service]="platform-service"
)

ALL_SERVICES=(bin-gateway admin-api open-api platform-service)

log() {
    printf '[bin-cloud] %s\n' "$*"
}

fail() {
    printf '[bin-cloud] ERROR: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
用法：
  ./deploy/backend-deploy.sh all
  ./deploy/backend-deploy.sh bin-gateway
  ./deploy/backend-deploy.sh admin-api open-api
  ./deploy/backend-deploy.sh platform-service

可选服务：bin-gateway、admin-api、open-api、platform-service、all
默认跳过测试；需要执行测试时使用 RUN_TESTS=true。
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "缺少命令：$1"
}

run_docker() {
    if docker info >/dev/null 2>&1; then
        docker "$@"
    else
        require_command sudo
        sudo docker "$@"
    fi
}

require_env_value() {
    local key="$1"
    local line
    line="$(grep -E "^${key}=.+$" "${ENV_FILE}" | tail -n 1 || true)"
    [[ -n "${line}" ]] || fail "deploy/.env 缺少 ${key}"
    [[ "${line#*=}" != "CHANGE_ME" ]] || fail "请设置 deploy/.env 中的 ${key}"
}

copy_artifact() {
    local service="$1"
    local source_dir="${PROJECT_DIR}/${TARGET_DIR[${service}]}/target"
    local prefix="${JAR_PREFIX[${service}]}"
    local jars=()

    mapfile -t jars < <(
        find "${source_dir}" -maxdepth 1 -type f -name "${prefix}-*.jar" ! -name "*.original" -print
    )
    [[ "${#jars[@]}" -eq 1 ]] || fail "${service} 构建产物数量异常：${#jars[@]}"
    install -m 0644 "${jars[0]}" "${ARTIFACT_DIR}/${service}.jar"
}

if [[ "$#" -eq 0 ]]; then
    usage
    exit 1
fi

SELECTED_SERVICES=()
declare -A SEEN=()
for requested in "$@"; do
    if [[ "${requested}" == "all" ]]; then
        SELECTED_SERVICES=("${ALL_SERVICES[@]}")
        break
    fi
    [[ -n "${MAVEN_MODULE[${requested}]:-}" ]] || {
        usage
        fail "不支持的服务：${requested}"
    }
    if [[ -z "${SEEN[${requested}]:-}" ]]; then
        SELECTED_SERVICES+=("${requested}")
        SEEN[${requested}]=1
    fi
done

require_command git
require_command java
require_command mvn
require_command docker
require_command find
require_command install
require_command awk

JAVA_SPECIFICATION_VERSION="$(
    java -XshowSettings:properties -version 2>&1 \
        | awk -F= '/java.specification.version/ {gsub(/[[:space:]]/, "", $2); print $2; exit}'
)"
[[ "${JAVA_SPECIFICATION_VERSION}" == "21" ]] || {
    fail "当前 Java 版本为 ${JAVA_SPECIFICATION_VERSION:-unknown}，项目必须使用 JDK 21"
}

MAVEN_VERSION="$(mvn -version | awk 'NR == 1 {print $3}')"
MAVEN_MAJOR="${MAVEN_VERSION%%.*}"
MAVEN_REMAINDER="${MAVEN_VERSION#*.}"
MAVEN_MINOR="${MAVEN_REMAINDER%%.*}"
[[ "${MAVEN_MAJOR}" == "3" && "${MAVEN_MINOR}" -ge 9 ]] || {
    fail "当前 Maven 版本为 ${MAVEN_VERSION:-unknown}，项目要求 Maven 3.9+"
}

[[ -f "${ENV_FILE}" ]] || fail "请先执行 cp deploy/.env.example deploy/.env 并填写配置"
require_env_value JWT_SECRET
require_env_value GATEWAY_CONTEXT_SIGNING_SECRET
require_env_value NACOS_SERVER_ADDR
require_env_value NACOS_PASSWORD
for service in "${SELECTED_SERVICES[@]}"; do
    if [[ "${service}" == "platform-service" ]]; then
        require_env_value MYSQL_PASSWORD
        require_env_value REDIS_PASSWORD
        break
    fi
done

cd "${PROJECT_DIR}"
[[ -f pom.xml ]] || fail "未找到根 pom.xml"
if [[ -n "$(git status --porcelain)" ]]; then
    fail "工作区存在未提交修改。为避免拉取代码时覆盖服务器文件，部署已停止"
fi

BRANCH="$(git branch --show-current)"
[[ -n "${BRANCH}" ]] || fail "当前处于 detached HEAD，请先切换到需要部署的分支"

log "拉取 origin/${BRANCH} 最新代码"
git fetch --prune origin
git merge --ff-only "origin/${BRANCH}"

MAVEN_MODULES=()
for service in "${SELECTED_SERVICES[@]}"; do
    MAVEN_MODULES+=(":${MAVEN_MODULE[${service}]}")
done
MODULE_LIST="$(IFS=,; printf '%s' "${MAVEN_MODULES[*]}")"

MAVEN_ARGS=(--batch-mode --no-transfer-progress -pl "${MODULE_LIST}" -am clean package)
if [[ "${RUN_TESTS:-false}" != "true" ]]; then
    MAVEN_ARGS+=(-DskipTests)
fi

log "Maven 构建模块：${SELECTED_SERVICES[*]}"
mvn "${MAVEN_ARGS[@]}"

mkdir -p "${ARTIFACT_DIR}"
for service in "${SELECTED_SERVICES[@]}"; do
    copy_artifact "${service}"
done

log "校验 Docker Compose 配置"
run_docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet

log "构建镜像：${SELECTED_SERVICES[*]}"
run_docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" build "${SELECTED_SERVICES[@]}"

log "仅更新选中的容器"
run_docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up \
    -d --no-deps --force-recreate "${SELECTED_SERVICES[@]}"

log "部署完成"
run_docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" ps "${SELECTED_SERVICES[@]}"
