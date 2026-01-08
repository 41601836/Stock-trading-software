#!/bin/bash

# AI炒股软件启动脚本 (Mac版)
# 适用于40G内存的Mac系统，配置了16G JVM堆内存以优化并行回测性能

# 设置脚本执行时的当前目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR"

echo "========================================="
echo "AI炒股软件启动脚本 (Mac版)"
echo "当前目录: $SCRIPT_DIR"
echo "========================================="

# 1. 创建数据目录（用于H2数据库文件）
echo "正在创建数据目录..."
mkdir -p ./data
echo "✅ 数据目录创建完成"

# 2. 检查Java环境
echo "正在检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 未检测到Java环境，请确保已安装Java 17或更高版本"
    echo "   您可以从 https://adoptium.net/zh-CN/ 下载OpenJDK"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -i version | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java版本过低 (当前: $JAVA_VERSION)，请安装Java 17或更高版本"
    exit 1
fi

echo "✅ Java环境正常 (版本: $(java -version 2>&1 | grep -i version | cut -d'"' -f2))"

# 3. 配置JVM参数
echo "正在配置JVM参数..."
# 针对40G内存的Mac系统优化配置
JVM_OPTS=""
JVM_OPTS="$JVM_OPTS -Xms8g"          # 初始堆内存8G
JVM_OPTS="$JVM_OPTS -Xmx16g"         # 最大堆内存16G（充分利用40G内存）
JVM_OPTS="$JVM_OPTS -XX:MaxMetaspaceSize=1g"  # 元空间最大值1G
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"    # 使用G1垃圾回收器，适合大内存应用
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"  # 最大GC暂停时间200ms
JVM_OPTS="$JVM_OPTS -XX:ParallelGCThreads=16"  # 并行GC线程数，根据CPU核心数调整
JVM_OPTS="$JVM_OPTS -XX:ConcGCThreads=4"       # 并发GC线程数
JVM_OPTS="$JVM_OPTS -XX:+UseStringDeduplication"  # 启用字符串去重，减少内存占用
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"  # OOM时生成堆转储文件
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=./logs/heapdump.hprof"  # 堆转储文件路径
JVM_OPTS="$JVM_OPTS -Dspring.profiles.active=h2"  # 使用H2数据库配置

# 添加日志配置
JVM_OPTS="$JVM_OPTS -Dlogging.config=classpath:logback-spring.xml"
echo "✅ JVM参数配置完成"

# 4. 检查是否存在可执行的jar文件
echo "正在查找应用程序jar包..."
JAR_FILE=$(find . -name "*.jar" | grep -v "original" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ 未找到应用程序jar包，请先执行构建命令：mvn clean package"
    exit 1
fi

echo "✅ 找到应用程序jar包: $JAR_FILE"

# 5. 启动应用
echo ""
echo "========================================="
echo "正在启动AI炒股软件..."
echo "JVM堆内存配置: 16G (针对40G内存优化)"
echo "数据库: H2 (本地文件模式)"
echo "访问地址: http://localhost:8080"
echo "H2控制台: http://localhost:8080/h2-console"
echo "========================================="
echo ""

# 创建日志目录
mkdir -p ./logs

# 启动应用并将输出重定向到日志文件
java $JVM_OPTS -jar "$JAR_FILE" > ./logs/startup.log 2>&1 &

# 获取进程ID
APP_PID=$!
echo "✅ 应用已启动，进程ID: $APP_PID"
echo "启动日志: ./logs/startup.log"
echo ""
echo "========================================="
echo "实时查看启动日志 (按Ctrl+C退出):"
echo "========================================="

# 实时显示日志
 tail -f ./logs/startup.log