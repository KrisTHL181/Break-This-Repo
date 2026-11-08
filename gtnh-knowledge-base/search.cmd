@echo off
rem GTNH 知识库检索入口（Windows，自动处理 UTF-8 中文显示）
rem   search 真空冷冻机             关键词检索
rem   search --exact 钨             只列标题含该词的页面
rem   search --item Vacuum          物品名 → 内部 ID
rem   search --ask "钨矿怎么处理"    生成给 AI 的上下文包
rem   search --stats                数据规模统计
setlocal
chcp 65001 >nul
set PYTHONIOENCODING=utf-8
python "%~dp0kb\search.py" %*
endlocal
