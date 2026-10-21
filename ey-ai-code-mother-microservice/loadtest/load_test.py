#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""
图片收集节点 20 并发端到端压测（Python 3 纯标准库，无第三方依赖）

一次性自动完成：
  注册测试账号 -> 登录取会话 -> 创建 N 个全新 app -> N 并发发起 SSE 代码生成
  -> 逐请求统计首字节时间(TTFB)/首个代码块时间/总耗时 -> 汇总报告

用途：验证并发阻塞已解决（各请求 TTFB 极差小），并配合服务端日志/线程快照
观察图片任务是否跑在 image-collector 线程池、超时或异常能否返回空列表而不阻断流程。

运行（宿主机 PowerShell，接入 compose 网络的一次性容器；零残留，测完删本文件即可）：
  docker network ls   # 找到形如 <项目>_default 的网络名
  docker run --rm --network ey-ai-code-mother-microservice_default `
    -v "d:\Code\ey-ai-code-mother\ey-ai-code-mother-microservice\loadtest:/loadtest:ro" `
    -e USER_BASE=http://user:8124/api -e APP_BASE=http://app:8125/api -e CONCURRENCY=20 `
    python:3.12-slim python /loadtest/load_test.py

可覆盖的环境变量：
  USER_BASE, APP_BASE, CONCURRENCY, MESSAGE, INIT_PROMPT,
  ACCOUNT(默认 loadtest_<时间戳>), PASSWORD, READY_TIMEOUT, GEN_TIMEOUT, FAIL_THRESHOLD
"""
import os
import ssl
import sys
import json
import time
import threading
import http.client
import urllib.parse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed

# 让中文输出在容器/管道下不因编码报错，并按行刷新便于实时观察
try:
    sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
except Exception:
    pass

USER_BASE = os.environ.get("USER_BASE", "http://user:8124/api")
APP_BASE = os.environ.get("APP_BASE", "http://app:8125/api")
CONCURRENCY = int(os.environ.get("CONCURRENCY", "20"))
MESSAGE = os.environ.get(
    "MESSAGE",
    "生成一个展示搜图结果的静态HTML页面，代码不超过50行，把收集到的图片以网格展示出来",
)
INIT_PROMPT = os.environ.get("INIT_PROMPT", "制作一个展示图片搜索结果的静态网页画廊")
PASSWORD = os.environ.get("PASSWORD", "Test@12345")
READY_TIMEOUT = int(os.environ.get("READY_TIMEOUT", "180"))
GEN_TIMEOUT = int(os.environ.get("GEN_TIMEOUT", "300"))
FAIL_THRESHOLD = float(os.environ.get("FAIL_THRESHOLD", "0.5"))


def _split_base(base):
    """把 http://host:port/api 拆成 (scheme, host, port, base_path)"""
    u = urllib.parse.urlsplit(base)
    scheme = u.scheme or "http"
    host = u.hostname
    port = u.port or (443 if scheme == "https" else 80)
    return scheme, host, port, (u.path or "")


def _connect(scheme, host, port, timeout):
    if scheme == "https":
        ctx = ssl._create_unverified_context()
        return http.client.HTTPSConnection(host, port, timeout=timeout, context=ctx)
    return http.client.HTTPConnection(host, port, timeout=timeout)


def http_json(method, base, path, body=None, cookie=None, timeout=30):
    """发送 JSON 请求，返回 (status, resp_json_or_None, session_cookie_value_or_None)"""
    scheme, host, port, base_path = _split_base(base)
    full_path = base_path + path
    conn = _connect(scheme, host, port, timeout)
    try:
        headers = {"Content-Type": "application/json", "Accept": "application/json"}
        if cookie:
            headers["Cookie"] = cookie
        data = json.dumps(body).encode("utf-8") if body is not None else None
        conn.request(method, full_path, body=data, headers=headers)
        resp = conn.getresponse()
        raw = resp.read().decode("utf-8", errors="replace")
        session = None
        for k, v in resp.getheaders():
            if k.lower() == "set-cookie" and "SESSION=" in v:
                for part in v.split(";"):
                    part = part.strip()
                    if part.startswith("SESSION="):
                        session = part[len("SESSION="):]
        try:
            j = json.loads(raw) if raw else None
        except Exception:
            j = None
        return resp.status, j, session
    finally:
        conn.close()


def register_and_login():
    """注册（已存在则忽略）后登录，返回 (account, session)"""
    account = os.environ.get("ACCOUNT", "loadtest_%d" % int(time.time()))
    try:
        st, j, _ = http_json("POST", USER_BASE, "/user/register", {
            "userAccount": account,
            "userPassword": PASSWORD,
            "checkPassword": PASSWORD,
        })
        print("[register] account=%s status=%s resp=%s" % (account, st, j))
    except Exception as e:
        print("[register] 异常(忽略，尝试直接登录): %r" % e)
    st, j, session = http_json("POST", USER_BASE, "/user/login", {
        "userAccount": account,
        "userPassword": PASSWORD,
    })
    if st != 200 or not session:
        raise RuntimeError("登录失败 status=%s resp=%s session=%s" % (st, j, bool(session)))
    print("[login] OK，已获取 SESSION(len=%d)" % len(session))
    return account, session


def wait_and_login():
    """轮询直到服务就绪并成功登录"""
    deadline = time.time() + READY_TIMEOUT
    last = None
    while time.time() < deadline:
        try:
            return register_and_login()
        except Exception as e:
            last = e
            print("[ready] 等待 user/app 服务就绪... 最近错误: %r" % e)
            time.sleep(3)
    raise RuntimeError("服务在 %ds 内未就绪: %r" % (READY_TIMEOUT, last))


def create_apps(session, n):
    """顺序创建 n 个全新 app（保证无历史 -> 首轮 -> 会走图片收集），返回 appId 列表"""
    cookie = "SESSION=" + session
    app_ids = []
    for i in range(n):
        try:
            st, j, _ = http_json("POST", APP_BASE, "/app/add", {"initPrompt": INIT_PROMPT}, cookie=cookie)
            if st == 200 and j and j.get("code") == 0 and j.get("data") is not None:
                app_ids.append(int(j["data"]))
            else:
                print("[create-app %d] 失败 status=%s resp=%s" % (i, st, j))
        except Exception as e:
            print("[create-app %d] 异常: %r" % (i, e))
    return app_ids


def get_code_gen_type(session, app_id):
    """best-effort 读取 app 的 codeGenType（期望 html）"""
    try:
        st, j, _ = http_json("GET", APP_BASE, "/app/get/vo?id=%d" % app_id, cookie="SESSION=" + session)
        if st == 200 and j and isinstance(j.get("data"), dict):
            return j["data"].get("codeGenType")
    except Exception:
        pass
    return None


def unwrap_content(payload):
    """SSE data 负载可能被双层 {"d": ...} 包裹，尽力解出实际内容字符串"""
    try:
        obj = json.loads(payload)
        if isinstance(obj, dict) and "d" in obj:
            inner = obj["d"]
            if isinstance(inner, str):
                try:
                    obj2 = json.loads(inner)
                    if isinstance(obj2, dict) and "d" in obj2:
                        return obj2["d"]
                except Exception:
                    pass
                return inner
    except Exception:
        pass
    return payload


def looks_like_code(s):
    if not s:
        return False
    low = s.lower()
    return ("<!doctype" in low) or ("<html" in low) or ("<body" in low) or ("<div" in low) or ("```" in s)


def run_gen(idx, app_id, session, barrier):
    """发起单个 SSE 生成请求并采集时间指标"""
    scheme, host, port, base_path = _split_base(APP_BASE)
    q = urllib.parse.urlencode({"appId": app_id, "message": MESSAGE})
    path = "%s/app/chat/gen/code?%s" % (base_path, q)
    r = {
        "idx": idx, "app_id": app_id, "status": None, "ttfb_ms": None,
        "first_code_ms": None, "total_ms": None, "events": 0,
        "content_chars": 0, "got_content": False, "error": None,
    }
    # 所有并发线程在此对齐，尽量同一刻发起，令 TTFB 极差更能反映服务端是否串行化
    try:
        if barrier is not None:
            barrier.wait(timeout=60)
    except Exception:
        pass
    conn = None
    t0 = time.perf_counter()
    try:
        conn = _connect(scheme, host, port, GEN_TIMEOUT)
        conn.request("GET", path, headers={"Accept": "text/event-stream", "Cookie": "SESSION=" + session})
        resp = conn.getresponse()
        r["status"] = resp.status
        if resp.status != 200:
            body = resp.read().decode("utf-8", errors="replace")[:300]
            r["error"] = "HTTP %s: %s" % (resp.status, body)
            r["total_ms"] = (time.perf_counter() - t0) * 1000
            return r
        while True:
            line = resp.readline()
            if not line:
                break
            now = time.perf_counter()
            text = line.decode("utf-8", errors="replace").rstrip("\r\n")
            if not text:
                continue
            if text.startswith("event:done") or text == "event: done":
                break
            if text.startswith("data:"):
                payload = text[len("data:"):].strip()
                r["events"] += 1
                if r["ttfb_ms"] is None:
                    r["ttfb_ms"] = (now - t0) * 1000
                content = unwrap_content(payload)
                if content:
                    r["got_content"] = True
                    r["content_chars"] += len(content)
                    if r["first_code_ms"] is None and looks_like_code(content):
                        r["first_code_ms"] = (now - t0) * 1000
        r["total_ms"] = (time.perf_counter() - t0) * 1000
    except Exception as e:
        r["error"] = "%s: %s" % (type(e).__name__, e)
        r["total_ms"] = (time.perf_counter() - t0) * 1000
    finally:
        if conn:
            conn.close()
    return r


def percentile(sorted_vals, p):
    if not sorted_vals:
        return None
    k = (len(sorted_vals) - 1) * p
    f = int(k)
    c = min(f + 1, len(sorted_vals) - 1)
    if f == c:
        return sorted_vals[f]
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def summarize(name, results):
    vals = sorted([r[name] for r in results if r.get(name) is not None])
    if not vals:
        return "%-13s 无数据" % name
    return "%-13s n=%d min=%.0f avg=%.0f p50=%.0f p95=%.0f max=%.0f (ms)" % (
        name, len(vals), vals[0], sum(vals) / len(vals),
        percentile(vals, 0.5), percentile(vals, 0.95), vals[-1],
    )


def main():
    print("=== 图片节点并发端到端压测 ===")
    print("USER_BASE=%s APP_BASE=%s CONCURRENCY=%d" % (USER_BASE, APP_BASE, CONCURRENCY))

    account, session = wait_and_login()
    print("[account] %s" % account)

    app_ids = create_apps(session, CONCURRENCY)
    print("[apps] 创建成功 %d/%d 个: %s" % (len(app_ids), CONCURRENCY, app_ids))
    if not app_ids:
        print("[FATAL] 未能创建任何 app，终止")
        sys.exit(2)

    types = {aid: get_code_gen_type(session, aid) for aid in app_ids}
    print("[types] codeGenType 分布: %s" % dict(Counter(types.values())))

    n = len(app_ids)
    barrier = threading.Barrier(n) if n > 1 else None
    print("[gen] 发起 %d 并发 SSE 生成请求 ..." % n)
    print("[hint] 现在可在宿主机执行: docker exec code-mother-app sh -c 'kill -3 1' 抓线程快照")

    results = []
    with ThreadPoolExecutor(max_workers=n) as ex:
        futures = [ex.submit(run_gen, i, app_ids[i], session, barrier) for i in range(n)]
        for f in as_completed(futures):
            results.append(f.result())
    results.sort(key=lambda x: x["idx"])

    print("\n=== 每请求结果 ===")
    print("%3s %8s %10s %6s %8s %9s %9s %7s %8s %s" % (
        "idx", "appId", "type", "status", "TTFB_ms", "code_ms", "total_ms", "events", "content", "err"))
    for r in results:
        print("%3d %8d %10s %6s %8s %9s %9s %7d %8s %s" % (
            r["idx"], r["app_id"], str(types.get(r["app_id"])), str(r["status"]),
            ("%.0f" % r["ttfb_ms"]) if r["ttfb_ms"] is not None else "-",
            ("%.0f" % r["first_code_ms"]) if r["first_code_ms"] is not None else "-",
            ("%.0f" % r["total_ms"]) if r["total_ms"] is not None else "-",
            r["events"], str(r["got_content"]), r["error"] or ""))

    print("\n=== 汇总 ===")
    print(summarize("ttfb_ms", results))
    ttfbs = [r["ttfb_ms"] for r in results if r["ttfb_ms"] is not None]
    if ttfbs:
        print("TTFB 极差(max-min) = %.0f ms   <-- 越小越说明入口/虚拟线程层未被串行化" % (max(ttfbs) - min(ttfbs)))
    print(summarize("first_code_ms", results))
    print(summarize("total_ms", results))

    ok = sum(1 for r in results if r["status"] == 200 and r["got_content"])
    empty = sum(1 for r in results if r["status"] == 200 and not r["got_content"])
    failed = sum(1 for r in results if r["status"] != 200 or r["error"])
    print("成功(有内容)=%d  空结果=%d  失败=%d  总计=%d" % (ok, empty, failed, len(results)))

    if results and (failed / len(results)) > FAIL_THRESHOLD:
        print("[FAIL] 失败比例超过阈值 %.2f" % FAIL_THRESHOLD)
        sys.exit(1)
    print("[DONE] 压测完成")


if __name__ == "__main__":
    main()
