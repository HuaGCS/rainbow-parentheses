<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Rainbow Parentheses Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/HuaGCS/rainbow-parentheses/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/HuaGCS/rainbow-parentheses/releases/tag/v0.0.1
