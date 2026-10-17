# Shell 权限释放接口兼容修复

日期：2026-10-18

## 目标
- 修复 Android 17 上配置操作结束时出现的 `No interface method stopDelegateShellPermissionIdentity()V`。
- 在新旧系统上都实际释放委托权限，避免只吞掉接口缺失异常。

## 现场依据
- USB 连接的 Pixel 9 Pro XL：Android 17 / API 37，版本 `CP3A.260905.009`，安全补丁 `2026-09-05`。
- 已安装应用：`3.9.0.r106.e9035ac9`，Shizuku 服务运行中。
- 在设备上通过只读反射检查 `IActivityManager`，确认启动接口仍为 `startDelegateShellPermissionIdentity(int, String[])`，停止接口为 `stopDelegateShellPermissionIdentity(int)`，无旧版无参方法。
- 当前 `ImsModifier` 在 `finally` 中直接调用无参停止接口，异常会覆盖配置写入结果，也可能掩盖写入本身的异常；其余七个特权入口虽记录清理失败，仍未适配新签名。
- 远端 PR #61 尚未合并，其中清理修复仅捕获旧接口异常；本次根据真机签名补齐实际调用，不引入该 PR 的 APN 行为变更。

## 非目标
- 不修改 APN、运营商配置内容、SIM 选择规则或 IMS 注册策略。
- 不升级依赖、修改 targetSdk、扩大授权范围或自动调整手机网络设置。

## 行为变化
- 统一封装停止权限委托调用，按运行时接口能力优先调用带 UID 的方法；仅在该签名不存在时回退到无参方法。
- UID 与启动委托使用的应用 UID 相同；不依赖 Android 版本号猜测接口。
- 反射调用失败时保留底层异常，真实权限错误不触发旧接口重试。
- 所有八个特权入口复用兼容封装；`ImsModifier` 的清理失败记录日志，不再覆盖原操作结果，与其他入口保持一致。

## 影响范围
- 运营商配置开关、TikTok 修复、配置恢复及 IMS 注册前应用配置。
- SIM 信息、配置读取、IMS 状态与重启、APN 写入和网络验证设置等入口的权限清理。

## 验证方式
- 先用单元测试重现只有带 UID 接口时旧实现失败，再验证新旧签名、优先级和底层异常保留。
- 运行应用单元测试与 Release APK 构建。
- 在连接设备上检查新接口选择和 UID 传递；涉及覆盖安装或网络配置写入的验证另行明确，未执行时不宣称功能已在手机恢复。

## 验证结果
- 旧版无参调用在新签名测试中复现失败；修复后 6 项兼容测试全部通过，应用现有 22 项测试也全部通过。
- `./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest :app:assembleRelease` 成功；构建保留现有废弃 API 和 Gradle 配置警告。
- 在连接的 Android 17 手机上，用临时 `app_process` 程序分别执行编译后的 helper 和实际 Release APK 中经 R8 混淆的 helper，两者均连续两次通过 Binder 调用带 UID 的停止接口。
- 真机探针只使用诊断进程自身的 shell UID 2000，返回的委托权限为空；未通过探针写入运营商配置、APN 或重置 IMS。这只证明接口调用兼容，不等于已验证正式应用的完整功能流程。
- 本地 APK 与手机当前安装包签名不一致，未执行覆盖安装或卸载。现有应用和数据保持不变；正式更新需使用与已安装应用匹配的签名。
- 原安装包备份和诊断输出保存在忽略的构建目录 `app/build/diagnostics/2026-10-18-permission-compat/`；修复包为 `app/build/outputs/apk/release/app-release.apk`。
- 已删除手机临时探针与 APK，关闭临时 ADB 服务，确认无本次构建的 Gradle/Kotlin 常驻进程。

## 发布方式
- 本次作为 `3.9.1` 补丁版本发布，完整版本名与版本号沿用 Git 提交自动生成规则。
- 通过现有 `release` 分支的 GitHub Actions 构建并发布，沿用仓库发布密钥；不上传本地不同签名的 APK 作为正式更新包。
- 发布后核对安装包签名与旧版一致、版本号递增，确认 Release 附件可下载。
