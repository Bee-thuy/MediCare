#!/bin/bash
cd "$(dirname "$0")" || exit 1
printf '\nRoom Sort AI — khóa chỉ được giữ trong cửa sổ này, không lưu vào file.\n'
if [ -z "$OPENAI_API_KEY" ]; then
  printf 'Nhập OpenAI API key (ký tự sẽ không hiển thị): '
  IFS= read -r -s OPENAI_API_KEY
  printf '\n'
  export OPENAI_API_KEY
fi
if [ -z "$OPENAI_API_KEY" ]; then
  printf 'Chưa có API key. Nhấn Enter để đóng.\n'
  read -r
  exit 1
fi
printf '\nMở http://127.0.0.1:8765/ trong Chrome. Để cửa sổ này mở khi sử dụng.\n'
exec python3 roomsort_ai_server.py
