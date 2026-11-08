import { storage, genUUID, hashPassword } from '../node-functions/_lib/common.js';

const FOUNDER_ACCOUNTS = [
  { username: 'chenyida', realName: '陈益达', nickname: '陈益达', title: '第一技术及工程指导长官' },
  { username: 'luoyilin', realName: '罗逸琳', nickname: '烟澪', title: '首席技术设计及工程师' },
  { username: 'fanling', realName: '范凌', nickname: '范凌', title: '对外及对内协调总长' },
  { username: 'zhangmingye', realName: '张铭业', nickname: '张铭业', title: '首席理论验证工程师' },
  { username: 'laishibo', realName: '赖世博', nickname: '赖世博', title: '交涉及工程资源管理总长' },
  { username: 'liujiacheng', realName: '刘家成', nickname: '刘家成', title: '首席技术及工程审查长' },
];

const DEFAULT_PASSWORD = '20766643';

async function main() {
  let created = 0;
  for (const acc of FOUNDER_ACCOUNTS) {
    const existing = await storage.get(`user:username:${acc.username}`);
    if (existing) {
      console.log(`  跳过: ${acc.username} (已存在)`);
      continue;
    }
    const uid = acc.username;
    const uuid = genUUID();
    const { salt, hash } = hashPassword(DEFAULT_PASSWORD);

    const user = {
      uid, uuid,
      username: acc.username,
      nickname: acc.nickname,
      email: '',
      backupEmail: '',
      passwordHash: hash,
      passwordSalt: salt,
      role: 'admin',
      verified: true,
      realName: acc.realName,
      className: '2606班',
      enrollYear: '2023',
      school: '廉中',
      dormRoom: '',
      githubId: '',
      twoFactorEnabled: false,
      passkeyEnabled: false,
      createdAt: Date.now(),
      lastLoginAt: Date.now(),
      status: 'active',
      tags: ['no_time', 'founder'],
      blockedUsers: [],
      notes: {},
      founderTitle: acc.title,
    };

    await storage.put(`user:uid:${uid}`, user);
    await storage.put(`user:username:${acc.username}`, uid);
    console.log(`  创建: ${acc.username} (${acc.realName})`);
    created++;
  }
  console.log(`\n完成: 创建 ${created} 个创始人账号，默认密码: ${DEFAULT_PASSWORD}`);
  console.log('请登录后及时修改密码并绑定邮箱。');
}

main();
