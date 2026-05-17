"""
ES索引创建脚本
运行前请确保ES已启动且可访问
"""

import json
import urllib.request

ES_HOST = "http://192.168.100.128:9200"
INDEX_NAME = "user_logs"

index_mapping = {
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0
    },
    "mappings": {
        "properties": {
            "timestamp": {
                "type": "date",
                "format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            },
            "user_id": {
                "type": "keyword"
            },
            "page": {
                "type": "keyword"
            },
            "event": {
                "type": "keyword"
            },
            "duration": {
                "type": "integer"
            },
            "device": {
                "type": "keyword"
            },
            "error_msg": {
                "type": "text",
                "fields": {
                    "keyword": {
                        "type": "keyword"
                    }
                }
            }
        }
    }
}


def main():
    url = f"{ES_HOST}/{INDEX_NAME}"

    # 检查索引是否已存在
    try:
        req = urllib.request.Request(url, method="HEAD")
        urllib.request.urlopen(req)
        print(f"索引 '{INDEX_NAME}' 已存在，跳过创建。")
        overwrite = input("是否删除重建？(y/n): ").strip().lower()
        if overwrite == "y":
            delete_req = urllib.request.Request(url, method="DELETE")
            urllib.request.urlopen(delete_req)
            print(f"已删除索引 '{INDEX_NAME}'")
        else:
            print("使用现有索引。")
            return
    except urllib.error.HTTPError as e:
        if e.code != 404:
            print(f"检查索引时出错: {e}")
            return

    # 创建索引
    data = json.dumps(index_mapping).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method="PUT",
        headers={"Content-Type": "application/json"}
    )

    try:
        urllib.request.urlopen(req)
        print(f"索引 '{INDEX_NAME}' 创建成功！")
        print("Mapping配置:")
        print(json.dumps(index_mapping["mappings"], ensure_ascii=False, indent=2))
    except urllib.error.HTTPError as e:
        print(f"创建索引失败: {e.code} {e.reason}")
        print(e.read().decode())


if __name__ == "__main__":
    main()
