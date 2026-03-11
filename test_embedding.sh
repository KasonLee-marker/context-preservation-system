#!/bin/bash
# 测试 Embedding API

echo "Testing DashScope Embedding API..."

API_KEY="sk-b842f880e1a047578613f27e04478302"

response=$(curl -s --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding' \
--header "Authorization: Bearer $API_KEY" \
--header 'Content-Type: application/json' \
--data '{
 "model": "tongyi-embedding-vision-plus",
 "input": {
 "contents": [ 
 {"text": "测试文本"}
 ]
 }
}')

echo "Response:"
echo "$response" | head -c 500
