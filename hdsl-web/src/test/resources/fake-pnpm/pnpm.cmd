@echo off
rem The Windows entry point of the fake pnpm. The stub itself is pnpm.mjs beside
rem this file — one implementation, wrapped for each platform, so that `pnpm` and
rem `pnpm.cmd` cannot drift apart. See pnpm.mjs for what it does.
rem
rem This file has to exist: it is what the launcher's own executable resolution
rem finds first, and on Windows a bare `pnpm` names nothing at all.
node "%~dp0pnpm.mjs" %*
