"""
电商用户行为日志数据生成脚本
生成10万条模拟日志，保存为JSON文件（每行一条，兼容ES Bulk导入格式）
"""

import json
import random
import uuid
from datetime import datetime, timedelta


# 配置
TOTAL_RECORDS = 100000
OUTPUT_FILE = "user_logs.json"
ES_BULK_FILE = "user_logs_bulk.json"

# 用户池
USER_IDS = [f"user_{i:04d}" for i in range(1, 501)]

# 页面路径
PAGES = [
    "/home", "/product/list", "/product/detail", "/search",
    "/cart", "/checkout", "/payment", "/order/success",
    "/order/list", "/order/detail", "/user/profile",
    "/user/login", "/user/register", "/promotion/seckill",
    "/help/faq", "/about"
]

# 事件类型
EVENTS = ["page_view", "click", "payment", "error", "login"]

# 事件权重（控制分布）
EVENT_WEIGHTS = {
    "page_view": 45,
    "click": 25,
    "payment": 10,
    "error": 12,
    "login": 8
}

# 设备类型
DEVICES = ["PC", "Mobile", "Tablet"]
DEVICE_WEIGHTS = [40, 45, 15]

# 错误信息池
ERROR_MESSAGES = [
    "数据库连接超时",
    "支付网关响应超时",
    "用户权限不足",
    "商品库存不足",
    "系统内部错误：空指针异常",
    "请求参数校验失败",
    "验证码已过期",
    "网络连接异常",
    "会话已过期，请重新登录",
    "第三方接口调用失败",
    "订单状态异常",
    "金额计算精度丢失",
    "缓存服务不可用",
    "消息队列积压",
    "文件上传大小超出限制",
    "不支持的支付方式",
    "商品已下架",
    "地址解析失败",
    "短信发送频率超限",
    "并发冲突，请重试"
]

# 页面 -> 可能的事件（增加error覆盖页面）
PAGE_EVENTS = {
    "/home": ["page_view", "click", "error"],
    "/product/list": ["page_view", "click"],
    "/product/detail": ["page_view", "click", "payment"],
    "/search": ["page_view", "click"],
    "/cart": ["page_view", "click", "payment"],
    "/checkout": ["page_view", "click", "payment", "error"],
    "/payment": ["page_view", "click", "payment", "error"],
    "/order/success": ["page_view", "error"],
    "/order/list": ["page_view", "click"],
    "/order/detail": ["page_view", "click"],
    "/user/profile": ["page_view", "click"],
    "/user/login": ["page_view", "login", "error"],
    "/user/register": ["page_view", "click", "error"],
    "/promotion/seckill": ["page_view", "click", "payment", "error"],
    "/help/faq": ["page_view", "click"],
    "/about": ["page_view"]
}

# 页面 -> 停留时长范围（秒）
PAGE_DURATION = {
    "/home": (5, 60),
    "/product/list": (10, 120),
    "/product/detail": (30, 300),
    "/search": (5, 30),
    "/cart": (10, 120),
    "/checkout": (30, 180),
    "/payment": (20, 120),
    "/order/success": (5, 30),
    "/order/list": (10, 60),
    "/order/detail": (10, 60),
    "/user/profile": (10, 90),
    "/user/login": (10, 60),
    "/user/register": (30, 180),
    "/promotion/seckill": (10, 120),
    "/help/faq": (20, 120),
    "/about": (5, 30)
}


def choose_weighted(options, weights):
    total = sum(weights)
    r = random.uniform(0, total)
    upto = 0
    for i, w in enumerate(weights):
        upto += w
        if r < upto:
            return options[i]
    return options[-1]


def generate_timestamp(base_time, index):
    span_hours = 24
    seconds_offset = random.randint(0, span_hours * 3600 - 1)
    ts = base_time - timedelta(seconds=seconds_offset)
    # 在秒级别添加一些抖动，避免大量数据时间完全相同
    ts += timedelta(milliseconds=random.randint(0, 999))
    return ts.strftime("%Y-%m-%dT%H:%M:%S.") + f"{ts.microsecond // 1000:03d}Z"


def generate_log(base_time, index):
    user_id = random.choice(USER_IDS)
    page = random.choice(PAGES)

    # 使用权重选择事件类型，确保error占比12%
    event = choose_weighted(EVENTS, [EVENT_WEIGHTS[e] for e in EVENTS])

    timestamp = generate_timestamp(base_time, index)

    # 时长
    dur_range = PAGE_DURATION[page]
    duration = random.randint(dur_range[0], dur_range[1])

    # 如果error出现在不允许的页面上，强制重选
    error_pages = ["/home", "/checkout", "/payment", "/order/success", "/user/login", "/user/register", "/promotion/seckill"]
    if event == "error" and page not in error_pages:
        event = random.choice([e for e in ["page_view", "click", "payment", "login"] if e in PAGE_EVENTS[page]] or ["page_view"])

    device = choose_weighted(DEVICES, DEVICE_WEIGHTS)

    error_msg = None
    if event == "error":
        error_msg = random.choice(ERROR_MESSAGES)
        duration = random.randint(1, 10)

    log_entry = {
        "timestamp": timestamp,
        "user_id": user_id,
        "page": page,
        "event": event,
        "duration": duration,
        "device": device
    }

    if error_msg:
        log_entry["error_msg"] = error_msg

    return log_entry


def main():
    base_time = datetime.utcnow()
    print(f"开始生成 {TOTAL_RECORDS} 条模拟日志...")

    records = []
    for i in range(TOTAL_RECORDS):
        log = generate_log(base_time, i)
        records.append(log)

        if (i + 1) % 20000 == 0:
            print(f"  已生成 {i + 1} 条...")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        for record in records:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")

    # 生成ES Bulk导入格式
    with open(ES_BULK_FILE, "w", encoding="utf-8") as f:
        for i, record in enumerate(records):
            action = {"index": {"_index": "user_logs", "_id": str(i + 1)}}
            f.write(json.dumps(action, ensure_ascii=False) + "\n")
            f.write(json.dumps(record, ensure_ascii=False) + "\n")

    print(f"生成完成！")
    print(f"  JSON文件: {OUTPUT_FILE}")
    print(f"  Bulk文件: {ES_BULK_FILE}")
    print(f"  总记录数: {len(records)}")


if __name__ == "__main__":
    main()
