@echo off
rem The Windows entry point of the ZcodeBuilder fake pnpm. The stub itself is
rem pnpm.mjs beside this file — one implementation, wrapped for each platform, so
rem that `pnpm` and `pnpm.cmd` cannot drift apart. See pnpm.mjs for what it does.
node "%~dp0pnpm.mjs" %*
