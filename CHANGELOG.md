<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Rainbow Parentheses Changelog

## [Unreleased]

## [0.1.2] - 2026-06-04

### Added

- 当前块随光标持续高亮：开启后光标移动时自动淡染所在代码块（括号块 / 跨行 PSI 块），随光标实时更新；无块的顶层单行条目则淡染该行。设置项「当前块随光标持续高亮」（`Settings → Tools → Rainbow Parentheses`），默认关闭，开关即时生效。与手动 `Ctrl + 鼠标右键` 作用域高亮相互独立、可同时开

### Fixed

- 作用域 / 当前块解析不再把尖括号 `<>` 当作作用域括号：避免泛型 / 比较运算符、以及 XML 自闭合标签 `<.../>`（`<` 因闭合是 `/>` 双字符 token 而与后面无关的 `>` 错配）导致的范围错乱
- XML 标签间只含空白的文本节点不再被当作代码块，改为回退到外层标签

### Changed

- 作用域高亮（`Ctrl + 鼠标右键`）泛化到无括号语言：括号语言仍取最内层括号对；无括号包裹时（Python / YAML 等缩进结构，或括号外的代码）回退到包含光标的最内层跨行 PSI 代码块，按嵌套深度取缩进色
- 顶层单行条目（无可用子块）右键时回退为高亮该行本身，而不再无反应或染满整篇文件
- 同一作用域再次右键现在会关闭高亮（toggle）；点到不同作用域则切换

### Fixed

- `Ctrl + 鼠标右键` 不移动光标导致作用域按错误位置解析：改用实际点击位置（经屏幕坐标换算到编辑器内容组件），消除行错位
- 点击落在块结束边界（值在行尾，如 YAML `https: 443`）时解析不到所在块：改为同时探测光标与其前一字符，取包含光标的最内层跨行块

## [0.1.0] - 2026-06-03

### Added

- 标识符彩虹着色：按名字哈希给标识符上色（同名同色）；Java / Kotlin 进一步基于 PSI 作用域感知，只染局部变量与参数，同名不同作用域得到不同颜色，字段 / 类型 / 方法名不染。设置项「按名字给标识符着色」，默认关闭
- XML / HTML 标签名按嵌套深度着色，开标签名与闭标签名同色。设置项「按嵌套深度给 XML / HTML 标签名着色」，默认关闭
- JSON / YAML 键名按嵌套深度着色。设置项「按嵌套深度给 JSON / YAML 键名着色」，默认关闭
- 颜色设置页新增 Variables / Tags / Keys 分组，各 10 档颜色可逐项调整，明 / 暗主题各一套
- 标识符 / 标签 / 键名着色均通过可选依赖按需注册，缺少对应语言模块的 IDE 仍可正常加载插件
- 通过 IntelliJ Plugin Verifier 在 IC-241.19416.15 上的兼容性校验

## [0.0.2] - 2026-05-25

### Changed

- 最低支持 IDE 版本下调至 2024.1（`sinceBuild=241`），编译目标平台改为 2024.1.7 / JDK 17；`untilBuild` 留空，对未来版本不设上限
- 通过 IntelliJ Plugin Verifier 在 IC-241.19416.15 上的兼容性校验

## [0.0.1] - 2026-05-25

### Added

- 彩虹括号：按嵌套层级给四类括号 `()` `[]` `{}` `<>` 着不同颜色，循环层数 1–10 可配置
- 彩虹缩进线：接管 IDE 原生缩进引导线的渲染器，按缩进深度上色；与原生位置 / 长度一致，整体淡显，光标所在块对应的引导线自动加亮
- 作用域高亮：`Ctrl + 鼠标右键` 高亮光标所在的最内层括号作用域
- 颜色设置页：`Settings → Editor → Color Scheme → Rainbow Parentheses` 可逐层调整 40 个括号颜色 + 10 个缩进线颜色
- 主设置页：`Settings → Tools → Rainbow Parentheses`，含主开关、括号类型开关、缩进线 / 作用域开关、颜色层数、大文件阈值、排除清单等
- 快捷键：`Ctrl + Shift + R` 整体启用 / 禁用插件
- 大文件保护：默认对超过 1000 行的文件不着色，阈值可在设置页调整
- 排除清单：可按文件类型或语言 ID 排除

[Unreleased]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.1.2...HEAD
[0.1.2]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.0.2...v0.1.0
[0.0.2]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/HuaGCS/rainbow-parentheses/releases/tag/v0.0.1
