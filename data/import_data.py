"""
数据导入脚本
将生成的Bulk格式数据导入ES
"""

import json
import urllib.request
import urllib.error

ES_HOST = "http://192.168.100.128:9200"
BULK_FILE = "user_logs_bulk.json"


def main():
    print(f"开始导入数据到 ES ({ES_HOST})...")

    url = f"{ES_HOST}/_bulk"

    with open(BULK_FILE, "r", encoding="utf-8") as f:
        data = f.read()

    # 分批导入，每批5000条
    lines = data.strip().split("\n")
    batch_size = 5000
    total = len(lines) // 2
    imported = 0
    errors = 0
    first_error = None

    print(f"共 {total} 条记录，分 { (total + batch_size - 1) // batch_size } 批导入")
    print()

    for batch_num, i in enumerate(range(0, len(lines), batch_size * 2), 1):
        batch = "\n".join(lines[i:i + batch_size * 2]) + "\n"
        batch_data = batch.encode("utf-8")

        req = urllib.request.Request(
            url,
            data=batch_data,
            method="POST",
            headers={"Content-Type": "application/x-ndjson"}
        )

        try:
            resp = urllib.request.urlopen(req)
            result = json.loads(resp.read().decode())

            if result.get("errors"):
                batch_errors = 0
                for item in result["items"]:
                    if "error" in item.get("index", {}):
                        batch_errors += 1
                        if first_error is None:
                            err = item["index"]["error"]
                            first_error = f"文档ID: {item['index'].get('_id', '?')}, 类型: {err.get('type', '?')}, 原因: {err.get('reason', '?')}"
                imported += (batch_size - batch_errors)
                errors += batch_errors
                print(f"  第{batch_num}批: 成功 {batch_size - batch_errors}, 失败 {batch_errors}")
            else:
                imported += batch_size
                print(f"  第{batch_num}批: 全部成功 ({batch_size} 条)")

        except urllib.error.HTTPError as e:
            print(f"  第{batch_num}批 导入失败: {e.code} {e.reason}")
            print(f"    详情: {e.read().decode()[:300]}")
            return

    print()
    print(f"导入完成！")
    print(f"  成功: {imported} 条")
    print(f"  失败: {errors} 条")

    if first_error:
        print(f"\n  首个错误详情: {first_error}")
        if "ik" in first_error.lower() or "analyzer" in first_error.lower():
            print("  💡 提示: 这是IK分词器问题，不要在error_msg字段上使用ik_analyzer")
        if "mapper_parsing_exception" in first_error or "illegal_argument_exception" in first_error:
            print("  💡 提示: 日期格式可能不匹配，检查timestamp格式")

    # 验证
    print()
    try:
        count_url = f"{ES_HOST}/user_logs/_count"
        resp = urllib.request.urlopen(count_url)
        result = json.loads(resp.read().decode())
        print(f"ES中当前记录数: {result['count']}")
        if result['count'] == total:
            print("🎉 全部导入成功！")
        elif result['count'] < total:
            print(f"⚠️  缺少 {total - result['count']} 条记录，请检查错误日志")
    except Exception as e:
        print(f"验证失败: {e}")


if __name__ == "__main__":
    main()
