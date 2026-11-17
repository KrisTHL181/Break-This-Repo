import { spawnSync } from "node:child_process";

function run(command, args, options = {}) {
  console.log(command, args.join(" "));
  const result = spawnSync(command, args, {
    cwd: root,
    stdio: "inherit",
    ...options,
  });
  return result;
}
