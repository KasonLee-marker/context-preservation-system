# 部署指南

## 环境要求

### 最低配置
- CPU: 2 核
- 内存: 8GB（Milvus 限制 4GB）
- 磁盘: 20GB
- 网络: 可访问 Docker Hub 和 Maven 仓库

### 推荐配置
- CPU: 4 核
- 内存: 16GB
- 磁盘: 50GB SSD
- 网络: 稳定的国际网络连接

## 部署步骤

### 1. 安装 Docker

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 配置镜像加速
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.xuanyuan.me",
    "https://docker.1ms.run"
  ]
}
EOF
sudo systemctl restart docker
```

### 2. 安装 Java 和 Maven

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk maven

# 验证安装
java -version
mvn -version
```

### 3. 获取代码

```bash
git clone https://github.com/KasonLee-marker/context-preservation-system.git
cd context-preservation-system
```

### 4. 配置环境变量

```bash
# 创建 .env 文件
cat > .env <<EOF
DASHSCOPE_API_KEY=sk-your-key-here
EOF

# 加载环境变量
source .env
```

### 5. 启动基础设施

```bash
# 启动 Milvus + etcd + MinIO
docker compose up -d milvus etcd minio

# 等待服务就绪（约 30 秒）
sleep 30

# 检查状态
docker ps
docker logs context-preservation-system-milvus-1
```

### 6. 构建应用

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 如果下载慢，可以配置阿里云镜像
cat > ~/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
```

### 7. 运行应用

```bash
# 方式一：直接运行
java -jar target/context-preservation-system-1.0.0.jar

# 方式二：后台运行
nohup java -jar target/context-preservation-system-1.0.0.jar > app.log 2>&1 &

# 方式三：使用 Maven
mvn spring-boot:run
```

### 8. 验证部署

```bash
# 检查应用是否启动
curl http://localhost:8080/actuator/health

# 测试 API
curl -X POST http://localhost:8080/api/conversation/message \
  -H "Content-Type: application/json" \
  -d '{"content": "测试消息"}'
```

## 生产部署

### 使用 Docker Compose 完整部署

```bash
# 构建应用镜像
docker compose build

# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f app
```

### 使用 Kubernetes

```bash
# 构建镜像
docker build -t your-registry/cps:latest .
docker push your-registry/cps:latest

# 部署到 K8s
kubectl apply -f k8s/
```

## 监控和维护

### 查看日志

```bash
# 应用日志
tail -f logs/application.log

# Docker 日志
docker compose logs -f app

# Milvus 日志
docker logs context-preservation-system-milvus-1
```

### 备份数据

```bash
# 备份 Milvus 数据
docker exec context-preservation-system-milvus-1 \
  milvus-backup create -n backup_$(date +%Y%m%d)

# 备份到本地
docker cp context-preservation-system-milvus-1:/var/lib/milvus \
  ./backup/milvus_$(date +%Y%m%d)
```

### 升级版本

```bash
# 拉取最新代码
git pull origin main

# 重新构建
mvn clean package -DskipTests

# 重启服务
docker compose down
docker compose up -d
```

## 故障排查

### 问题：Milvus 启动失败

**症状**：`docker ps` 看不到 Milvus 容器

**解决**：
```bash
# 查看日志
docker logs context-preservation-system-milvus-1

# 检查内存
docker stats

# 重启服务
docker compose restart milvus
```

### 问题：应用连接 Milvus 失败

**症状**：报错 `Connection refused`

**解决**：
```bash
# 检查 Milvus 是否就绪
curl http://localhost:9091/api/v1/health

# 检查配置
grep -A 5 "milvus" src/main/resources/application.yml
```

### 问题：API Key 无效

**症状**：Embedding 调用失败

**解决**：
```bash
# 检查环境变量
echo $DASHSCOPE_API_KEY

# 测试 API Key
curl -X POST https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding \
  -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model": "text-embedding-v2", "input": {"texts": ["test"]}}'
```

## 性能优化

### 调整 Milvus 内存

编辑 `docker-compose.yml`：
```yaml
mem_limit: 8g  # 根据机器配置调整
```

### 调整并发数

编辑 `application.yml`：
```yaml
server:
  tomcat:
    threads:
      max: 100
```

### 启用缓存

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

## 安全建议

1. **API Key 管理**
   - 使用环境变量，不要硬编码
   - 定期更换 Key
   - 使用不同的 Key 用于开发和生产

2. **网络安全**
   - 使用防火墙限制端口访问
   - 生产环境启用 HTTPS
   - 配置合理的 CORS 策略

3. **数据安全**
   - 定期备份
   - 加密敏感数据
   - 限制数据保留时间

## 参考链接

- [Milvus 文档](https://milvus.io/docs)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [阿里云灵积](https://dashscope.aliyun.com/)
