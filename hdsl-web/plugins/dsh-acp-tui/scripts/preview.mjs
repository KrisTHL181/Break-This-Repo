/**
 * Renders sample frames to stdout for layout review.
 *
 * Not part of the published package: this is the tool the visual work is done
 * with. `node scripts/preview.mjs 100 30 busy` prints one stripped frame so the
 * alignment can be read in a diff, and `--ansi` keeps the colours for a real
 * terminal.
 */

import { createInitialState } from '../src/ui/app.js'
import { renderFrame } from '../../shared/ui/render.js'
import { stripAnsi } from '../../shared/term/text.js'

const width = Number(process.argv[2] ?? 100)
const height = Number(process.argv[3] ?? 30)
const scene = process.argv[4] ?? 'welcome'
const keepAnsi = process.argv.includes('--ansi')

function block(value) {
  return { rev: 0, ...value }
}

const base = createInitialState({
  cwd: '/data/hdsl/instances/dsh-0.2.1-alpha.1/home/workspace',
  sessionId: 'session-4f9c1b2e-8a3d-4c5e-9f10-2b7c8d9e0a1b',
  model: { current: 'deepseek-official/deepseek-v4-flash', currentLabel: 'deepseek-v4-flash', options: [] },
  effort: { current: 'high', options: [] },
  language: 'zh',
  usage: { used: 28400, size: 1000000 },
})

const banner = block({
  type: 'banner',
  tag: 'HDSL-web',
  version: 'v0.2.0 · dsh-acp-tui',
  subtitle: '终端里的 DeepSeek Harness',
  hint: '输入问题开始 · /help 查看命令',
  sessionId: base.sessionId,
  model: 'deepseek-v4-flash',
  mode: 'ask',
  planMode: false,
  cwd: base.cwd,
})

if (scene === 'welcome') {
  base.blocks = [banner]
  base.composer.setText('')
} else if (scene === 'busy') {
  base.blocks = [
    banner,
    block({ type: 'user', text: '把 plugins/dsh-acp-tui 里的渲染器拆成 blocks 和 render 两层，然后跑一遍测试。' }),
    block({
      type: 'thought',
      text: '先看 plugins/dsh-acp-tui/src/ui 下现在有几个文件。渲染和状态应该分开：render 只做布局，blocks 只做单个块。然后确认测试脚本还在跑。',
      streaming: false,
      expanded: false,
      full: '',
    }),
    block({
      type: 'tool',
      callId: 'call_1',
      name: 'bash',
      kind: 'other',
      status: 'completed',
      startedAt: 1000,
      endedAt: 1520,
      input: { command: 'ls plugins/dsh-acp-tui/src/ui && pnpm test', description: 'List UI files and run tests' },
      output: 'app.js\nblocks.js\ncomposer.js\ndialogs.js\nrender.js\n\n> @nas200-timmy/dsh-acp-tui@0.2.0 test\n> node --test test/*.test.js\n\n# pass 12\n# fail 0\n',
    }),
    block({
      type: 'tool',
      callId: 'call_2',
      name: 'edit',
      kind: 'other',
      status: 'in_progress',
      startedAt: 2000,
      input: { path: 'plugins/dsh-acp-tui/src/ui/render.js', old: 'a', new: 'b' },
      output: null,
    }),
    block({
      type: 'assistant',
      text: '已经拆开了：`render.js` 只负责布局和合成，`blocks.js` 负责每个块的绘制。\n\n- 顶部仍是会话头\n- 中间是记录区，工具卡片带耗时\n- 底部是输入框与状态栏\n\n接下来把 `ctrl+o` 的展开逻辑接上。',
      streaming: true,
      full: '',
    }),
  ]
  base.status = 'working'
  base.activity = { tool: 'edit', startedAt: 2000 }
  base.composer.setText('顺便给 /resume 加一个空列表时的提示')
} else if (scene === 'overlay') {
  base.blocks = [banner, block({ type: 'user', text: '换个模型' })]
  base.overlay = {
    kind: 'picker',
    title: '选择模型',
    items: [
      { label: 'deepseek-v4-flash', tail: '当前' },
      { label: 'DeepSeek-V4.1-Flash', description: '更快，适合日常追问' },
      { label: 'DeepSeek-V4-Pro', description: '更强的 agentic 编码与推理，成本更高' },
    ],
    index: 1,
    hint: '↑↓ 选择 · enter 确认 · esc 取消',
  }
  base.composer.setText('')
}

const frame = renderFrame(base, {
  width,
  height,
  theme: (await import('../../shared/term/theme.js')).createTheme({ color: keepAnsi }),
  i18n: base.i18n,
  clock: 2400,
})

for (const row of frame.rows) {
  process.stdout.write((keepAnsi ? row : stripAnsi(row)).replace(/\s+$/u, '') + '\n')
}
if (frame.cursor !== null) process.stdout.write(`cursor: row=${frame.cursor.row} column=${frame.cursor.column}\n`)
