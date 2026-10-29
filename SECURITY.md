# 安全说明

## 支持范围

安全修复只针对最新发布版本。

| 版本 | 状态 |
| --- | --- |
| 2.2 | 支持 |
| 2.1 及更早 | 不再维护，建议升级 |

## 报告问题

请不要开公开 Issue。使用 GitHub 的私密漏洞报告：

https://github.com/XiuXianInk/auto-scroll-android/security/advisories/new

或者直接通过仓库所有者的 GitHub 主页联系方式告知。

收到后会确认。

## 权限与边界

**不做的事**

- 不读取、不解析、不上传屏幕上的任何内容（无障碍服务只调用 `dispatchGesture` 注入手势，未启用 `canRetrieveWindowContent`）
- 不发起任何网络请求，没有统计、埋点或崩溃上报
- 不申请存储、通讯录、位置、相机、麦克风等权限
- 不请求 root，不修改系统设置

**申请的权限**

| 权限 | 用途 |
| --- | --- |
| `BIND_ACCESSIBILITY_SERVICE` | 注入滑动手势，功能核心 |
| `SYSTEM_ALERT_WINDOW` | 显示悬浮球，可选 |
| `WAKE_LOCK` | 滚动期间保持屏幕常亮 |

`WAKE_LOCK` 有 10 分钟超时并在此期间自动续期，最长连续持有 60 分钟；进程被系统终止时锁随进程释放。

## 签名

发布版使用维护者自签名，非 Google Play 或任何商店分发。安装前可自行核对：

```bash
apksigner verify --print-certs AutoScroll.apk
```

如果从其他地方获取了签名不同的 APK，那不是本项目的构建产物。
