import { syncDataToGitee } from '../node-functions/_lib/gitee.js';
import { DATA_DIR } from '../node-functions/_lib/storage.js';

async function main() {
  const result = await syncDataToGitee(DATA_DIR);
  process.exit(result ? 0 : 1);
}

main();
