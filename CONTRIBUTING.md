# 贡献指南

## 反馈问题

部分现象属于平台机制，见 [常见问题](README.md#常见问题) 与 [已知限制](README.md#已知限制)。

提 Issue 请附上：

- 手机型号与系统版本（例如「Redmi K60 / HyperOS 2.0 / Android 15」）
- 使用的是哪个应用（番茄小说、微信读书等）
- 当前速度设置与「应用范围」是否填写
- 复现步骤：从哪一步开始、点了什么、期望看到什么、实际看到什么
- 如果有，附截图或录屏


## 功能建议

先开 Issue 讨论，说清使用场景。

## 代码贡献

### 流程

1. Fork 仓库并从 `main` 建分支
2. 改动尽量聚焦，一个 PR 解决一件事
3. 提交前跑通检查：

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
bash run-checks.sh
```

4. 提交 PR，说明改了什么、为什么改、如何验证

### 代码约定

- Java 8 语法，不引入第三方依赖（项目目标之一是不依赖 Gradle 也能构建）
- 4 空格缩进，类名大驼峰，方法与变量小驼峰
- 注释写「为什么」，不写「做了什么」。代码本身能说明在做什么
- 涉及用户可见文案时用中文，并在 `MainActivity` 与测试断言中保持同步

### 测试要求

改动以下部分时，**必须**补充或更新测试：

- `ScrollMath`：速度、位移、段长、映射
- `ScrollPolicy`：滚动条件判断
- 界面布局的关键尺寸或文案

这两类是纯 Java，测试成本很低。界面类改动如果影响布局，`LayoutTest` 里的几何断言需要同步。

### 提交信息

格式不严格，但请让第一行能独立说明改动：

```
Fix pause latency when restarting during an active segment

Restarting within the same gesture segment left inFlight set, so the
next chunk was rejected by the system and the chain stalled for ~600ms.
```

## 关于 AI 辅助

AI 生成的代码可以提交，前提是自己跑通测试、在设备上看过效果。未经验证的代码不要提 PR。
