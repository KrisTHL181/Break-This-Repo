@echo off
rem GTNH knowledge base launcher. Handles UTF-8 so Chinese output is readable.
rem   kb <keyword>          keyword search   (e.g. kb vacuum freezer)
rem   kb --item <term>      item name to internal id
rem   kb --ask "<question>" build an AI context pack
rem   kb --exact <term>     list pages whose TITLE contains the term
rem   kb --stats            knowledge base stats
rem   kb --ns wiki.main x   restrict to one source
setlocal
set PYTHONIOENCODING=utf-8
chcp 65001 >nul
python "%~dp0search.py" %*
endlocal
